(() => {
  const original = document.createElement('script');
  original.src = 'thecase-original.js?v=20260905-hero-copy';

  original.onload = () => {
    // Final hero copy. Russian version leads with the service proposition;
    // English version keeps THE CASE as the brand headline.
    const heroTitle = document.querySelector('.hero-title');
    const heroSubtitle = document.querySelector('.hero-subtitle');
    const heroAccent = document.querySelector('.hero-accent');

    if (heroTitle) {
      heroTitle.dataset.ru = 'Презентации для бизнеса';
      heroTitle.dataset.en = 'THE CASE';
    }
    if (heroSubtitle) {
      heroSubtitle.dataset.ru = 'инструмент, который продаёт';
      heroSubtitle.dataset.en = 'Business. Not just slides.';
    }
    if (heroAccent) {
      heroAccent.dataset.ru = 'Ваш продукт должен выглядеть убедительно.';
      heroAccent.dataset.en = 'Your product should look compelling.';
    }

    const applyHeroLanguage = lang => {
      document.documentElement.lang = lang;
      if (heroTitle) heroTitle.textContent = heroTitle.dataset[lang];
      if (heroSubtitle) heroSubtitle.textContent = heroSubtitle.dataset[lang];
      if (heroAccent) heroAccent.textContent = heroAccent.dataset[lang];

      const metaDescription = document.querySelector('meta[name="description"]');
      if (lang === 'en') {
        document.title = 'THE CASE — Business. Not just slides.';
        if (metaDescription) metaDescription.content = 'THE CASE creates business presentations that make products look compelling and sell the idea clearly.';
      } else {
        document.title = 'Презентации для бизнеса — THE CASE';
        if (metaDescription) metaDescription.content = 'Презентации для бизнеса: инструмент, который продаёт. Ваш продукт должен выглядеть убедительно.';
      }
    };

    let currentLang = document.documentElement.lang === 'en' ? 'en' : 'ru';
    try {
      const saved = localStorage.getItem('thecase-lang');
      if (saved === 'ru' || saved === 'en') currentLang = saved;
    } catch (_) {}
    applyHeroLanguage(currentLang);

    document.querySelectorAll('.lang-btn').forEach(btn => {
      btn.addEventListener('click', () => applyHeroLanguage(btn.dataset.lang));
    });

    // Services: keep the approved pattern background and remove character artwork.
    document.querySelectorAll('.services-art-base, .services-art-hand').forEach(el => {
      el.style.display = 'none';
      el.setAttribute('aria-hidden', 'true');
    });

    const style = document.createElement('style');
    style.textContent = `
      .services{
        background-image:url('assets/services-pattern.webp?v=20260905-pattern')!important;
        background-size:cover!important;
        background-position:center!important;
        background-repeat:no-repeat!important;
      }
      .services-art-base,.services-art-hand{display:none!important}

      html[lang="ru"] .hero-copy{width:min(800px,60vw)}
      html[lang="ru"] .hero-title{
        font-size:clamp(44px,5.7vw,82px);
        line-height:.98;
        letter-spacing:-.055em;
        max-width:800px;
      }
      html[lang="ru"] .hero-subtitle{
        font-size:clamp(21px,2.4vw,34px);
        max-width:680px;
      }
      html[lang="ru"] .hero-accent{font-size:17px;max-width:620px}

      @media(max-width:640px){
        html[lang="ru"] .hero-copy{width:72vw;padding-top:104px}
        html[lang="ru"] .hero-title{font-size:clamp(29px,8.7vw,37px);line-height:1.01;max-width:72vw}
        html[lang="ru"] .hero-subtitle{font-size:clamp(16px,4.8vw,20px);line-height:1.12;max-width:67vw;margin-top:15px}
        html[lang="ru"] .hero-accent{font-size:10px;line-height:1.35;max-width:61vw;margin-top:8px}
      }
    `;
    document.head.appendChild(style);
  };

  document.head.appendChild(original);
})();
