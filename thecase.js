(() => {
  const original = document.createElement('script');
  original.src = 'thecase-original.js?v=20260905-hero-copy';

  original.onload = () => {
    // Approved copy only. Layout, imagery and visual styling stay unchanged.
    const heroTitle = document.querySelector('.hero-title');
    const heroSubtitle = document.querySelector('.hero-subtitle');
    const heroAccent = document.querySelector('.hero-accent');

    if (heroTitle) {
      heroTitle.dataset.ru = 'Презентации и сайты для бизнеса';
      heroTitle.dataset.en = 'THE CASE';
    }
    if (heroSubtitle) {
      heroSubtitle.dataset.ru = 'Концепция. Аргументация. Визуальный код.';
      heroSubtitle.dataset.en = 'Business. Not just slides.';
    }
    if (heroAccent) {
      heroAccent.dataset.ru = 'Ваш продукт должен выглядеть убедительно.';
      heroAccent.dataset.en = 'Your product should look compelling.';
    }

    // Product slogan is a small overline above the main hero title.
    if (heroAccent && heroTitle && heroTitle.parentNode === heroAccent.parentNode) {
      heroTitle.parentNode.insertBefore(heroAccent, heroTitle);
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
        document.title = 'Презентации и сайты для бизнеса — THE CASE';
        if (metaDescription) metaDescription.content = 'THE CASE — презентации и сайты для бизнеса. Концепция. Аргументация. Визуальный код.';
      }
    };

    const applyApprovedCopy = lang => {
      if (lang !== 'ru') return;

      const setRu = (el, text) => {
        if (!el) return;
        el.dataset.ru = text;
        el.textContent = text;
      };

      setRu(document.querySelector('.head-cta'), 'ЗАКАЗАТЬ ↗');
      setRu(document.querySelector('.hero-actions .button.primary'), 'ЗАКАЗАТЬ ПРЕЗЕНТАЦИЮ ↗');

      // Old explanatory hero labels are removed textually only.
      document.querySelectorAll('.hero-facts strong, .hero-facts small').forEach(el => {
        el.dataset.ru = '';
        el.textContent = '';
      });

      setRu(document.querySelector('.services .section-note'), '');

      const presentationOffer = [...document.querySelectorAll('.service-offer')].find(offer =>
        offer.querySelector('[data-ru="Презентации"]')
      );
      if (presentationOffer) {
        setRu(presentationOffer.querySelector('.service-desc'), 'Для бизнеса, конференций, выступлений, учёбы');
        const resultParts = presentationOffer.querySelectorAll('.service-result b span');
        if (resultParts[0]) {
          resultParts[0].dataset.ru = '';
          resultParts[0].textContent = '';
        }
        if (resultParts[1]) {
          resultParts[1].dataset.ru = ' + ';
          resultParts[1].textContent = ' + ';
        }
      }

      setRu(document.querySelector('.work h2'), 'ПОРТФОЛИО');

      const categories = {
        'FABERGÉ': 'Культура',
        'RED FOX': 'Продукт',
        'ЕЛЕНА ЦВЕТОЧНАЯ': 'Бренд',
        'JAPANESE MINIMALISM': 'Editorial',
        'YANDEX TAXI': 'Recruitment',
        'CAT GROOMER': 'Сервис'
      };
      document.querySelectorAll('.case-card').forEach(card => {
        const title = card.querySelector('h3')?.textContent?.trim();
        const category = card.querySelector('.case-meta p');
        if (title && category && categories[title]) setRu(category, categories[title]);
      });

      const contactTitleParts = document.querySelectorAll('.contact-main h2 span');
      if (contactTitleParts[0]) setRu(contactTitleParts[0], 'КОНТАКТЫ');
      if (contactTitleParts[1]) setRu(contactTitleParts[1], '');
      setRu(document.querySelector('.contact-copy'), '');
    };

    let currentLang = document.documentElement.lang === 'en' ? 'en' : 'ru';
    try {
      const saved = localStorage.getItem('thecase-lang');
      if (saved === 'ru' || saved === 'en') currentLang = saved;
    } catch (_) {}
    applyHeroLanguage(currentLang);
    applyApprovedCopy(currentLang);

    document.querySelectorAll('.lang-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const lang = btn.dataset.lang;
        applyHeroLanguage(lang);
        applyApprovedCopy(lang);
      });
    });

    // Services only: approved pattern, no character artwork, no card container.
    document.querySelectorAll('.services-art-base, .services-art-hand').forEach(el => {
      el.style.display = 'none';
      el.setAttribute('aria-hidden', 'true');
    });

    const servicesPanel = document.querySelector('.services-panel');
    if (servicesPanel) {
      const offers = [...servicesPanel.querySelectorAll('.service-offer')];
      const presentations = offers.find(offer => offer.querySelector('[data-ru="Презентации"]'));
      if (presentations) servicesPanel.prepend(presentations);
    }

    const style = document.createElement('style');
    style.textContent = `
      .services{
        background-image:url('assets/services-pattern-final.svg?v=20260905-final')!important;
        background-size:cover!important;
        background-position:center!important;
        background-repeat:no-repeat!important;
        border-bottom:0!important;
        padding-top:76px!important;
        padding-bottom:90px!important;
      }
      .services .section-head{display:none!important}
      .services-art-base,.services-art-hand{display:none!important}
      .services-stage{
        min-height:0!important;
        display:block!important;
        padding:56px 0 48px!important;
      }
      .services-panel{
        width:100%!important;
        min-height:0!important;
        display:grid!important;
        grid-template-columns:minmax(0,1fr) minmax(0,1fr)!important;
        gap:clamp(56px,8vw,128px)!important;
        background:transparent!important;
        border:0!important;
        border-radius:0!important;
        box-shadow:none!important;
        overflow:visible!important;
      }
      .services-panel:before,.services-panel:after{content:none!important;display:none!important}
      .service-offer,
      .service-offer:first-of-type{
        padding:0!important;
        min-height:0!important;
        background:transparent!important;
      }
      .service-offer+.service-offer{border-left:0!important;border-top:0!important}
      .service-icon{
        width:54px!important;
        height:54px!important;
        border-radius:0!important;
        background:transparent!important;
        border:0!important;
        box-shadow:none!important;
        place-items:start!important;
      }
      .service-icon svg{width:42px!important;height:42px!important;stroke:var(--orange)!important;stroke-width:1.5!important}
      .service-offer h3{
        margin:18px 0 10px!important;
        font-size:clamp(36px,4.4vw,62px)!important;
        line-height:1!important;
        letter-spacing:-.055em!important;
      }
      .service-desc{
        margin:0!important;
        font-size:clamp(16px,1.5vw,21px)!important;
        color:#4c5961!important;
      }
      .service-result{
        margin-top:34px!important;
        padding-top:0!important;
        border-top:0!important;
      }
      .service-result small{
        margin-bottom:8px!important;
        color:#6b777f!important;
        font-size:10px!important;
        letter-spacing:.18em!important;
      }
      .service-result b{
        max-width:430px!important;
        font-size:clamp(18px,1.6vw,23px)!important;
        font-weight:400!important;
      }
      .service-pills,
      .service-pills-bottom{
        gap:16px 30px!important;
        margin-top:30px!important;
      }
      .service-pill{
        min-height:0!important;
        padding:0!important;
        border:0!important;
        border-radius:0!important;
        background:transparent!important;
        color:#c9562c!important;
        font-size:14px!important;
      }

      .hero h1 .hero-accent{
        order:-1;
        margin:0 0 14px 2px;
        font-family:var(--m);
        font-size:12px;
        line-height:1.35;
        font-weight:500;
        letter-spacing:.02em;
        max-width:560px;
      }

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

      @media(max-width:640px){
        .services{
          padding-top:54px!important;
          padding-bottom:74px!important;
          background-position:center top!important;
        }
        .services-stage{padding:26px 8px 20px!important}
        .services-panel{
          grid-template-columns:1fr!important;
          gap:84px!important;
        }
        .service-icon{width:46px!important;height:46px!important}
        .service-icon svg{width:36px!important;height:36px!important}
        .service-offer h3{
          margin-top:14px!important;
          font-size:clamp(36px,11vw,46px)!important;
          line-height:.98!important;
        }
        .service-desc{font-size:17px!important;line-height:1.35!important}
        .service-result{margin-top:28px!important}
        .service-result b{font-size:18px!important;line-height:1.35!important}
        .service-pills,.service-pills-bottom{display:grid!important;gap:14px!important;margin-top:25px!important}
        .service-pill{font-size:13px!important}

        .hero h1 .hero-accent{
          font-size:9px;
          line-height:1.3;
          max-width:62vw;
          margin:0 0 10px 1px;
        }
        html[lang="ru"] .hero-copy{width:72vw;padding-top:104px}
        html[lang="ru"] .hero-title{font-size:clamp(29px,8.7vw,37px);line-height:1.01;max-width:72vw}
        html[lang="ru"] .hero-subtitle{font-size:clamp(16px,4.8vw,20px);line-height:1.12;max-width:67vw;margin-top:15px}
      }
    `;
    document.head.appendChild(style);
  };

  document.head.appendChild(original);
})();