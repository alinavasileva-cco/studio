(() => {
  const presentationId = '1sPhXPXcCsKAWD3QMxlUK-hCWob6zspTFyYv56O0reNQ';
  const slideIds = ['p1', 'p2', 'p3', 'p4'];
  const embed = `https://docs.google.com/presentation/d/${presentationId}/embed?start=false&loop=false&delayms=60000`;
  const slideUrl = pageId => `https://docs.google.com/presentation/d/${presentationId}/export/png?id=${presentationId}&pageid=${pageId}`;

  const mount = () => {
    const grid = document.querySelector('.cases-grid');
    if (!grid || grid.querySelector('.case-aurelia')) return false;

    const nativeExisting = grid.querySelectorAll('.case-viewport.case-native');
    if (nativeExisting.length < 6) return false;

    const card = document.createElement('article');
    card.className = 'case-card case-large case-aurelia';
    card.innerHTML = `
      <div class="case-viewport case-native">
        <div class="case-slides">
          ${slideIds.map((id, i) => `<img class="case-slide${i === 0 ? ' is-active' : ''}" src="${slideUrl(id)}" alt="AURELIA — slide ${i + 1}" loading="${i === 0 ? 'eager' : 'lazy'}" decoding="async">`).join('')}
        </div>
        <iframe class="case-fallback" loading="lazy" allowfullscreen src="${embed}" title="AURELIA presentation"></iframe>
        <div class="case-controls">
          <div class="case-arrows">
            <button class="case-arrow case-prev" type="button" aria-label="Previous slide">←</button>
            <button class="case-arrow case-next" type="button" aria-label="Next slide">→</button>
          </div>
          <div class="case-counter"><b>1</b> / 4</div>
        </div>
      </div>
      <div class="case-meta"><div><h3>AURELIA</h3><p data-ru="Beauty" data-en="Beauty">Beauty</p></div></div>
    `;

    grid.prepend(card);

    const viewport = card.querySelector('.case-viewport');
    const slides = [...card.querySelectorAll('.case-slide')];
    const counter = card.querySelector('.case-counter b');
    let active = 0;

    const show = next => {
      active = (next + slides.length) % slides.length;
      slides.forEach((slide, i) => slide.classList.toggle('is-active', i === active));
      if (counter) counter.textContent = String(active + 1);
    };

    card.querySelector('.case-prev')?.addEventListener('click', () => show(active - 1));
    card.querySelector('.case-next')?.addEventListener('click', () => show(active + 1));

    let touchX = null;
    viewport?.addEventListener('touchstart', event => {
      touchX = event.touches?.[0]?.clientX ?? null;
    }, { passive: true });
    viewport?.addEventListener('touchend', event => {
      if (touchX == null) return;
      const endX = event.changedTouches?.[0]?.clientX;
      if (typeof endX === 'number' && Math.abs(endX - touchX) > 46) {
        show(active + (endX < touchX ? 1 : -1));
      }
      touchX = null;
    }, { passive: true });

    slides.forEach(slide => {
      slide.addEventListener('error', () => {
        if (slide.classList.contains('is-active')) viewport.classList.add('is-fallback');
      }, { once: true });
      slide.addEventListener('load', () => {
        if (slide.classList.contains('is-active')) viewport.classList.remove('is-fallback');
      }, { once: true });
    });

    const syncLanguage = () => {
      const category = card.querySelector('.case-meta p');
      if (category) category.textContent = document.documentElement.lang === 'en' ? category.dataset.en : category.dataset.ru;
    };
    syncLanguage();
    document.querySelectorAll('.lang-btn').forEach(btn => btn.addEventListener('click', () => window.setTimeout(syncLanguage, 0)));

    const style = document.createElement('style');
    style.textContent = `
      @media(min-width:981px){
        .cases-grid>.case-card:nth-child(1){grid-column:span 7!important}
        .cases-grid>.case-card:nth-child(2){grid-column:span 5!important}
        .cases-grid>.case-card:nth-child(3){grid-column:span 5!important}
        .cases-grid>.case-card:nth-child(4){grid-column:span 7!important}
        .cases-grid>.case-card:nth-child(5){grid-column:span 7!important}
        .cases-grid>.case-card:nth-child(6){grid-column:span 5!important}
        .cases-grid>.case-card:nth-child(7){grid-column:span 7!important}
      }
      @media(max-width:980px) and (min-width:641px){
        .cases-grid>.case-card{grid-column:span 6!important}
      }
      @media(max-width:640px){
        .cases-grid>.case-card{grid-column:auto!important}
      }
    `;
    document.head.appendChild(style);

    return true;
  };

  if (!mount()) {
    let attempts = 0;
    const timer = window.setInterval(() => {
      attempts += 1;
      if (mount() || attempts > 80) window.clearInterval(timer);
    }, 100);
  }
})();