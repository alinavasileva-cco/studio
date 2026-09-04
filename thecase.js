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

  // Services: presentations first; character peeks from behind the panel.
  const servicesPanel = document.querySelector('.services-panel');
  if (servicesPanel) {
    const offers = [...servicesPanel.querySelectorAll('.service-offer')];
    const presentations = offers.find(offer => offer.querySelector('[data-ru="Презентации"]'));
    if (presentations) servicesPanel.prepend(presentations);
  }

  const baseArt = document.querySelector('.services-art-base');
  const handArt = document.querySelector('.services-art-hand');
  if (baseArt) {
    baseArt.src = 'assets/hero-character-hd.png?v=20260904-4';
    baseArt.onerror = () => { baseArt.src = 'assets/services-decor-sharp.webp?v=20260904-4'; };
  }
  if (handArt) {
    const handSources = [
      'assets/services-decor-sharp.webp?v=20260904-4',
      'assets/services-decor-vivid.webp?v=20260904-4'
    ];
    let handSourceIndex = 0;
    handArt.src = handSources[0];
    handArt.onerror = () => {
      handSourceIndex += 1;
      if (handSourceIndex < handSources.length) handArt.src = handSources[handSourceIndex];
    };
  }

  const cases = [
    {
      title: 'FABERGÉ',
      files: ['faberge-01.webp','faberge-02.webp','faberge-03.webp'],
      ids: ['1IGgu1qRFx-1TeHR2fuVQ9Cf_kSzLcCsk','15zOarlo3gqSp4Bl1sQFt2gG45qdrLV9A','1DW5TNnWwACtL7Y2jvpzJV1ylbU1abphr'],
      embed: 'https://docs.google.com/presentation/d/1U2kSLYvgoq1DzTv52fAxs1wjbvIYNylOVjtapuNP16Y/embed?start=false&loop=false&delayms=60000'
    },
    {
      title: 'RED FOX',
      files: ['redfox-01.webp','redfox-02.webp'],
      ids: ['19aSZt58h59aTRnrhRn6Vm31QSfMR88Xs','1TN1lRXM1LuvVjaDD0BiMTqWNC9eeZLcu'],
      embed: 'https://docs.google.com/presentation/d/1Fzz3W0-_Ir70kt8s0_VbsAvhzs0CCuOJIk1M24HGLCI/embed?start=false&loop=false&delayms=60000'
    },
    {
      title: 'ЕЛЕНА ЦВЕТОЧНАЯ',
      files: ['elena-01.webp','elena-02.webp','elena-03.webp','elena-04.webp','elena-05.webp'],
      ids: ['14NBiuR8zeW_rBP7IoOOgGpocYwqFOaHg','1agKSGk0q9L6pL3j3ZJPyd09JRWVmy6wN','1bzY34Gs-FZcON-BflE2S0g2YMOpUUFmi','1Q6mJvDWTf53mEriXhlnUrsYArziBCikz','1gsQQPS80pxylWN9VAap4CiZVvgGbvcnu'],
      embed: 'https://docs.google.com/presentation/d/1c4yJK7twEgL1nSS_sHkAUkabu2yGLQ7xvQeS2rNyO6k/embed?start=false&loop=false&delayms=60000'
    },
    {
      title: 'JAPANESE MINIMALISM',
      files: ['japanese-01.webp','japanese-02.webp','japanese-03.webp'],
      ids: ['1B8r2lBHFj6zuk8UKVbgHX_Hk8bnXJfl5','126y4HZl3n0I_x7kj5o8VvtOb_ed9p4gw','1qjl-AOdwkp05cu-zFh6uN33uldYZPzza'],
      embed: 'https://docs.google.com/presentation/d/1LxFhOK6EKMKNn4aIy_Y48HlyfuW-tE1ORtpe8n4ozJk/embed?start=false&loop=false&delayms=60000'
    },
    {
      title: 'YANDEX TAXI',
      files: ['yandex-01.webp','yandex-02.webp','yandex-03.webp','yandex-04.webp'],
      ids: ['1ChbJ8aVs7Cq-teQ7ipsdiMHRpq5kyAf_','1YmEHUd39cddtY8T_mxNUMviS5ZeSDqyq','1H44MBDgFywp32nkBMHekDjt8Maws6ekx','1pK7wnMM0iREze4BsNNb5xPGZWsNWbM-e'],
      embed: 'https://docs.google.com/presentation/d/1O0LuxPKg917YbccV5rgVue16HbaZwSwicNI_N67Q5Cc/embed?start=false&loop=false&delayms=60000'
    },
    {
      title: 'CAT GROOMER',
      files: ['cat-01.webp','cat-02.webp'],
      ids: ['1kzfjYZEVkpZefDxLWVDvM_5h3ZfqI7ud','1KN4eR6e_Dd3Y6nkBv_SwKtyl7FiRXbsW'],
      embed: 'https://docs.google.com/presentation/d/1deDoDf3BO3Hf01T_wUzVa-VcDSmMoZYLjBKlyNK1hFk/embed?start=false&loop=false&delayms=60000'
    }
  ];

  const style = document.createElement('style');
  style.textContent = `
    .services{padding-top:68px!important;padding-bottom:62px!important}
    .services .section-head{margin-bottom:24px!important}
    .services-stage{min-height:500px!important;padding:0 0 12px!important;position:relative!important}
    .services-panel{min-height:450px!important}
    .services-panel:before,.services-panel:after{content:none!important;display:none!important}
    .service-icon{border-radius:13px!important;box-shadow:0 5px 15px rgba(32,44,52,.055)!important}
    .service-offer h3{margin-top:24px!important}
    .services-art-base{width:430px!important;max-width:none!important;left:-106px!important;top:-32px!important;bottom:auto!important;z-index:4!important;clip-path:polygon(0 0,67% 0,67% 100%,0 100%)!important;filter:drop-shadow(0 16px 24px rgba(32,44,52,.08))!important}
    .services-art-hand{width:345px!important;max-width:none!important;left:-118px!important;top:96px!important;bottom:auto!important;z-index:6!important;clip-path:polygon(62% 0,100% 0,100% 100%,52% 100%,52% 46%,59% 31%)!important}
    @media(max-width:640px){
      .services{padding-top:40px!important;padding-bottom:42px!important}
      .services .section-head{margin-bottom:16px!important}
      .services .section-note{margin-top:8px!important;line-height:1.35!important}
      .services-stage{min-height:0!important;padding:0!important}
      .services-panel{min-height:0!important;border-radius:24px!important;overflow:visible!important}
      .service-offer,.service-offer+.service-offer{min-height:0!important;padding:23px 20px 22px 22px!important}
      .service-offer:first-of-type{min-height:0!important;padding:23px 20px 22px 82px!important}
      .service-offer+.service-offer{border-left:0!important;border-top:1px solid var(--line)!important}
      .service-icon{width:43px!important;height:43px!important;border-radius:11px!important}
      .service-icon svg{width:22px!important;height:22px!important}
      .service-offer h3{font-size:24px!important;margin:17px 0 7px!important}
      .service-desc{font-size:13.5px!important}
      .service-pills{margin-top:15px!important;gap:6px!important}
      .service-pill{min-height:31px!important;font-size:9px!important;padding:0 9px!important}
      .service-result{margin-top:18px!important;padding-top:14px!important}
      .service-result b{font-size:13.5px!important}
      .service-pills-bottom{margin-top:14px!important}
      .services-art-base{width:252px!important;left:-101px!important;top:-14px!important;bottom:auto!important;z-index:4!important;clip-path:polygon(0 0,68% 0,68% 100%,0 100%)!important}
      .services-art-hand{width:286px!important;left:-121px!important;top:206px!important;bottom:auto!important;z-index:6!important;clip-path:polygon(64% 0,100% 0,100% 100%,52% 100%,52% 45%,59% 32%)!important}
    }
    @media(max-width:390px){
      .services{padding-top:38px!important;padding-bottom:40px!important}
      .service-offer,.service-offer+.service-offer{padding:21px 18px 20px 20px!important}
      .service-offer:first-of-type{padding:21px 18px 20px 78px!important}
      .services-art-base{width:240px!important;left:-97px!important;top:-10px!important}
      .services-art-hand{width:276px!important;left:-118px!important;top:200px!important}
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
    .case-native .case-fallback{position:absolute;inset:0;width:100%;height:100%;border:0;display:none;background:#e7ecef}
    .case-native.is-fallback .case-fallback{display:block}
    .case-native.is-fallback .case-slides{display:none}
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
      .work{padding-top:54px!important;padding-bottom:66px!important}
      .work-head{display:flex!important;align-items:flex-end!important;gap:14px!important;margin-bottom:22px!important}
      .work-head h2{max-width:72vw!important;font-size:29px!important;line-height:.98!important}
      .portfolio-link{font-size:9px!important;white-space:nowrap!important;margin-bottom:2px!important}
      .cases-grid{display:block!important;width:100%!important;max-width:100%!important}
      .case-card,.case-card.case-large,.case-card.case-medium,.case-card:nth-child(5),.case-card:nth-child(6){display:block!important;width:100%!important;max-width:100%!important;margin:0 0 32px!important;grid-column:auto!important}
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
      .case-card,.case-card.case-large,.case-card.case-medium{margin-bottom:29px!important}
      .case-viewport.case-native{border-radius:15px!important}
    }
  `;
  document.head.appendChild(style);

  const driveSources = id => [
    `https://drive.google.com/thumbnail?id=${id}&sz=w1600`,
    `https://lh3.googleusercontent.com/d/${id}=w1600`,
    `https://drive.google.com/uc?export=view&id=${id}`
  ];

  const imageMarkup = (file, id, title, slideIndex, cardIndex) => {
    const sources = [
      `assets/cases/${file}?v=20260904-1`,
      ...driveSources(id)
    ];
    const eager = cardIndex === 0 && slideIndex === 0;
    return `<img class="case-slide${slideIndex === 0 ? ' is-active' : ''}" src="${sources[0]}" data-sources="${sources.map(encodeURIComponent).join('|')}" data-source-index="0" alt="${title} — slide ${slideIndex + 1}" loading="${eager ? 'eager' : 'lazy'}" decoding="async">`;
  };

  document.querySelectorAll('.case-card').forEach((card, cardIndex) => {
    const data = cases[cardIndex];
    const viewport = card.querySelector('.case-viewport');
    if (!data || !viewport) return;

    viewport.classList.add('case-native');
    viewport.innerHTML = `
      <div class="case-slides">
        ${data.files.map((file, i) => imageMarkup(file, data.ids[i], data.title, i, cardIndex)).join('')}
      </div>
      <iframe class="case-fallback" loading="lazy" allowfullscreen src="${data.embed}" title="${data.title} presentation"></iframe>
      <div class="case-controls">
        <div class="case-arrows">
          <button class="case-arrow case-prev" type="button" aria-label="Previous slide">←</button>
          <button class="case-arrow case-next" type="button" aria-label="Next slide">→</button>
        </div>
        <span class="case-counter"><b>1</b> / ${data.files.length}</span>
      </div>`;

    const slides = [...viewport.querySelectorAll('.case-slide')];
    let failedSlides = 0;
    slides.forEach(img => {
      img.addEventListener('error', () => {
        const sources = (img.dataset.sources || '').split('|').filter(Boolean).map(decodeURIComponent);
        const nextIndex = Number(img.dataset.sourceIndex || 0) + 1;
        if (nextIndex < sources.length) {
          img.dataset.sourceIndex = String(nextIndex);
          img.src = sources[nextIndex];
        } else {
          failedSlides += 1;
          if (failedSlides >= slides.length) viewport.classList.add('is-fallback');
        }
      });
    });

    const current = viewport.querySelector('.case-counter b');
    let index = 0;
    const show = value => {
      if (!slides.length || viewport.classList.contains('is-fallback')) return;
      index = (value + slides.length) % slides.length;
      slides.forEach((slide, i) => slide.classList.toggle('is-active', i === index));
      current.textContent = String(index + 1);
    };

    viewport.querySelector('.case-prev').addEventListener('click', () => show(index - 1));
    viewport.querySelector('.case-next').addEventListener('click', () => show(index + 1));

    let startX = 0;
    let startY = 0;
    viewport.addEventListener('touchstart', e => {
      const t = e.changedTouches[0];
      startX = t.clientX;
      startY = t.clientY;
    }, { passive: true });
    viewport.addEventListener('touchend', e => {
      const t = e.changedTouches[0];
      const dx = t.clientX - startX;
      const dy = t.clientY - startY;
      if (Math.abs(dx) > 42 && Math.abs(dx) > Math.abs(dy) * 1.25) show(index + (dx < 0 ? 1 : -1));
    }, { passive: true });
  });
})();
