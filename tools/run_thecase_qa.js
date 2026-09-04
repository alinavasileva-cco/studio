const { chromium } = require('playwright-core');
const fs = require('fs');

const candidates = ['/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium', '/usr/bin/chromium-browser'];
const executablePath = candidates.find(fs.existsSync);
if (!executablePath) throw new Error('No system Chrome/Chromium found');

(async () => {
  const browser = await chromium.launch({ headless: true, executablePath, args: ['--no-sandbox'] });
  const widths = [320, 360, 375, 390, 412, 430, 640, 768, 1024, 1440, 1920];
  const results = [];
  fs.mkdirSync('qa-thecase', { recursive: true });

  for (const width of widths) {
    const page = await browser.newPage({ viewport: { width, height: width >= 1000 ? 1000 : 1200 }, deviceScaleFactor: 1 });
    await page.route('**/*', async route => {
      const url = route.request().url();
      if (url.startsWith('http://127.0.0.1:8000/') || url.startsWith('data:') || url.startsWith('blob:')) return route.continue();
      return route.abort();
    });
    await page.goto('http://127.0.0.1:8000/', { waitUntil: 'domcontentloaded', timeout: 10000 });
    await page.waitForTimeout(200);

    const data = await page.evaluate(() => {
      const text = document.body.innerText;
      const laptop = document.querySelector('.laptop');
      const laptopIframe = document.querySelector('.laptop-screen iframe');
      const brandMark = document.querySelector('.brand-mark');
      const brandTagline = document.querySelector('.brand-tagline');
      const navTexts = [...document.querySelectorAll('.nav a')].map(a => a.textContent.trim());
      const caseNums = [...document.querySelectorAll('.case-meta > span')].filter(el => getComputedStyle(el).display !== 'none').map(el => el.textContent.trim());
      const oversized = [...document.querySelectorAll('body *')].filter(el => {
        const s = getComputedStyle(el);
        if (s.position === 'fixed') return false;
        const r = el.getBoundingClientRect();
        return r.width > window.innerWidth + 2 && !el.closest('.hero') && !el.closest('.services');
      }).slice(0, 12).map(el => ({tag:el.tagName, cls:el.className || '', width:Math.round(el.getBoundingClientRect().width)}));
      return {
        innerWidth: window.innerWidth,
        scrollWidth: document.documentElement.scrollWidth,
        bodyScrollWidth: document.body.scrollWidth,
        brandMarkPresent: !!brandMark,
        brandTagline: brandTagline ? brandTagline.textContent.trim() : '',
        navTexts,
        oldEyebrowPresent: text.includes('ПРЕЗЕНТАЦИИ · СТРУКТУРА · ДИЗАЙН'),
        newEyebrowPresent: text.includes('ПРЕЗЕНТАЦИИ ДЛЯ БИЗНЕСА, КОТОРЫЕ ПРОДАЮТ'),
        phonePresent: !!document.querySelector('.phone-frame'),
        laptopPresent: !!laptop,
        laptopIframePresent: !!laptopIframe,
        laptopWidth: laptop ? Math.round(laptop.getBoundingClientRect().width) : 0,
        caseNums,
        oversized
      };
    });

    const noOverflow = data.scrollWidth <= width + 1 && data.bodyScrollWidth <= width + 1;
    const headerOk = !data.brandMarkPresent && data.brandTagline === 'Business presentations. Not just slides.' && !data.navTexts.includes('ПРИМЕРЫ');
    const heroOk = !data.oldEyebrowPresent && data.newEyebrowPresent;
    const contactOk = !data.phonePresent && data.laptopPresent && data.laptopIframePresent && data.laptopWidth <= width + 1;
    const numberingOk = data.caseNums.length === 0;

    await page.locator('[data-lang="en"]').click();
    await page.waitForTimeout(50);
    const englishOk = await page.evaluate(() => document.querySelector('.eyebrow')?.textContent.trim() === 'BUSINESS PRESENTATIONS THAT SELL' && document.querySelector('.hero-title')?.textContent.trim() === 'THE CASE');

    const pass = noOverflow && headerOk && heroOk && contactOk && numberingOk && englishOk;
    results.push({ width, pass, noOverflow, headerOk, heroOk, contactOk, numberingOk, englishOk, ...data });

    if (width === 390 || width === 430 || width === 1440) {
      await page.locator('[data-lang="ru"]').click();
      await page.screenshot({ path: `qa-thecase/site-${width}.png`, fullPage: true });
    }
    await page.close();
  }

  fs.writeFileSync('qa-thecase/results.json', JSON.stringify(results, null, 2));
  console.log('THECASE_QA_RESULTS=' + JSON.stringify(results));
  await browser.close();
  if (results.some(r => !r.pass)) process.exit(1);
})();
