(() => {
  const configs = {
    'OZON': {
      slides: ['p2', 'p3', 'p4'].map(pageId => `https://docs.google.com/presentation/d/1vCxqqiwonb6E-VVP4qggV6tVw7P4bHWeFDSti5zBu4c/export/png?id=1vCxqqiwonb6E-VVP4qggV6tVw7P4bHWeFDSti5zBu4c&pageid=${pageId}`),
      embed: 'https://docs.google.com/presentation/d/1vCxqqiwonb6E-VVP4qggV6tVw7P4bHWeFDSti5zBu4c/embed?start=false&loop=false&delayms=60000'
    },
    'AURELIA': {
      slides: ['p1', 'p2', 'p3', 'p4'].map(pageId => `https://docs.google.com/presentation/d/1sPhXPXcCsKAWD3QMxlUK-hCWob6zspTFyYv56O0reNQ/export/png?id=1sPhXPXcCsKAWD3QMxlUK-hCWob6zspTFyYv56O0reNQ&pageid=${pageId}`),
      embed: 'https://docs.google.com/presentation/d/1sPhXPXcCsKAWD3QMxlUK-hCWob6zspTFyYv56O0reNQ/embed?start=false&loop=false&delayms=60000'
    },
    'FABERGÉ': {
      slides: ['assets/cases/faberge-01.webp?v=20260904-1','assets/cases/faberge-02.webp?v=20260904-1','assets/cases/faberge-03.webp?v=20260904-1'],
      embed: 'https://docs.google.com/presentation/d/1U2kSLYvgoq1DzTv52fAxs1wjbvIYNylOVjtapuNP16Y/embed?start=false&loop=false&delayms=60000'
    },
    'RED FOX': {
      slides: ['assets/cases/redfox-01.webp?v=20260904-1','assets/cases/redfox-02.webp?v=20260904-1'],
      embed: 'https://docs.google.com/presentation/d/1Fzz3W0-_Ir70kt8s0_VbsAvhzs0CCuOJIk1M24HGLCI/embed?start=false&loop=false&delayms=60000'
    },
    'ЕЛЕНА ЦВЕТОЧНАЯ': {
      slides: ['assets/cases/elena-01.webp?v=20260904-1','assets/cases/elena-02.webp?v=20260904-1','assets/cases/elena-03.webp?v=20260904-1','assets/cases/elena-04.webp?v=20260904-1','assets/cases/elena-05.webp?v=20260904-1'],
      embed: 'https://docs.google.com/presentation/d/1c4yJK7twEgL1nSS_sHkAUkabu2yGLQ7xvQeS2rNyO6k/embed?start=false&loop=false&delayms=60000'
    },
    'JAPANESE MINIMALISM': {
      slides: ['assets/cases/japanese-01.webp?v=20260904-1','assets/cases/japanese-02.webp?v=20260904-1','assets/cases/japanese-03.webp?v=20260904-1'],
      embed: 'https://docs.google.com/presentation/d/1LxFhOK6EKMKNn4aIy_Y48HlyfuW-tE1ORtpe8n4ozJk/embed?start=false&loop=false&delayms=60000'
    },
    'YANDEX TAXI': {
      slides: ['assets/cases/yandex-01.webp?v=20260904-1','assets/cases/yandex-02.webp?v=20260904-1','assets/cases/yandex-03.webp?v=20260904-1','assets/cases/yandex-04.webp?v=20260904-1'],
      embed: 'https://docs.google.com/presentation/d/1O0LuxPKg917YbccV5rgVue16HbaZwSwicNI_N67Q5Cc/embed?start=false&loop=false&delayms=60000'
    },
    'CAT GROOMER': {
      slides: ['assets/cases/cat-01.webp?v=20260904-1','assets/cases/cat-02.webp?v=20260904-1'],
      embed: 'https://docs.google.com/presentation/d/1deDoDf3BO3Hf01T_wUzVa-VcDSmMoZYLjBKlyNK1hFk/embed?start=false&loop=false&delayms=60000'
    }
  };

  const ensurePortfolioOrder = () => {
    const grid = document.querySelector('.cases-grid');
    if (!grid) return false;

    let ozonCard = [...grid.querySelectorAll('.case-card')].find(card => card.querySelector('.case-meta h3')?.textContent?.trim() === 'OZON');
    if (!ozonCard) {
      ozonCard = document.createElement('article');
      ozonCard.className = 'case-card case-large case-ozon';
      ozonCard.innerHTML = `
        <div class="case-viewport"><iframe allowfullscreen loading="lazy" referrerpolicy="no-referrer-when-downgrade" src="${configs.OZON.embed}" title="OZON presentation"></iframe></div>
        <div class="case-meta"><div><h3>OZON</h3><p data-en="Retail media" data-ru="Retail media">Retail media</p></div></div>`;
    }

    grid.insertBefore(ozonCard, grid.firstElementChild);

    const redFoxCard = [...grid.querySelectorAll('.case-card')].find(card => card.querySelector('.case-meta h3')?.textContent?.trim() === 'RED FOX');
    if (redFoxCard) {
      redFoxCard.classList.remove('case-medium');
      redFoxCard.classList.add('case-large');
      grid.insertBefore(redFoxCard, ozonCard.nextElementSibling);
    }

    return true;
  };

  const rebuildCard = card => {
    if (card.dataset.portfolioFixed === '1') return;
    const title = card.querySelector('.case-meta h3')?.textContent?.trim();
    const config = configs[title];
    const viewport = card.querySelector('.case-viewport');
    if (!config || !viewport) return;

    viewport.classList.add('case-native');
    viewport.classList.remove('is-fallback');
    viewport.innerHTML = `
      <div class="case-slides">
        ${config.slides.map((src, i) => `<img class="case-slide${i === 0 ? ' is-active' : ''}" src="${src}" alt="${title} — slide ${i + 1}" loading="${i === 0 ? 'eager' : 'lazy'}" decoding="async">`).join('')}
      </div>
      <iframe class="case-fallback" loading="lazy" allowfullscreen src="${config.embed}" title="${title} presentation"></iframe>
      <div class="case-controls">
        <div class="case-arrows">
          <button class="case-arrow case-prev" type="button" aria-label="Previous slide">←</button>
          <button class="case-arrow case-next" type="button" aria-label="Next slide">→</button>
        </div>
        <span class="case-counter"><b>1</b> / ${config.slides.length}</span>
      </div>`;

    const slides = [...viewport.querySelectorAll('.case-slide')];
    const counter = viewport.querySelector('.case-counter b');
    let active = 0;
    let failed = 0;

    const show = value => {
      if (!slides.length || viewport.classList.contains('is-fallback')) return;
      active = (value + slides.length) % slides.length;
      slides.forEach((slide, i) => slide.classList.toggle('is-active', i === active));
      if (counter) counter.textContent = String(active + 1);
    };

    slides.forEach(slide => {
      slide.addEventListener('error', () => {
        failed += 1;
        if (slide.classList.contains('is-active') || failed >= slides.length) viewport.classList.add('is-fallback');
      }, { once: true });
      slide.addEventListener('load', () => {
        if (slide.classList.contains('is-active')) viewport.classList.remove('is-fallback');
      }, { once: true });
    });

    viewport.querySelector('.case-prev')?.addEventListener('click', () => show(active - 1));
    viewport.querySelector('.case-next')?.addEventListener('click', () => show(active + 1));

    let startX = null;
    viewport.addEventListener('touchstart', event => {
      startX = event.touches?.[0]?.clientX ?? null;
    }, { passive: true });
    viewport.addEventListener('touchend', event => {
      if (startX == null) return;
      const endX = event.changedTouches?.[0]?.clientX;
      if (typeof endX === 'number' && Math.abs(endX - startX) > 46) show(active + (endX < startX ? 1 : -1));
      startX = null;
    }, { passive: true });

    card.dataset.portfolioFixed = '1';
  };

  const repairPortfolio = () => {
    ensurePortfolioOrder();

    const grid = document.querySelector('.cases-grid');
    if (!grid) return false;

    const cards = [...grid.querySelectorAll('.case-card')];
    if (cards.length < 8) return false;

    // thecase-original.js has finished when at least six existing cards are native.
    // At that point rebuild every card by its own title, not by DOM index.
    const nativeCount = cards.filter(card => card.querySelector('.case-viewport.case-native')).length;
    if (nativeCount < 6) return false;

    cards.forEach(rebuildCard);
    return cards.every(card => card.dataset.portfolioFixed === '1');
  };

  ensurePortfolioOrder();

  if (!repairPortfolio()) {
    let attempts = 0;
    const timer = window.setInterval(() => {
      attempts += 1;
      if (repairPortfolio() || attempts > 120) window.clearInterval(timer);
    }, 100);
  }
})();