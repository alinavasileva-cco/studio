(() => {
  const metaDescription = document.querySelector('meta[name="description"]');
  const langButtons = [...document.querySelectorAll('.lang-btn')];
  const translatables = [...document.querySelectorAll('[data-ru][data-en]')];

  function setLang(lang) {
    document.documentElement.lang = lang;
    langButtons.forEach(btn => btn.classList.toggle('is-active', btn.dataset.lang === lang));
    translatables.forEach(el => {
      const value = el.dataset[lang];
      if (value != null) el.textContent = value;
    });
    if (lang === 'en') {
      document.title = 'THE CASE — Business presentations. Not just slides.';
      if (metaDescription) metaDescription.content = 'THE CASE creates business presentations: structure, copy, visual system and ready-to-use PPTX/PDF.';
    } else {
      document.title = 'THE CASE — презентации для бизнеса';
      if (metaDescription) metaDescription.content = 'THE CASE создаёт бизнес-презентации: структура, текст, визуальная система и готовый PPTX/PDF.';
    }
    try { localStorage.setItem('thecase-lang', lang); } catch (_) {}
  }

  langButtons.forEach(btn => btn.addEventListener('click', () => setLang(btn.dataset.lang)));
  let initial = 'ru';
  try {
    const saved = localStorage.getItem('thecase-lang');
    if (saved === 'ru' || saved === 'en') initial = saved;
  } catch (_) {}
  setLang(initial);

  // Requested services pass: presentations first, compact spacing, local character art.
  const servicesPanel = document.querySelector('.services-panel');
  if (servicesPanel) {
    const offers = [...servicesPanel.querySelectorAll('.service-offer')];
    const presentations = offers.find(offer => offer.querySelector('[data-ru="Презентации"]'));
    if (presentations) servicesPanel.prepend(presentations);
  }

  const serviceArtSources = [
    'assets/services-decor-sharp.webp?v=20260904-3',
    'assets/services-decor-vivid.webp?v=20260904-3',
    'assets/hero-character-hd.png?v=20260904-3'
  ];
  document.querySelectorAll('.services-art').forEach(img => {
    let sourceIndex = 0;
    const loadSource = () => { img.src = serviceArtSources[sourceIndex]; };
    img.onerror = () => {
      sourceIndex += 1;
      if (sourceIndex < serviceArtSources.length) loadSource();
    };
    loadSource();
  });

  const cases = [
    { title: 'FABERGÉ', ids: ['1IGgu1qRFx-1TeHR2fuVQ9Cf_kSzLcCsk','15zOarlo3gqSp4Bl1sQFt2gG45qdrLV9A','1DW5TNnWwACtL7Y2jvpzJV1ylbU1abphr'] },
    { title: 'RED FOX', ids: ['19aSZt58h59aTRnrhRn6Vm31QSfMR88Xs','1TN1lRXM1LuvVjaDD0BiMTqWNC9eeZLcu'] },
    { title: 'ЕЛЕНА ЦВЕТОЧНАЯ', ids: ['14NBiuR8zeW_rBP7IoOOgGpocYwqFOaHg','1agKSGk0q9L6pL3j3ZJPyd09JRWVmy6wN','1bzY34Gs-FZcON-BflE2S0g2YMOpUUFmi','1Q6mJvDWTf53mEriXhlnUrsYArziBCikz','1gsQQPS80pxylWN9VAap4CiZVvgGbvcnu'] },
    { title: 'JAPANESE MINIMALISM', ids: ['1B8r2lBHFj6zuk8UKVbgHX_Hk8bnXJfl5','126y4HZl3n0I_x7kj5o8VvtOb_ed9p4gw','1qjl-AOdwkp05cu-zFh6uN33uldYZPzza'] },
    { title: 'YANDEX TAXI', ids: ['1ChbJ8aVs7Cq-teQ7ipsdiMHRpq5kyAf_','1YmEHUd39cddtY8T_mxNUMviS5ZeSDqyq','1H44MBDgFywp32nkBMHekDjt8Maws6ekx','1pK7wnMM0iREze4BsNNb5xPGZWsNWbM-e'] },
    { title: 'CAT GROOMER', ids: ['1kzfjYZEVkpZefDxLWVDvM_5h3ZfqI7ud','1KN4eR6e_Dd3Y6nkBv_SwKtyl7FiRXbsW'] }
  ];

  const style = document.createElement('style');
  style.textContent = `
    .services{padding-top:72px!important;padding-bottom:72px!important}
    .services .section-head{margin-bottom:28px!important}
    .services-stage{min-height:560px!important;padding:0 0 18px!important}
    .services-panel{min-height:470px!important}
    .services-panel:before,.services-panel:after{content:none!important;display:none!important}
    .service-icon{border-radius:14px!important;box-shadow:0 6px 16px rgba(32,44,52,.06)!important}
    .service-offer h3{margin-top:26px!important}
    @media(max-width:640px){
      .services{padding-top:44px!important;padding-bottom:48px!important}
      .services .section-head{margin-bottom:20px!important}
      .services .section-note{margin-top:10px!important;line-height:1.4!important}
      .services-stage{min-height:0!important;padding:0!important}
      .services-panel{min-height:0!important;border-radius:25px!important}
      .service-offer,.service-offer:first-of-type,.service-offer+.service-offer{min-height:0!important;padding:27px 22px 26px 24px!important}
      .service-offer+.service-offer{border-left:0!important;border-top:1px solid var(--line)!important}
      .service-icon{width:46px!important;height:46px!important;border-radius:12px!important}
      .service-icon svg{width:24px!important;height:24px!important}
      .service-offer h3{font-size:25px!important;margin:20px 0 8px!important}
      .service-desc{font-size:14px!important}
      .service-pills{margin-top:18px!important;gap:7px!important}
      .service-pill{min-height:34px!important;font-size:9.5px!important;padding:0 10px!important}
      .service-result{margin-top:21px!important;padding-top:16px!important}
      .service-result b{font-size:14px!important}
      .service-pills-bottom{margin-top:17px!important}
      .services-art{width:292px!important;max-width:none!important;left:-126px!important;bottom:18px!important}
      .services-art-hand{clip-path:polygon(66% 0,100% 0,100% 100%,55% 100%,55% 45%,61% 33%)!important}
    }
    @media(max-width:390px){
      .services{padding-top:40px!important;padding-bottom:44px!important}
      .services-art{width:276px!important;left:-119px!important;bottom:20px!important}
      .service-offer,.service-offer:first-of-type,.service-offer+.service-offer{padding:24px 19px 23px 21px!important}
    }

    .work{background:linear-gradient(180deg,#f4f7f9 0%,#eef3f6 100%)}
    .work-head{align-items:flex-end!important;margin-bottom:40px!important}
    .cases-grid{display:grid!important;grid-template-columns:repeat(12,minmax(0,1fr))!important;gap:52px 28px!important;align-items:start!important;overflow:visible!important}
    .case-card{min-width:0!important;max-width:100%!important;background:transparent!important;border:0!important;border-radius:0!important;overflow:visible!important;box-shadow:none!important}
    .case-card.case-large{grid-column:span 7!important}.case-card.case-medium{grid-column:span 5!important}
    .case-card:nth-child(5){grid-column:span 7!important}.case-card:nth-child(6){grid-column:span 5!important}
    .case-viewport.case-native{position:relative!important;width:100%!important;max-width:100%!important;aspect-ratio:16/9!important;height:auto!important;overflow:hidden!important;border-radius:24px!important;background:#e7ecef!important;border:1px solid rgba(18,23,27,.10)!important;box-shadow:0 22px 48px rgba(24,34,40,.10)!important;touch-action:pan-y!important;overscroll-behavior-x:contain!important;isolation:isolate!important}
    .case-native .case-slides{position:absolute;inset:0;width:100%;height:100%}
    .case-native .case-slide{position:absolute;inset:0;width:100%!important;height:100%!important;max-width:none!important;object-fit:cover!important;display:none!important;background:#e7ecef!important}
    .case-native .case-slide.is-active{display:block!important}
    .case-native .case-controls{position:absolute;z-index:3;left:14px;right:14px;bottom:14px;display:flex;align-items:center;justify-content:space-between;pointer-events:none}
    .case-native .case-arrows{display:flex;gap:7px;pointer-events:auto}
    .case-native .case-arrow{display:grid;place-items:center;width:40px;height:40px;padding:0;border:1px solid rgba(255,255,255,.42);border-radius:999px;background:rgba(11,16,19,.60);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);color:#fff;font:400 18px/1 Manrope,sans-serif;cursor:pointer;transition:background .15s ease,border-color .15s ease}
    .case-native .case-arrow:hover{background:rgba(216,92,43,.94);border-color:rgba(216,92,43,.94)}
    .case-native .case-counter{min-width:58px;padding:9px 12px;border:1px solid rgba(255,255,255,.42);border-radius:999px;background:rgba(251,253,255,.90);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);color:#12171b;font:500 11px/1 Manrope,sans-serif;letter-spacing:.05em;text-align:center;box-shadow:0 7px 18px rgba(0,0,0,.08);pointer-events:none}
    .case-native .case-counter b{color:#d85c2b;font-weight:600}
    .case-meta{display:block!important;padding:15px 2px 0!important;border:0!important;min-height:0!important}
    .case-meta h3{margin:0 0 5px!important;font-family:Unbounded,sans-serif!important;font-size:clamp(15px,1.35vw,20px)!important;font-weight:300!important;letter-spacing:-.035em!important;color:#12171b!important}
    .case-meta p{margin:0!important;font-size:12px!important;line-height:1.35!important;color:#68757d!important}
    .case-card:nth-child(3),.case-card:nth-child(4){margin-top:22px}
    @media(max-width:980px){
      .cases-grid{gap:38px 22px!important}
      .case-card.case-large,.case-card.case-medium,.case-card:nth-child(5),.case-card:nth-child(6){grid-column:span 6!important}
      .case-card:nth-child(3),.case-card:nth-child(4){margin-top:0}
      .case-viewport.case-native{border-radius:20px!important}
    }
    @media(max-width:640px){
      .work{padding-top:58px!important;padding-bottom:72px!important}
      .work-head{display:flex!important;align-items:flex-end!important;gap:14px!important;margin-bottom:24px!important}
      .work-head h2{max-width:72vw!important;font-size:29px!important;line-height:.98!important}
      .portfolio-link{font-size:9px!important;white-space:nowrap!important;margin-bottom:2px!important}
      .cases-grid{display:block!important;width:100%!important;max-width:100%!important}
      .case-card,.case-card.case-large,.case-card.case-medium,.case-card:nth-child(5),.case-card:nth-child(6){display:block!important;width:100%!important;max-width:100%!important;margin:0 0 34px!important;grid-column:auto!important}
      .case-viewport.case-native{width:100%!important;max-width:100%!important;aspect-ratio:16/9!important;border-radius:17px!important;box-shadow:0 14px 30px rgba(24,34,40,.09)!important}
      .case-native .case-controls{left:10px;right:10px;bottom:10px}
      .case-native .case-arrow{width:36px;height:36px;font-size:16px;background:rgba(11,16,19,.66)}
      .case-native .case-counter{min-width:54px;padding:8px 10px;font-size:10px}
      .case-meta{padding:11px 2px 0!important}
      .case-meta h3{font-size:17px!important;line-height:1.08!important;margin-bottom:4px!important}
      .case-meta p{font-size:11px!important}
    }
    @media(max-width:390px){
      .work-head h2{font-size:26px!important;max-width:70vw!important}
      .case-card,.case-card.case-large,.case-card.case-medium{margin-bottom:30px!important}
      .case-viewport.case-native{border-radius:15px!important}
    }
  `;
  document.head.appendChild(style);

  const driveSources = id => [
    `https://drive.google.com/thumbnail?id=${id}&sz=w1600`,
    `https://lh3.googleusercontent.com/d/${id}=w1600`,
    `https://drive.google.com/uc?export=view&id=${id}`
  ];

  const imageMarkup = (id, title, slideIndex, cardIndex) => {
    const sources = driveSources(id);
    const eager = cardIndex === 0 && slideIndex === 0;
    return `<img class="case-slide${slideIndex === 0 ? ' is-active' : ''}" src="${sources[0]}" data-sources="${sources.map(encodeURIComponent).join('|')}" data-source-index="0" alt="${title} — slide ${slideIndex + 1}" loading="${eager ? 'eager' : 'lazy'}" decoding="async" referrerpolicy="no-referrer">`;
  };

  document.querySelectorAll('.case-card').forEach((card, cardIndex) => {
    const data = cases[cardIndex];
    const viewport = card.querySelector('.case-viewport');
    if (!data || !viewport) return;

    viewport.classList.add('case-native');
    viewport.innerHTML = `
      <div class="case-slides">
        ${data.ids.map((id, i) => imageMarkup(id, data.title, i, cardIndex)).join('')}
      </div>
      <div class="case-controls">
        <div class="case-arrows">
          <button class="case-arrow case-prev" type="button" aria-label="Previous slide">←</button>
          <button class="case-arrow case-next" type="button" aria-label="Next slide">→</button>
        </div>
        <span class="case-counter"><b>1</b> / ${data.ids.length}</span>
      </div>`;

    const slides = [...viewport.querySelectorAll('.case-slide')];
    slides.forEach(img => {
      img.addEventListener('error', () => {
        const sources = (img.dataset.sources || '').split('|').filter(Boolean).map(decodeURIComponent);
        const nextIndex = Number(img.dataset.sourceIndex || 0) + 1;
        if (nextIndex < sources.length) {
          img.dataset.sourceIndex = String(nextIndex);
          img.src = sources[nextIndex];
        }
      });
    });

    const current = viewport.querySelector('.case-counter b');
    let index = 0;
    const show = value => {
      index = (value + slides.length) % slides.length;
      slides.forEach((slide, i) => slide.classList.toggle('is-active', i === index));
      current.textContent = String(index + 1);
    };

    viewport.querySelector('.case-prev').addEventListener('click', () => show(index - 1));
    viewport.querySelector('.case-next').addEventListener('click', () => show(index + 1));

    let startX = 0, startY = 0;
    viewport.addEventListener('touchstart', e => {
      const t = e.changedTouches[0];
      startX = t.clientX; startY = t.clientY;
    }, { passive: true });
    viewport.addEventListener('touchend', e => {
      const t = e.changedTouches[0];
      const dx = t.clientX - startX;
      const dy = t.clientY - startY;
      if (Math.abs(dx) > 42 && Math.abs(dx) > Math.abs(dy) * 1.25) show(index + (dx < 0 ? 1 : -1));
    }, { passive: true });
  });
})();
