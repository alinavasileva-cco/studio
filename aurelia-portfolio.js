(() => {
  const order = [
    'HUAWEI',
    'ALFA BANK',
    'RED FOX',
    'JAPANESE MINIMALISM',
    'OZON',
    'YANDEX TAXI'
  ];

  const configs = {
    'HUAWEI': {
      ru: 'Технологии', en: 'Technology',
      slides: ['p1','p2','p3'].map(pageId => `https://docs.google.com/presentation/d/14EqD4BfOBe61t_WIry_DLBpq6UdKv78zi9e0s305ZFs/export/png?id=14EqD4BfOBe61t_WIry_DLBpq6UdKv78zi9e0s305ZFs&pageid=${pageId}`),
      fallback: 'https://drive.google.com/thumbnail?id=14EqD4BfOBe61t_WIry_DLBpq6UdKv78zi9e0s305ZFs&sz=w1600'
    },
    'ALFA BANK': {
      ru: 'Банк', en: 'Banking',
      slides: ['p1','p2'].map(pageId => `https://docs.google.com/presentation/d/15kwfUROFvD_83DoYzY5CQS3ilD3cg0hrGAQVcFTMPD8/export/png?id=15kwfUROFvD_83DoYzY5CQS3ilD3cg0hrGAQVcFTMPD8&pageid=${pageId}`),
      fallback: 'https://drive.google.com/thumbnail?id=15kwfUROFvD_83DoYzY5CQS3ilD3cg0hrGAQVcFTMPD8&sz=w1600'
    },
    'RED FOX': {
      ru: 'Продукт', en: 'Product',
      slides: ['assets/cases/redfox-01.webp?v=20260904-1','assets/cases/redfox-02.webp?v=20260904-1']
    },
    'JAPANESE MINIMALISM': {
      ru: 'Editorial', en: 'Editorial',
      slides: ['assets/cases/japanese-01.webp?v=20260904-1','assets/cases/japanese-02.webp?v=20260904-1','assets/cases/japanese-03.webp?v=20260904-1']
    },
    'OZON': {
      ru: 'Retail media', en: 'Retail media',
      slides: ['p2','p3','p4'].map(pageId => `https://docs.google.com/presentation/d/1vCxqqiwonb6E-VVP4qggV6tVw7P4bHWeFDSti5zBu4c/export/png?id=1vCxqqiwonb6E-VVP4qggV6tVw7P4bHWeFDSti5zBu4c&pageid=${pageId}`),
      fallback: 'https://drive.google.com/thumbnail?id=1vCxqqiwonb6E-VVP4qggV6tVw7P4bHWeFDSti5zBu4c&sz=w1600'
    },
    'YANDEX TAXI': {
      ru: 'Recruitment', en: 'Recruitment',
      slides: ['assets/cases/yandex-01.webp?v=20260904-1','assets/cases/yandex-02.webp?v=20260904-1','assets/cases/yandex-03.webp?v=20260904-1','assets/cases/yandex-04.webp?v=20260904-1']
    }
  };

  const grid = document.querySelector('.cases-grid');
  if (!grid) return;

  // Replace the legacy portfolio immediately so old Google Slides iframes are removed
  // before the rest of the app initializes. Pending cards intentionally do not use
  // .case-card yet, so the legacy portfolio initializer ignores them.
  const cards = order.map((title, index) => {
    const cfg = configs[title];
    const card = document.createElement('article');
    card.className = 'portfolio-card-pending';
    card.dataset.title = title;
    card.dataset.order = String(index + 1);
    card.innerHTML = `
      <div class="case-viewport portfolio-light-viewport" aria-label="${title}"></div>
      <div class="case-meta"><div><h3>${title}</h3><p data-ru="${cfg.ru}" data-en="${cfg.en}">${cfg.ru}</p></div></div>`;
    return card;
  });
  grid.replaceChildren(...cards);
  grid.classList.add('portfolio-six');

  const style = document.createElement('style');
  style.id = 'portfolio-six-light-style';
  style.textContent = `
    .cases-grid.portfolio-six{
      display:grid!important;
      grid-template-columns:repeat(2,minmax(0,1fr))!important;
      gap:46px 28px!important;
      align-items:start!important;
      overflow:visible!important;
    }
    .cases-grid.portfolio-six .portfolio-card-pending,
    .cases-grid.portfolio-six .case-card{
      display:block!important;
      width:100%!important;
      min-width:0!important;
      max-width:100%!important;
      margin:0!important;
      padding:0!important;
      grid-column:auto!important;
      background:transparent!important;
      border:0!important;
      border-radius:0!important;
      box-shadow:none!important;
      overflow:visible!important;
    }
    .cases-grid.portfolio-six .case-viewport{
      position:relative!important;
      width:100%!important;
      max-width:100%!important;
      aspect-ratio:16/9!important;
      height:auto!important;
      overflow:hidden!important;
      border-radius:22px!important;
      background:#e7ecef!important;
      border:1px solid rgba(18,23,27,.09)!important;
      box-shadow:0 18px 38px rgba(24,34,40,.08)!important;
      isolation:isolate!important;
      touch-action:pan-y!important;
    }
    .cases-grid.portfolio-six .case-slides{position:absolute;inset:0;width:100%;height:100%}
    .cases-grid.portfolio-six .case-slide{position:absolute;inset:0;display:none!important;width:100%!important;height:100%!important;object-fit:cover!important;background:#e7ecef!important}
    .cases-grid.portfolio-six .case-slide.is-active{display:block!important}
    .cases-grid.portfolio-six .case-controls{position:absolute;z-index:4;left:12px;right:12px;bottom:12px;display:flex;align-items:center;justify-content:space-between;pointer-events:none}
    .cases-grid.portfolio-six .case-arrows{display:flex;gap:7px;pointer-events:auto}
    .cases-grid.portfolio-six .case-arrow{display:grid;place-items:center;width:38px;height:38px;padding:0;border:1px solid rgba(255,255,255,.42);border-radius:999px;background:rgba(11,16,19,.62);color:#fff;font:400 17px/1 Manrope,sans-serif;cursor:pointer}
    .cases-grid.portfolio-six .case-counter{min-width:54px;padding:8px 10px;border:1px solid rgba(255,255,255,.42);border-radius:999px;background:rgba(251,253,255,.92);color:#12171b;font:500 10px/1 Manrope,sans-serif;letter-spacing:.05em;text-align:center;pointer-events:none}
    .cases-grid.portfolio-six .case-counter b{color:#d85c2b;font-weight:600}
    .cases-grid.portfolio-six .case-meta{display:block!important;padding:13px 2px 0!important;border:0!important;min-height:0!important}
    .cases-grid.portfolio-six .case-meta h3{margin:0 0 4px!important;font-family:Unbounded,sans-serif!important;font-size:clamp(15px,1.25vw,18px)!important;font-weight:300!important;letter-spacing:-.035em!important;color:#12171b!important}
    .cases-grid.portfolio-six .case-meta p{margin:0!important;font-size:11px!important;line-height:1.35!important;color:#68757d!important}
    @media(max-width:980px){
      .cases-grid.portfolio-six{gap:34px 20px!important}
      .cases-grid.portfolio-six .case-viewport{border-radius:18px!important}
    }
    @media(max-width:640px){
      .cases-grid.portfolio-six{display:flex!important;flex-direction:column!important;gap:30px!important}
      .cases-grid.portfolio-six .portfolio-card-pending,.cases-grid.portfolio-six .case-card{width:100%!important;margin:0!important}
      .cases-grid.portfolio-six .case-viewport{border-radius:16px!important;box-shadow:0 12px 26px rgba(24,34,40,.08)!important}
      .cases-grid.portfolio-six .case-meta{padding-top:10px!important}
      .cases-grid.portfolio-six .case-meta h3{font-size:16px!important}
    }
  `;
  document.head.appendChild(style);

  const buildCard = (card, cardIndex) => {
    if (card.dataset.ready === '1') return;
    const title = card.dataset.title;
    const cfg = configs[title];
    const viewport = card.querySelector('.case-viewport');
    if (!cfg || !viewport) return;

    card.className = 'case-card case-balanced';
    const slidesMarkup = cfg.slides.map((src, i) => {
      const fallbackAttr = cfg.fallback ? ` data-fallback="${cfg.fallback}"` : '';
      const eager = cardIndex === 0 && i === 0;
      return `<img class="case-slide${i === 0 ? ' is-active' : ''}" src="${src}"${fallbackAttr} alt="${title} — slide ${i + 1}" loading="${eager ? 'eager' : 'lazy'}" decoding="async">`;
    }).join('');

    viewport.innerHTML = `
      <div class="case-slides">${slidesMarkup}</div>
      <div class="case-controls">
        <div class="case-arrows">
          <button class="case-arrow case-prev" type="button" aria-label="Previous slide">←</button>
          <button class="case-arrow case-next" type="button" aria-label="Next slide">→</button>
        </div>
        <span class="case-counter"><b>1</b> / ${cfg.slides.length}</span>
      </div>`;

    const slides = [...viewport.querySelectorAll('.case-slide')];
    slides.forEach(img => {
      img.addEventListener('error', () => {
        const fallback = img.dataset.fallback;
        if (fallback && img.src !== fallback) {
          img.removeAttribute('data-fallback');
          img.src = fallback;
        }
      }, { once: true });
    });

    const counter = viewport.querySelector('.case-counter b');
    let active = 0;
    const show = value => {
      active = (value + slides.length) % slides.length;
      slides.forEach((slide, i) => slide.classList.toggle('is-active', i === active));
      if (counter) counter.textContent = String(active + 1);
    };
    viewport.querySelector('.case-prev')?.addEventListener('click', () => show(active - 1));
    viewport.querySelector('.case-next')?.addEventListener('click', () => show(active + 1));

    let startX = null;
    viewport.addEventListener('touchstart', e => { startX = e.touches?.[0]?.clientX ?? null; }, { passive:true });
    viewport.addEventListener('touchend', e => {
      if (startX == null) return;
      const endX = e.changedTouches?.[0]?.clientX;
      if (typeof endX === 'number' && Math.abs(endX - startX) > 44) show(active + (endX < startX ? 1 : -1));
      startX = null;
    }, { passive:true });

    card.dataset.ready = '1';
  };

  const activate = () => cards.forEach(buildCard);

  // thecase.js adds .brief-modal only after the legacy original script has completed.
  // Activate after that point so the legacy portfolio code never touches these cards.
  if (document.querySelector('.brief-modal')) {
    activate();
  } else {
    const observer = new MutationObserver(() => {
      if (document.querySelector('.brief-modal')) {
        observer.disconnect();
        activate();
      }
    });
    observer.observe(document.body, { childList:true, subtree:true });
    window.setTimeout(() => { observer.disconnect(); activate(); }, 2500);
  }
})();