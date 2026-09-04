const { chromium } = require('playwright-core');
const fs = require('fs');

const candidates = ['/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium', '/usr/bin/chromium-browser'];
const executablePath = candidates.find(fs.existsSync);
if (!executablePath) throw new Error('No system Chrome/Chromium found');

(async () => {
  const browser = await chromium.launch({ headless: true, executablePath, args: ['--no-sandbox'] });
  const widths = [320, 360, 375, 390, 412, 430, 768, 1440];
  const results = [];
  fs.mkdirSync('qa-output', { recursive: true });

  for (const width of widths) {
    const page = await browser.newPage({ viewport: { width, height: width >= 1000 ? 1000 : 1200 }, deviceScaleFactor: 1 });
    await page.route('**/*', async route => {
      const url = route.request().url();
      if (url.startsWith('http://127.0.0.1:8000/') || url.startsWith('data:') || url.startsWith('blob:')) return route.continue();
      return route.abort();
    });
    await page.goto('http://127.0.0.1:8000/', { waitUntil: 'domcontentloaded', timeout: 10000 });
    await page.locator('#services').scrollIntoViewIfNeeded();
    await page.locator('.services-decor-hand').waitFor({ state: 'attached', timeout: 5000 });
    await page.waitForTimeout(250);

    const data = await page.evaluate(() => {
      const panel = document.querySelector('.services-panel');
      const decor = document.querySelector('.services-decor-hand');
      const base = document.querySelector('.services-decor-base');
      const wind = document.querySelector('.services-decor-wind');
      const panelRect = panel.getBoundingClientRect();
      const overflowingText = [...document.querySelectorAll('#services h2,#services h3,#services p,#services span,#services b,#services small')]
        .filter(el => el.clientWidth > 0 && el.scrollWidth > el.clientWidth + 2)
        .map(el => ({ tag: el.tagName, text: (el.textContent || '').trim().slice(0, 80), clientWidth: el.clientWidth, scrollWidth: el.scrollWidth }));
      const desc = [...document.querySelectorAll('#services .service-desc')].map(el => (el.textContent || '').trim());
      return {
        innerWidth: window.innerWidth,
        documentScrollWidth: document.documentElement.scrollWidth,
        bodyScrollWidth: document.body.scrollWidth,
        panelLeft: Math.round(panelRect.left * 10) / 10,
        panelRight: Math.round(panelRect.right * 10) / 10,
        panelWidth: Math.round(panelRect.width * 10) / 10,
        decorLoaded: !!decor && decor.complete && decor.naturalWidth > 0,
        decorNaturalWidth: decor ? decor.naturalWidth : 0,
        decorAnimation: decor ? getComputedStyle(decor).animationName : null,
        basePresent: !!base,
        windPresent: !!wind,
        desc,
        overflowingText
      };
    });

    const copyOk = data.desc[0] === 'Одностраничники / лендинги' && data.desc[1] === 'Брендбуки, фирменный стиль';
    const pass = data.documentScrollWidth <= width + 1 && data.panelLeft >= -1 && data.panelRight <= width + 1 && data.decorLoaded && data.overflowingText.length === 0 && data.decorAnimation === 'none' && !data.basePresent && !data.windPresent && copyOk;
    results.push({ width, pass, ...data });

    if (width === 390 || width === 1440) await page.locator('#services').screenshot({ path: `qa-output/services-${width}.png` });
    await page.close();
  }

  fs.writeFileSync('qa-output/results.json', JSON.stringify(results, null, 2));
  console.log('SERVICES_QA_RESULTS=' + JSON.stringify(results));
  await browser.close();
  if (results.some(r => !r.pass)) process.exit(1);
})();
