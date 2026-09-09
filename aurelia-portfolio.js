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

  const mobileOrder = ['OZON', 'RED FOX', 'AURELIA', 'FABERGÉ', 'ЕЛЕНА ЦВЕТОЧНАЯ', 'JAPANESE MINIMALISM', 'YANDEX TAXI', 'CAT GROOMER'];
  const majorTitles = ['OZON', 'AURELIA', 'YANDEX TAXI'];
  const minorTitles = ['RED FOX', 'FABERGÉ', 'ЕЛЕНА ЦВЕТОЧНАЯ', 'JAPANESE MINIMALISM', 'CAT GROOMER'];

  const injectPortfolioLayoutStyles = () => {
    if (document.getElementById('portfolio-editorial-layout')) return;
    const style = document.createElement('style');
    style.id = 'portfolio-editorial-layout';
    style.textContent = `
      @media (min-width: 981px) {
        .cases-grid.portfolio-composed {
          display: grid !important;
          grid-template-columns: minmax(0, 1.52fr) minmax(330px, 1fr) !important;
          gap: 28px !important;
          align-items: stretch !important;
        }
        .cases-grid.portfolio-composed .cases-column {
          min-width: 0;
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          gap: 28px;
        }
        .cases-grid.portfolio-composed .case-card {
          width: 100% !important;
          min-width: 0 !important;
          grid-column: auto !important;
          margin: 0 !important;
        }
        .cases-grid.portfolio-composed .case-viewport {
          width: 100%;
          aspect-ratio: 16 / 9;
        }
        .cases-grid.portfolio-composed .cases-column-major .case-viewport {
          border-radius: 28px;
        }
        .cases-grid.portfolio-composed .cases-column-minor .case-viewport {
          border-radius: 22px;
        }
        .cases-grid.portfolio-composed .cases-column-major .case-meta h3 {
          font-size: 17px;
        }
        .cases-grid.portfolio-composed .cases-column-minor .case-meta h3 {
          font-size: 14px;
        }
        .cases-grid.portfolio-composed .cases-column-minor .case-meta p {
          font-size: 10px;
        }
      }

      @media (min-width: 641px) and (max-width: 980px) {
        .cases-grid.portfolio-composed {
          display: grid !important;
          grid-template-columns: minmax(0, 1.15fr) minmax(0, .85fr) !important;
          gap: 20px !important;
          align-items: stretch !important;
        }
        .cases-grid.portfolio-composed .cases-column {
          min-width: 0;
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          gap: 20px;
        }
        .cases-grid.portfolio-composed .case-card {
          width: 100% !important;
          grid-column: auto !important;
          margin: 0 !important;
        }
        .cases-grid.portfolio-composed .case-viewport {
          width: 100%;
          aspect-ratio: 16 / 9;
          border-radius: 20px;
        }
        .cases-grid.portfolio-composed .cases-column-minor .case-meta h3 {
          font-size: 12px;
        }
      }

      @media (max-width: 640px) {
        .cases-grid.portfolio-composed {
          display: flex !important;
          flex-direction: column !important;
          gap: 30px !important;
        }
        .cases-grid.portfolio-composed .cases-column {
          display: contents !important;
        }
        .cases-grid.portfolio-composed .case-card {
          width: 100% !important;
          grid-column: auto !important;
          order: var(--mobile-order, 99);
          margin: 0 !important;
        }
        .cases-grid.portfolio-composed .case-viewport {
          width: 100%;
          aspect-ratio: 16 / 9;
          border-radius: 20px;
        }
      }
    `;
    document.head.appendChild(style);
  };

  const ensurePortfolioOrder = () => {
    const grid = document.querySelector('.cases-grid');
    if (!grid) return false;
    if (grid.dataset.composed === '1') return true;

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

  const composePortfolio = () => {
    const grid = document.querySelector('.cases-grid');
    if (!grid || grid.dataset.composed === '1') return true;

    const cards = [...grid.querySelectorAll(':scope > .case-card')];
    if (cards.length < 8 || !cards.every(card => card.dataset.portfolioFixed === '1')) return false;

    const byTitle = new Map(cards.map(card => [card.querySelector('.case-meta h3')?.textContent?.trim(), card]));
    if (!mobileOrder.every(title => byTitle.has(title))) return false;

    mobileOrder.forEach((title, index) => {
      byTitle.get(title)?.style.setProperty('--mobile-order', String(index + 1));
    });

    const majorColumn = document.createElement('div');
    majorColumn.className = 'cases-column cases-column-major';
    const minorColumn = document.createElement('div');
    minorColumn.className = 'cases-column cases-column-minor';

    majorTitles.forEach(title => majorColumn.appendChild(byTitle.get(title)));
    minorTitles.forEach(title => minorColumn.appendChild(byTitle.get(title)));

    grid.replaceChildren(majorColumn, minorColumn);
    grid.classList.add('portfolio-composed');
    grid.dataset.composed = '1';
    return true;
  };

  const repairPortfolio = () => {
    injectPortfolioLayoutStyles();
    ensurePortfolioOrder();

    const grid = document.querySelector('.cases-grid');
    if (!grid) return false;
    if (grid.dataset.composed === '1') return true;

    const cards = [...grid.querySelectorAll('.case-card')];
    if (cards.length < 8) return false;

    const nativeCount = cards.filter(card => card.querySelector('.case-viewport.case-native')).length;
    if (nativeCount < 6) return false;

    cards.forEach(rebuildCard);
    if (!cards.every(card => card.dataset.portfolioFixed === '1')) return false;

    return composePortfolio();
  };

  injectPortfolioLayoutStyles();
  ensurePortfolioOrder();

  if (!repairPortfolio()) {
    let attempts = 0;
    const timer = window.setInterval(() => {
      attempts += 1;
      if (repairPortfolio() || attempts > 120) window.clearInterval(timer);
    }, 100);
  }
})();