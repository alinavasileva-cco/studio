(() => {
  const original = document.createElement('script');
  original.src = 'thecase-original.js?v=20260905-hero-copy';

  original.onload = () => {
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
    if (heroAccent && heroTitle && heroTitle.parentNode === heroAccent.parentNode) {
      heroTitle.parentNode.insertBefore(heroAccent, heroTitle);
    }

    document.querySelectorAll('.services-art-base, .services-art-hand').forEach(el => {
      el.style.display = 'none';
      el.setAttribute('aria-hidden', 'true');
    });

    const servicesPanel = document.querySelector('.services-panel');
    let presentationOffer = null;
    let websiteOffer = null;

    const createSpec = (labelRu, labelEn, contentNode) => {
      const spec = document.createElement('div');
      spec.className = 'service-spec';
      const label = document.createElement('small');
      label.className = 'service-spec-label';
      label.dataset.ru = labelRu;
      label.dataset.en = labelEn;
      label.textContent = labelRu;
      spec.appendChild(label);
      if (contentNode) spec.appendChild(contentNode);
      return spec;
    };

    const prepareOffer = (offer, type) => {
      if (!offer) return;

      if (!offer.querySelector('.service-visual')) {
        const visual = document.createElement('figure');
        visual.className = `service-visual service-visual-${type}`;
        const img = document.createElement('img');
        img.alt = '';
        img.loading = 'lazy';
        img.decoding = 'async';
        img.dataset.ruSrc = type === 'presentation'
          ? 'assets/service-presentations-approved-ru.svg?v=20260906-services-final'
          : 'assets/service-sites-approved-ru.svg?v=20260906-services-final';
        img.dataset.enSrc = type === 'presentation'
          ? 'assets/service-presentations-approved-en.svg?v=20260906-services-final'
          : 'assets/service-sites-approved-en.svg?v=20260906-services-final';
        visual.appendChild(img);
        const heading = offer.querySelector('h3');
        if (heading) heading.insertAdjacentElement('afterend', visual);
        else offer.prepend(visual);
      }

      const desc = offer.querySelector('.service-desc');
      const result = offer.querySelector('.service-result');
      const pills = [...offer.querySelectorAll('.service-pill')];
      const oldPills = offer.querySelector('.service-pills');

      if (!offer.querySelector('.service-specs')) {
        const specs = document.createElement('div');
        specs.className = 'service-specs';

        if (result) {
          const oldLabel = result.querySelector('small');
          if (oldLabel) oldLabel.remove();
          result.classList.add('service-spec-value');
          specs.appendChild(createSpec('РЕЗУЛЬТАТ', 'OUTPUT', result));
        }

        if (pills[0]) {
          pills[0].classList.add('service-spec-value');
          specs.appendChild(createSpec('СРОК', 'TIMING', pills[0]));
        }
        if (pills[1]) {
          pills[1].classList.add('service-spec-value');
          specs.appendChild(createSpec('СТОИМОСТЬ', 'PRICE', pills[1]));
        }

        if (oldPills) oldPills.remove();
        if (desc) desc.insertAdjacentElement('afterend', specs);
        else offer.appendChild(specs);
      }

      if (!offer.querySelector('.service-brief-cta')) {
        const cta = document.createElement('a');
        cta.className = 'service-brief-cta';
        cta.href = 'https://t.me/AlinaVasileva';
        cta.target = '_blank';
        cta.rel = 'noopener';
        cta.dataset.ru = 'Отправить ТЗ  →';
        cta.dataset.en = 'Send brief  →';
        cta.textContent = cta.dataset.ru;
        offer.appendChild(cta);
      }
    };

    if (servicesPanel) {
      const offers = [...servicesPanel.querySelectorAll('.service-offer')];
      presentationOffer = offers.find(offer => offer.querySelector('[data-ru="Презентации"]')) || null;
      websiteOffer = offers.find(offer => offer.querySelector('[data-ru="Сайты"]')) || null;
      if (presentationOffer) servicesPanel.prepend(presentationOffer);
      prepareOffer(presentationOffer, 'presentation');
      prepareOffer(websiteOffer, 'website');
    }

    const setText = (el, lang, ru, en) => {
      if (!el) return;
      el.dataset.ru = ru;
      el.dataset.en = en;
      el.textContent = lang === 'en' ? en : ru;
    };

    const applyLanguage = lang => {
      document.documentElement.lang = lang;
      if (heroTitle) heroTitle.textContent = heroTitle.dataset[lang];
      if (heroSubtitle) heroSubtitle.textContent = heroSubtitle.dataset[lang];
      if (heroAccent) heroAccent.textContent = heroAccent.dataset[lang];

      const metaDescription = document.querySelector('meta[name="description"]');
      if (lang === 'en') {
        document.title = 'THE CASE — Business. Not just slides.';
        if (metaDescription) metaDescription.content = 'THE CASE creates business presentations and websites with clear argumentation and a strong visual code.';
      } else {
        document.title = 'Презентации и сайты для бизнеса — THE CASE';
        if (metaDescription) metaDescription.content = 'THE CASE — презентации и сайты для бизнеса. Концепция. Аргументация. Визуальный код.';
      }

      setText(document.querySelector('.head-cta'), lang, 'ЗАКАЗАТЬ ↗', 'START ↗');
      setText(document.querySelector('.hero-actions .button.primary'), lang, 'ЗАКАЗАТЬ ПРЕЗЕНТАЦИЮ ↗', 'START A PROJECT ↗');

      document.querySelectorAll('.hero-facts strong, .hero-facts small').forEach(el => {
        if (lang === 'ru') el.textContent = '';
      });

      if (presentationOffer) {
        setText(presentationOffer.querySelector('.service-desc'), lang,
          'Для бизнеса, конференций, выступлений, учёбы',
          'For business, conferences, talks and study');
        const resultParts = presentationOffer.querySelectorAll('.service-result b span');
        if (resultParts[0]) setText(resultParts[0], lang, '', '');
        if (resultParts[1]) setText(resultParts[1], lang, ' + ', ' + ');
        const pills = presentationOffer.querySelectorAll('.service-spec > .service-pill');
        if (pills[0]) setText(pills[0], lang, 'от 1 дня', 'from 1 day');
        if (pills[1]) setText(pills[1], lang, 'от 5 000 рублей', 'from 5,000 ₽');
      }

      if (websiteOffer) {
        setText(websiteOffer.querySelector('.service-desc'), lang,
          'Одностраничники / лендинги',
          'One-page websites / landing pages');
        const pills = websiteOffer.querySelectorAll('.service-spec > .service-pill');
        if (pills[0]) setText(pills[0], lang, 'от 3 дней', 'from 3 days');
        if (pills[1]) setText(pills[1], lang, 'от 10 000 рублей', 'from 10,000 ₽');
      }

      document.querySelectorAll('.service-spec-label').forEach(label => {
        label.textContent = label.dataset[lang] || label.textContent;
      });
      document.querySelectorAll('.service-visual img').forEach(img => {
        img.src = lang === 'en' ? img.dataset.enSrc : img.dataset.ruSrc;
      });
      document.querySelectorAll('.service-brief-cta').forEach(cta => {
        cta.textContent = cta.dataset[lang];
      });

      setText(document.querySelector('.work h2'), lang, 'ПОРТФОЛИО', 'PORTFOLIO');
      const categories = {
        'FABERGÉ': ['Культура', 'Culture'],
        'RED FOX': ['Продукт', 'Product'],
        'ЕЛЕНА ЦВЕТОЧНАЯ': ['Бренд', 'Brand'],
        'JAPANESE MINIMALISM': ['Editorial', 'Editorial'],
        'YANDEX TAXI': ['Recruitment', 'Recruitment'],
        'CAT GROOMER': ['Сервис', 'Service']
      };
      document.querySelectorAll('.case-card').forEach(card => {
        const title = card.querySelector('h3')?.textContent?.trim();
        const category = card.querySelector('.case-meta p');
        if (title && category && categories[title]) {
          setText(category, lang, categories[title][0], categories[title][1]);
        }
      });

      const contactTitleParts = document.querySelectorAll('.contact-main h2 span');
      if (contactTitleParts[0]) setText(contactTitleParts[0], lang, 'КОНТАКТЫ', 'CONTACTS');
      if (contactTitleParts[1]) setText(contactTitleParts[1], lang, '', '');
      const contactCopy = document.querySelector('.contact-copy');
      if (contactCopy && lang === 'ru') contactCopy.textContent = '';
    };

    let currentLang = document.documentElement.lang === 'en' ? 'en' : 'ru';
    try {
      const saved = localStorage.getItem('thecase-lang');
      if (saved === 'ru' || saved === 'en') currentLang = saved;
    } catch (_) {}
    applyLanguage(currentLang);

    document.querySelectorAll('.lang-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        currentLang = btn.dataset.lang;
        applyLanguage(currentLang);
      });
    });

    const style = document.createElement('style');
    style.textContent = `
      .services{
        background:#eef4f7!important;
        background-image:radial-gradient(circle at 50% 42%,rgba(255,255,255,.68),rgba(255,255,255,0) 58%)!important;
        border-bottom:0!important;
        padding:58px 0 72px!important;
      }
      .services>.shell{width:min(1480px,calc(100% - 64px))!important}
      .services .section-head{display:none!important}
      .services-art-base,.services-art-hand{display:none!important}
      .services-stage{min-height:0!important;display:block!important;padding:0!important}
      .services-panel{
        width:100%!important;min-height:0!important;display:grid!important;
        grid-template-columns:minmax(0,1fr) minmax(0,1fr)!important;
        gap:0!important;align-items:stretch!important;
        background:transparent!important;border:0!important;border-radius:0!important;
        box-shadow:none!important;overflow:visible!important;
      }
      .services-panel:before,.services-panel:after{content:none!important;display:none!important}
      .service-offer,.service-offer:first-of-type{
        padding:0 46px 0 38px!important;margin:0!important;min-height:0!important;
        background:transparent!important;transform:none!important;align-self:stretch!important;
      }
      .service-offer+.service-offer{
        border-left:1px solid rgba(18,23,27,.14)!important;
        border-top:0!important;padding-left:52px!important;padding-right:22px!important;
      }
      .service-icon{display:none!important}
      .service-offer h3{
        margin:0 0 18px!important;
        font-family:Georgia,'Times New Roman',serif!important;
        font-size:clamp(72px,7.2vw,112px)!important;
        line-height:.9!important;font-weight:400!important;letter-spacing:-.055em!important;
        color:#0b0f12!important;
      }
      .service-visual{
        margin:8px 0 24px!important;width:100%!important;
        aspect-ratio:16/10!important;overflow:visible!important;
        display:flex!important;align-items:center!important;justify-content:center!important;
      }
      .service-visual img{
        display:block!important;width:100%!important;height:100%!important;object-fit:contain!important;
        filter:drop-shadow(0 18px 24px rgba(34,46,54,.10))!important;
      }
      .service-desc{
        margin:0 0 30px!important;font-size:clamp(23px,2vw,31px)!important;
        color:#5d6870!important;line-height:1.16!important;letter-spacing:-.025em!important;
        max-width:620px!important;
      }
      .service-specs{
        display:grid!important;grid-template-columns:1fr 1fr 1fr!important;
        gap:0!important;margin:0 0 24px!important;width:100%!important;
      }
      .service-spec{
        min-width:0!important;padding:0 24px 2px 0!important;
      }
      .service-spec+.service-spec{
        border-left:1px solid rgba(18,23,27,.12)!important;
        padding-left:36px!important;
      }
      .service-spec-label{
        display:block!important;margin:0 0 12px!important;
        font-family:var(--m)!important;font-size:11px!important;font-weight:500!important;
        letter-spacing:.20em!important;color:#66727a!important;text-transform:uppercase!important;
      }
      .service-result{
        margin:0!important;padding:0!important;border:0!important;display:block!important;
      }
      .service-result b{
        display:block!important;max-width:none!important;font-size:clamp(19px,1.65vw,26px)!important;
        font-weight:400!important;line-height:1.12!important;color:#11171c!important;letter-spacing:-.025em!important;
      }
      .service-result em{font-style:normal!important;color:#d85c2b!important}
      .service-pill{
        display:block!important;min-height:0!important;padding:0!important;border:0!important;border-radius:0!important;
        background:transparent!important;color:#11171c!important;font-size:clamp(18px,1.5vw,24px)!important;
        line-height:1.18!important;white-space:normal!important;
      }
      .service-pills,.service-pills-bottom{display:contents!important}
      .service-brief-cta{
        display:flex!important;align-items:center!important;justify-content:center!important;width:100%!important;
        margin-top:22px!important;min-height:72px!important;padding:0 22px!important;
        border:0!important;border-radius:12px!important;background:#db5524!important;color:#fff!important;
        font-family:var(--m)!important;font-size:clamp(18px,1.65vw,25px)!important;font-weight:400!important;
        letter-spacing:-.015em!important;text-decoration:none!important;
        box-shadow:none!important;transition:transform .18s ease,background .18s ease!important;
      }
      .service-brief-cta:hover{transform:translateY(-2px)!important;background:#c94b1d!important}

      .hero h1 .hero-accent{order:-1;margin:0 0 14px 2px;font-family:var(--m);font-size:12px;line-height:1.35;font-weight:500;letter-spacing:.02em;max-width:560px}
      html[lang="ru"] .hero-copy{width:min(800px,60vw)}
      html[lang="ru"] .hero-title{font-size:clamp(44px,5.7vw,82px);line-height:.98;letter-spacing:-.055em;max-width:800px}
      html[lang="ru"] .hero-subtitle{font-size:clamp(21px,2.4vw,34px);max-width:680px}

      @media(max-width:900px){
        .services{padding:36px 0 56px!important}
        .services>.shell{width:auto!important;margin-left:0!important;margin-right:0!important}
        .services-stage{padding:0!important}
        .services-panel{grid-template-columns:1fr!important}
        .service-offer,.service-offer:first-of-type{
          width:100%!important;padding:42px 30px 52px!important;margin:0!important;transform:none!important;
        }
        .service-offer:first-of-type{padding-top:34px!important}
        .service-offer+.service-offer{
          border-left:0!important;border-top:1px solid rgba(18,23,27,.08)!important;
          padding:54px 30px 44px!important;
        }
        .service-offer h3{
          font-size:clamp(60px,16vw,86px)!important;line-height:.92!important;margin-bottom:18px!important;
        }
        .service-visual{
          aspect-ratio:16/10!important;margin:2px -6px 26px!important;width:calc(100% + 12px)!important;
        }
        .service-desc{
          font-size:clamp(21px,6vw,28px)!important;line-height:1.16!important;margin-bottom:30px!important;
          max-width:90%!important;
        }
        .service-specs{grid-template-columns:1fr!important;margin-bottom:8px!important}
        .service-spec{
          padding:18px 0 22px!important;border-top:1px solid rgba(18,23,27,.10)!important;
        }
        .service-spec+.service-spec{
          border-left:0!important;padding-left:0!important;
        }
        .service-spec-label{font-size:10px!important;margin-bottom:9px!important}
        .service-result b,.service-pill{font-size:20px!important;line-height:1.22!important}
        .service-brief-cta{
          min-height:64px!important;margin-top:22px!important;border-radius:12px!important;
          font-size:18px!important;
        }
      }

      @media(max-width:640px){
        .services{padding-top:18px!important;padding-bottom:38px!important}
        .service-offer,.service-offer:first-of-type{padding-left:24px!important;padding-right:24px!important}
        .service-offer h3{font-size:clamp(58px,17vw,78px)!important}
        .service-visual{margin-left:-8px!important;width:calc(100% + 16px)!important}
        .service-desc{max-width:100%!important;font-size:22px!important}
        .service-result b,.service-pill{font-size:19px!important}
        .service-brief-cta{font-size:17px!important;min-height:60px!important}

        .hero h1 .hero-accent{font-size:9px;line-height:1.3;max-width:62vw;margin:0 0 10px 1px}
        html[lang="ru"] .hero-copy{width:72vw;padding-top:104px}
        html[lang="ru"] .hero-title{font-size:clamp(29px,8.7vw,37px);line-height:1.01;max-width:72vw}
        html[lang="ru"] .hero-subtitle{font-size:clamp(16px,4.8vw,20px);line-height:1.12;max-width:67vw;margin-top:15px}
      }
    `;
    document.head.appendChild(style);
  };

  document.head.appendChild(original);
})();