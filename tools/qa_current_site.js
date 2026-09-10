const { chromium } = require('playwright-core');
const fs = require('fs');

const candidates = ['/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium', '/usr/bin/chromium-browser'];
const executablePath = candidates.find(fs.existsSync);
if (!executablePath) throw new Error('No system Chrome/Chromium found');

(async () => {
  const browser = await chromium.launch({ headless: true, executablePath, args: ['--no-sandbox'] });
  fs.mkdirSync('qa-current', { recursive: true });
  const results = [];
  const expectedTitles = ['ALFA BANK', 'HUAWEI', 'RED FOX', 'AURELIA', 'YANDEX TAXI', 'JAPANESE MINIMALISM'];

  for (const width of [390, 430, 768, 1024, 1440]) {
    const height = width >= 1000 ? 1000 : width >= 700 ? 1024 : 900;
    const page = await browser.newPage({ viewport: { width, height }, deviceScaleFactor: 1 });
    const consoleErrors = [];
    const pageErrors = [];
    page.on('console', msg => { if (msg.type() === 'error') consoleErrors.push(msg.text()); });
    page.on('pageerror', err => pageErrors.push(String(err)));

    await page.route('**/*', route => {
      const url = route.request().url();
      if (url.startsWith('http://127.0.0.1:8000/') || url.startsWith('data:') || url.startsWith('blob:')) return route.continue();
      return route.abort();
    });

    const started = Date.now();
    await page.goto('http://127.0.0.1:8000/', { waitUntil: 'domcontentloaded', timeout: 15000 });
    const domContentLoadedMs = Date.now() - started;

    await page.locator('.hero').waitFor({ state: 'visible', timeout: 5000 });
    await page.locator('#services .services-master').waitFor({ state: 'visible', timeout: 5000 });
    await page.locator('.brief-modal').waitFor({ state: 'attached', timeout: 5000 });
    await page.waitForFunction(() => document.querySelectorAll('.cases-grid .case-card').length === 6, null, { timeout: 6000 });

    const data = await page.evaluate(expectedTitles => {
      const rect = el => el ? ({
        x: Math.round(el.getBoundingClientRect().x),
        y: Math.round(el.getBoundingClientRect().y),
        width: Math.round(el.getBoundingClientRect().width),
        height: Math.round(el.getBoundingClientRect().height)
      }) : null;
      const serviceImage = document.querySelector('.services-master-visual img');
      const cards = [...document.querySelectorAll('.cases-grid .case-card')];
      const titles = cards.map(card => card.querySelector('.case-meta h3')?.textContent?.trim() || '');
      const iframes = document.querySelectorAll('.cases-grid iframe').length;
      const serviceImageLoaded = !!serviceImage && serviceImage.complete && serviceImage.naturalWidth > 0;
      const hero = document.querySelector('.hero');
      const services = document.querySelector('#services');
      const work = document.querySelector('#work');
      return {
        title: document.title,
        readyState: document.readyState,
        hero: rect(hero),
        services: rect(services),
        work: rect(work),
        serviceImageLoaded,
        serviceImageNaturalWidth: serviceImage?.naturalWidth || 0,
        serviceImageSrc: serviceImage?.getAttribute('src') || '',
        cardCount: cards.length,
        titles,
        titleOrderCorrect: JSON.stringify(titles) === JSON.stringify(expectedTitles),
        portfolioIframeCount: iframes,
        overflowX: document.documentElement.scrollWidth - innerWidth,
        bodyHeight: document.body.scrollHeight,
        serviceText: document.querySelector('.services-master-subhead')?.textContent?.trim() || '',
        priceText: document.querySelector('.services-master-price')?.textContent?.trim() || ''
      };
    }, expectedTitles);

    const heartbeatStart = Date.now();
    await page.evaluate(() => new Promise(resolve => setTimeout(resolve, 100)));
    const heartbeatMs = Date.now() - heartbeatStart;

    await page.locator('.hero').screenshot({ path: `qa-current/hero-${width}.png` });
    await page.locator('#services').screenshot({ path: `qa-current/services-${width}.png` });
    await page.locator('#work').screenshot({ path: `qa-current/work-${width}.png` });

    const pass =
      domContentLoadedMs < 5000 &&
      heartbeatMs < 1500 &&
      data.hero?.width > 0 &&
      data.services?.width > 0 &&
      data.work?.width > 0 &&
      data.serviceImageLoaded &&
      data.cardCount === 6 &&
      data.titleOrderCorrect &&
      data.portfolioIframeCount === 0 &&
      data.overflowX <= 1 &&
      data.serviceText === 'Презентации и сайты' &&
      data.priceText === '10 000 рублей (до 15 слайдов)' &&
      pageErrors.length === 0;

    results.push({ width, pass, domContentLoadedMs, heartbeatMs, data, consoleErrors, pageErrors });
    await page.close();
  }

  fs.writeFileSync('qa-current/results.json', JSON.stringify(results, null, 2));
  console.log(JSON.stringify(results, null, 2));
  await browser.close();
  if (results.some(r => !r.pass)) process.exit(1);
})();
