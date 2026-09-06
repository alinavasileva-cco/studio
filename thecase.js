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

    if (servicesPanel) {
      const offers = [...servicesPanel.querySelectorAll('.service-offer')];
      presentationOffer = offers.find(offer => offer.querySelector('[data-ru="Презентации"]')) || null;
      websiteOffer = offers.find(offer => offer.querySelector('[data-ru="Сайты"]')) || null;
      if (presentationOffer) servicesPanel.prepend(presentationOffer);

      const addVisual = (offer, type) => {
        if (!offer || offer.querySelector('.service-visual')) return;
        const visual = document.createElement('figure');
        visual.className = `service-visual service-visual-${type}`;
        const img = document.createElement('img');
        img.alt = '';
        img.loading = 'lazy';
        img.decoding = 'async';
        img.dataset.ruSrc = type === 'presentation' ? 'assets/service-presentations-ru.svg' : 'assets/service-sites-ru.svg';
        img.dataset.enSrc = type === 'presentation' ? 'assets/service-presentations-en.svg' : 'assets/service-sites-en.svg';
        visual.appendChild(img);
        const desc = offer.querySelector('.service-desc');
        if (desc) desc.insertAdjacentElement('afterend', visual);
        else offer.prepend(visual);
      };

      addVisual(presentationOffer, 'presentation');
      addVisual(websiteOffer, 'website');

      [presentationOffer, websiteOffer].forEach(offer => {
        if (!offer || offer.querySelector('.service-brief-cta')) return;
        const cta = document.createElement('a');
        cta.className = 'service-brief-cta';
        cta.href = 'https://t.me/AlinaVasileva';
        cta.target = '_blank';
        cta.rel = 'noopener';
        cta.dataset.ru = 'ОТПРАВИТЬ ТЗ ↗';
        cta.dataset.en = 'SEND BRIEF ↗';
        offer.appendChild(cta);
      });
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
        const pills = presentationOffer.querySelectorAll('.service-pill');
        if (pills[0]) setText(pills[0], lang, '◷ от 1 дня', '◷ from 1 day');
        if (pills[1]) setText(pills[1], lang, '₽ от 5 000 рублей', '₽ from 5,000');
      }

      if (websiteOffer) {
        setText(websiteOffer.querySelector('.service-desc'), lang,
          'Одностраничники / лендинги',
          'One-page websites / landing pages');
        const pills = websiteOffer.querySelectorAll('.service-pill');
        if (pills[0]) setText(pills[0], lang, '◷ от 3 дней', '◷ from 3 days');
        if (pills[1]) setText(pills[1], lang, '₽ от 10 000 рублей', '₽ from 10,000');
      }

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
        background:#eef3f6!important;
        background-image:linear-gradient(180deg,#eef3f6 0%,#f8fbfd 52%,#eef3f6 100%)!important;
        border-bottom:0!important;
        padding-top:72px!important;
        padding-bottom:88px!important;
      }
      .services .section-head{display:none!important}
      .services-art-base,.services-art-hand{display:none!important}
      .services-stage{min-height:0!important;display:block!important;padding:38px 0 30px!important}
      .services-panel{
        width:100%!important;min-height:0!important;display:grid!important;
        grid-template-columns:minmax(0,1fr) minmax(0,1fr)!important;
        gap:clamp(54px,6vw,98px)!important;align-items:start!important;
        background:transparent!important;border:0!important;border-radius:0!important;
        box-shadow:none!important;overflow:visible!important;
      }
      .services-panel:before,.services-panel:after{content:none!important;display:none!important}
      .service-offer,.service-offer:first-of-type{
        padding:0!important;margin:0!important;min-height:0!important;background:transparent!important;
        transform:none!important;align-self:start!important;
      }
      .service-offer+.service-offer{border-left:0!important;border-top:0!important}
      .service-icon{display:none!important}
      .service-offer h3{
        margin:0 0 10px!important;font-size:clamp(38px,4.2vw,62px)!important;
        line-height:1!important;letter-spacing:-.055em!important;
      }
      .service-desc{margin:0!important;font-size:clamp(16px,1.45vw,20px)!important;color:#4c5961!important;line-height:1.35!important}
      .service-visual{margin:28px 0 26px!important;width:100%!important;aspect-ratio:16/9!important;overflow:visible!important}
      .service-visual img{display:block!important;width:100%!important;height:100%!important;object-fit:contain!important}
      .service-result{margin-top:20px!important;padding-top:0!important;border-top:0!important}
      .service-result small{margin-bottom:8px!important;color:#6b777f!important;font-size:10px!important;letter-spacing:.18em!important}
      .service-result b{max-width:430px!important;font-size:clamp(18px,1.55vw,22px)!important;font-weight:400!important}
      .service-pills,.service-pills-bottom{gap:14px 26px!important;margin-top:22px!important}
      .service-pill{min-height:0!important;padding:0!important;border:0!important;border-radius:0!important;background:transparent!important;color:#c9562c!important;font-size:14px!important}
      .service-brief-cta{
        display:flex!important;align-items:center!important;justify-content:center!important;width:100%!important;
        margin-top:28px!important;min-height:54px!important;padding:0 22px!important;
        border:0!important;border-radius:16px!important;background:#d85c2b!important;color:#fff!important;
        font-family:var(--m)!important;font-size:14px!important;font-weight:600!important;
        letter-spacing:.035em!important;text-decoration:none!important;box-shadow:0 12px 28px rgba(216,92,43,.14)!important;
      }
      .service-brief-cta:hover{transform:translateY(-1px)!important;background:#c95126!important}
      .hero h1 .hero-accent{order:-1;margin:0 0 14px 2px;font-family:var(--m);font-size:12px;line-height:1.35;font-weight:500;letter-spacing:.02em;max-width:560px}
      html[lang="ru"] .hero-copy{width:min(800px,60vw)}
      html[lang="ru"] .hero-title{font-size:clamp(44px,5.7vw,82px);line-height:.98;letter-spacing:-.055em;max-width:800px}
      html[lang="ru"] .hero-subtitle{font-size:clamp(21px,2.4vw,34px);max-width:680px}

      @media(max-width:640px){
        .services{padding-top:38px!important;padding-bottom:62px!important}
        .services-stage{padding:18px 0 10px!important}
        .services-panel{grid-template-columns:1fr!important;gap:58px!important}
        .service-offer,.service-offer:first-of-type{width:100%!important;padding:0!important;margin:0!important;transform:none!important}
        .service-offer h3{font-size:clamp(38px,11vw,48px)!important;line-height:.98!important}
        .service-desc{font-size:17px!important;line-height:1.35!important}
        .service-visual{margin:22px -4px 20px!important;width:calc(100% + 8px)!important}
        .service-result{margin-top:18px!important}.service-result b{font-size:18px!important;line-height:1.35!important}
        .service-pills,.service-pills-bottom{display:grid!important;gap:11px!important;margin-top:18px!important}
        .service-pill{font-size:13px!important}
        .service-brief-cta{margin-top:22px!important;min-height:50px!important;border-radius:14px!important;font-size:13px!important}
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