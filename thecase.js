(() => {
  const original = document.createElement('script');
  original.src = 'thecase-original.js?v=20260905-hero-copy';

  original.onload = () => {
    const heroTitle = document.querySelector('.hero-title');
    const heroSubtitle = document.querySelector('.hero-subtitle');
    const heroAccent = document.querySelector('.hero-accent');
    const heroShell = document.querySelector('.hero-shell');

    if (heroTitle) {
      heroTitle.dataset.ru = 'Презентации и сайты для бизнеса';
      heroTitle.dataset.en = 'THE CASE';
    }
    if (heroSubtitle) {
      heroSubtitle.dataset.ru = 'Концепция. Аргументация. Визуальный код.';
      heroSubtitle.dataset.en = 'Concept. Argumentation. Visual code.';
    }
    if (heroAccent) {
      heroAccent.dataset.ru = 'Ваш продукт должен выглядеть убедительно.';
      heroAccent.dataset.en = 'Your product should look compelling.';
    }

    if (heroAccent && heroTitle && heroTitle.parentNode === heroAccent.parentNode) {
      heroTitle.parentNode.insertBefore(heroAccent, heroTitle);
    }

    if (heroSubtitle && heroShell) {
      heroSubtitle.classList.add('hero-bottom-line');
      heroShell.appendChild(heroSubtitle);
    }

    const heroFacts = document.querySelector('.hero-facts');
    if (heroFacts) heroFacts.style.display = 'none';

    const services = document.querySelector('#services');
    let servicesTitle = null;
    let servicesSubhead = null;
    let servicesCopy = null;
    let servicesCta = null;
    const portfolioLinkLabel = document.querySelector('.portfolio-link span');

    if (services) {
      const shell = services.querySelector('.shell');
      if (shell) {
        shell.innerHTML = `
          <div class="services-master">
            <h2 class="services-master-title" data-ru="УСЛУГИ" data-en="SERVICES">УСЛУГИ</h2>
            <h3 class="services-master-subhead" data-ru="Презентации и сайты" data-en="Presentations and websites">Презентации и сайты</h3>
            <p class="services-master-copy" data-ru="Для бизнеса, конференций, выступлений и учёбы." data-en="For business, conferences, talks and study.">Для бизнеса, конференций, выступлений и учёбы.</p>
            <figure class="services-master-visual">
              <img src="assets/service-combo-premium-final.png?v=20260907-final-png" alt="THE CASE — презентации и сайты" loading="eager" decoding="async" />
            </figure>
            <a class="services-master-cta" href="https://t.me/AlinaVasileva" target="_blank" rel="noopener" data-ru="Отправить ТЗ →" data-en="Send brief →">Отправить ТЗ →</a>
          </div>
        `;
        servicesTitle = shell.querySelector('.services-master-title');
        servicesSubhead = shell.querySelector('.services-master-subhead');
        servicesCopy = shell.querySelector('.services-master-copy');
        servicesCta = shell.querySelector('.services-master-cta');
      }
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
        document.title = 'THE CASE — Presentations and websites for business';
        if (metaDescription) metaDescription.content = 'THE CASE creates business presentations and websites with clear argumentation and a strong visual code.';
      } else {
        document.title = 'Презентации и сайты для бизнеса — THE CASE';
        if (metaDescription) metaDescription.content = 'THE CASE — презентации и сайты для бизнеса. Концепция. Аргументация. Визуальный код.';
      }

      setText(document.querySelector('.head-cta'), lang, 'ЗАКАЗАТЬ ↗', 'START ↗');
      setText(document.querySelector('.hero-actions .button.primary'), lang, 'ЗАКАЗАТЬ ПРЕЗЕНТАЦИЮ ↗', 'START A PROJECT ↗');

      setText(servicesTitle, lang, 'УСЛУГИ', 'SERVICES');
      setText(servicesSubhead, lang, 'Презентации и сайты', 'Presentations and websites');
      setText(servicesCopy, lang, 'Для бизнеса, конференций, выступлений и учёбы.', 'For business, conferences, talks and study.');
      setText(servicesCta, lang, 'Отправить ТЗ →', 'Send brief →');
      setText(portfolioLinkLabel, lang, 'БОЛЬШЕ РАБОТ', 'MORE WORK');

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
      .hero h1 .hero-accent{order:-1;margin:0 0 14px 2px;font-family:var(--m);font-size:12px;line-height:1.35;font-weight:500;letter-spacing:.02em;max-width:560px}
      html[lang="ru"] .hero-copy{width:min(800px,60vw)}
      html[lang="ru"] .hero-title{font-size:clamp(44px,5.7vw,82px);line-height:.98;letter-spacing:-.055em;max-width:800px}
      .hero-actions{margin-top:22px!important}
      .hero-bottom-line{position:absolute!important;left:0!important;bottom:34px!important;z-index:6!important;margin:0!important;max-width:none!important;width:auto!important;font-family:var(--u)!important;font-size:clamp(13px,1.28vw,18px)!important;font-weight:300!important;line-height:1!important;letter-spacing:-.025em!important;color:#12171b!important;white-space:nowrap!important}
      .services{background:#eef3f6!important;background-image:none!important;border-bottom:0!important;padding:94px 0 104px!important}
      .services>.shell{width:min(1380px,calc(100% - 72px))!important}
      .services-master{width:100%!important;margin:0!important;padding:0!important}
      .services-master-title{margin:0 0 48px!important;font-family:var(--u)!important;font-size:clamp(34px,3.8vw,50px)!important;font-weight:300!important;line-height:1!important;letter-spacing:-.05em!important;text-transform:uppercase!important;color:#12171b!important}
      .services-master-subhead{margin:0!important;max-width:1120px!important;font-family:var(--u)!important;font-size:clamp(38px,4.6vw,66px)!important;font-weight:300!important;line-height:1.04!important;letter-spacing:-.055em!important;color:#12171b!important}
      .services-master-copy{margin:24px 0 0!important;max-width:920px!important;font-family:var(--m)!important;font-size:clamp(17px,1.55vw,22px)!important;line-height:1.48!important;color:#59656d!important}
      .services-master-visual{display:block!important;width:100%!important;height:auto!important;min-height:0!important;margin:36px 0 0!important;padding:0!important;background:transparent!important;border:0!important;border-radius:0!important;box-shadow:none!important;overflow:visible!important}
      .services-master-visual img{display:block!important;visibility:visible!important;opacity:1!important;width:min(1200px,100%)!important;max-width:100%!important;height:auto!important;max-height:none!important;margin:0 auto!important;padding:0!important;object-fit:contain!important;object-position:center center!important;background:transparent!important;border:0!important;box-shadow:none!important;transform:none!important}
      .services-master-cta{display:flex!important;align-items:center!important;justify-content:center!important;width:min(760px,100%)!important;min-height:68px!important;margin:34px auto 0!important;padding:0 28px!important;border:0!important;border-radius:14px!important;background:#d85c2b!important;color:#fff!important;font-family:var(--m)!important;font-size:18px!important;font-weight:500!important;letter-spacing:-.01em!important;text-decoration:none!important;box-shadow:none!important;transition:transform .18s ease,background .18s ease!important}
      .services-master-cta:hover{transform:translateY(-2px)!important;background:#c94b1d!important}
      @media(max-width:900px){.services{padding:68px 0 78px!important}.services>.shell{width:auto!important;margin-left:30px!important;margin-right:30px!important}.services-master-title{margin-bottom:36px!important}.services-master-visual{margin-top:38px!important;padding:0!important;background:transparent!important}}
      @media(max-width:640px){
        .hero h1 .hero-accent{font-size:9px!important;line-height:1.3!important;max-width:72vw!important;margin:0 0 10px 1px!important}
        html[lang="ru"] .hero-copy{width:76vw!important;padding-top:104px!important}
        html[lang="ru"] .hero-title{font-size:clamp(29px,8.7vw,37px)!important;line-height:1.01!important;max-width:76vw!important}
        .hero-actions{margin-top:16px!important;width:58vw!important}
        .hero-bottom-line{left:0!important;bottom:17px!important;width:calc(100vw - 28px)!important;font-size:clamp(10.5px,3.25vw,13px)!important;line-height:1!important;letter-spacing:-.035em!important;white-space:nowrap!important}
        .services{padding:56px 0 66px!important}
        .services>.shell{margin-left:24px!important;margin-right:24px!important}
        .services-master-title{margin-bottom:30px!important;font-size:clamp(30px,8.5vw,38px)!important;line-height:1!important;letter-spacing:-.055em!important}
        .services-master-subhead{max-width:100%!important;font-size:clamp(29px,8.8vw,39px)!important;line-height:1.06!important;letter-spacing:-.05em!important}
        .services-master-copy{margin-top:20px!important;font-size:16px!important;line-height:1.48!important;max-width:100%!important}
        .services-master-visual{width:calc(100% + 16px)!important;margin:28px -8px 0!important;padding:0!important;min-height:0!important;background:transparent!important}
        .services-master-visual img{display:block!important;visibility:visible!important;opacity:1!important;width:100%!important;max-width:none!important;height:auto!important;max-height:none!important;margin:0!important;object-fit:contain!important}
        .services-master-cta{width:100%!important;min-height:60px!important;margin-top:26px!important;border-radius:12px!important;font-size:17px!important}
      }
    `;
    document.head.appendChild(style);
  };

  document.head.appendChild(original);
})();