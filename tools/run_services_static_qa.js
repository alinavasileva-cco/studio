const { chromium } = require('playwright-core');
const fs = require('fs');

(async()=>{
  const browser = await chromium.launch({headless:true, executablePath:'/usr/bin/google-chrome'});
  const widths=[390,1440];
  const results=[];
  fs.mkdirSync('qa-static',{recursive:true});
  for(const width of widths){
    const page=await browser.newPage({viewport:{width,height:1200},deviceScaleFactor:1});
    await page.goto('http://127.0.0.1:8000',{waitUntil:'networkidle'});
    await page.locator('#services').scrollIntoViewIfNeeded();
    await page.waitForTimeout(500);
    const data=await page.evaluate(()=>{
      const sec=document.querySelector('#services');
      const panel=document.querySelector('.services-panel');
      const base=document.querySelector('.services-decor-base');
      const hand=document.querySelector('.services-decor-hand');
      const wind=document.querySelector('.services-decor-wind');
      const index=document.querySelector('.section-index');
      const all=[...sec.querySelectorAll('*')];
      const overflowing=all.filter(el=>el.scrollWidth>el.clientWidth+2).map(el=>el.className||el.tagName).slice(0,10);
      return {
        innerWidth:innerWidth,
        docWidth:document.documentElement.scrollWidth,
        panelBg:getComputedStyle(panel).backgroundColor,
        baseAnimation:getComputedStyle(base).animationName,
        handAnimation:getComputedStyle(hand).animationName,
        baseFilter:getComputedStyle(base).filter,
        handFilter:getComputedStyle(hand).filter,
        baseLoaded:base.complete && base.naturalWidth>0,
        handLoaded:hand.complete && hand.naturalWidth>0,
        windExists:!!wind,
        indexExists:!!index,
        overflowing
      };
    });
    const box=await page.locator('#services').boundingBox();
    await page.screenshot({path:`qa-static/services-${width}.png`,clip:{x:0,y:Math.max(0,box.y),width,height:Math.min(box.height,1200)}});
    const pass=data.docWidth===width && data.baseLoaded && data.handLoaded && !data.windExists && !data.indexExists && data.baseAnimation==='none' && data.handAnimation==='none' && data.baseFilter==='none' && data.handFilter==='none' && data.overflowing.length===0;
    results.push({width,pass,...data});
    await page.close();
  }
  console.log('STATIC_SERVICES_QA='+JSON.stringify(results));
  await browser.close();
  if(results.some(r=>!r.pass)) process.exit(1);
})();
