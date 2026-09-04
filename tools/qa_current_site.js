const { chromium } = require('playwright-core');
const fs = require('fs');

const candidates = ['/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium', '/usr/bin/chromium-browser'];
const executablePath = candidates.find(fs.existsSync);
if (!executablePath) throw new Error('No system Chrome/Chromium found');

(async () => {
  const browser = await chromium.launch({ headless: true, executablePath, args: ['--no-sandbox'] });
  fs.mkdirSync('qa-current', { recursive: true });
  const results = [];

  for (const width of [390, 430, 1440]) {
    const page = await browser.newPage({ viewport: { width, height: width >= 1000 ? 1000 : 900 }, deviceScaleFactor: 1 });
    const consoleErrors = [];
    page.on('console', msg => { if (msg.type() === 'error') consoleErrors.push(msg.text()); });
    page.on('pageerror', err => consoleErrors.push(String(err)));

    await page.route('**/*', route => {
      const url = route.request().url();
      if (url.startsWith('http://127.0.0.1:8000/') || url.startsWith('data:') || url.startsWith('blob:')) return route.continue();
      return route.abort();
    });
    await page.goto('http://127.0.0.1:8000/', { waitUntil: 'domcontentloaded', timeout: 15000 });
    await page.waitForTimeout(500);

    const services = page.locator('#services');
    await services.scrollIntoViewIfNeeded();
    await page.waitForTimeout(350);
    await services.screenshot({ path: `qa-current/services-${width}.png` });

    const firstService = await page.locator('.services-panel .service-offer h3').first().textContent();
    const servicesData = await page.evaluate(() => {
      const stage = document.querySelector('.services-stage');
      const panel = document.querySelector('.services-panel');
      const base = document.querySelector('.services-art-base');
      const hand = document.querySelector('.services-art-hand');
      const rect = el => el ? ({ x: Math.round(el.getBoundingClientRect().x), y: Math.round(el.getBoundingClientRect().y), width: Math.round(el.getBoundingClientRect().width), height: Math.round(el.getBoundingClientRect().height) }) : null;
      return {
        stage: rect(stage),
        panel: rect(panel),
        base: base ? { loaded: base.complete && base.naturalWidth > 0, naturalWidth: base.naturalWidth, naturalHeight: base.naturalHeight, rect: rect(base), src: base.getAttribute('src') } : null,
        hand: hand ? { loaded: hand.complete && hand.naturalWidth > 0, naturalWidth: hand.naturalWidth, naturalHeight: hand.naturalHeight, rect: rect(hand), src: hand.getAttribute('src') } : null,
        beforeContent: panel ? getComputedStyle(panel, '::before').content : null,
        afterContent: panel ? getComputedStyle(panel, '::after').content : null,
        overflowX: document.documentElement.scrollWidth - innerWidth
      };
    });

    const work = page.locator('#work');
    await work.scrollIntoViewIfNeeded();
    await page.waitForTimeout(350);

    const cards = page.locator('.case-card');
    const cardCount = await cards.count();
    const caseImages = [];
    for (let i = 0; i < cardCount; i++) {
      const card = cards.nth(i);
      await card.scrollIntoViewIfNeeded();
      await page.waitForTimeout(180);
      caseImages.push(await card.evaluate(el => {
        const img = el.querySelector('.case-slide.is-active');
        const vp = el.querySelector('.case-viewport');
        return {
          title: el.querySelector('.case-meta h3')?.textContent.trim() || '',
          complete: !!img?.complete,
          naturalWidth: img?.naturalWidth || 0,
          naturalHeight: img?.naturalHeight || 0,
          src: img?.getAttribute('src') || '',
          currentSrc: img?.currentSrc || '',
          fallback: !!vp?.classList.contains('is-fallback')
        };
      }));
    }
    await page.locator('#work').screenshot({ path: `qa-current/work-${width}.png` });

    const pass = firstService.trim() === 'Презентации' && servicesData.base?.loaded && servicesData.hand?.loaded && servicesData.beforeContent === 'none' && servicesData.afterContent === 'none' && servicesData.overflowX <= 1 && caseImages.length === 6 && caseImages.every(x => x.complete && x.naturalWidth > 0 && !x.fallback && x.src.startsWith('assets/cases/'));
    results.push({ width, pass, firstService: firstService.trim(), servicesData, caseImages, consoleErrors });
    await page.close();
  }

  fs.writeFileSync('qa-current/results.json', JSON.stringify(results, null, 2));
  console.log(JSON.stringify(results, null, 2));
  await browser.close();
  if (results.some(r => !r.pass)) process.exit(1);
})();
