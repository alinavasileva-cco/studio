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
    let servicesPrice = null;
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
            <p class="services-master-price" data-ru="10 000 рублей (до 15 слайдов)" data-en="from €100 (up to 15 slides)">10 000 рублей (до 15 слайдов)</p>
            <figure class="services-master-visual">
              <img src="assets/service-combo-premium-final.png?v=20260910-services-approved" alt="THE CASE — презентации и сайты" loading="eager" decoding="async" />
            </figure>
            <a class="services-master-cta" href="#brief-form" role="button" aria-haspopup="dialog" data-ru="Отправить ТЗ →" data-en="Send brief →">Отправить ТЗ →</a>
          </div>
        `;
        servicesTitle = shell.querySelector('.services-master-title');
        servicesSubhead = shell.querySelector('.services-master-subhead');
        servicesCopy = shell.querySelector('.services-master-copy');
        servicesPrice = shell.querySelector('.services-master-price');
        servicesCta = shell.querySelector('.services-master-cta');
      }
    }

    const contactEmail = 'thecasebusinesspresentations@gmail.com';
    document.querySelectorAll('a[href^="mailto:"]').forEach(link => {
      link.href = `mailto:${contactEmail}`;
      const value = link.querySelector('b');
      if (value) value.textContent = contactEmail;
    });

    const briefModal = document.createElement('div');
    briefModal.className = 'brief-modal';
    briefModal.setAttribute('aria-hidden', 'true');
    briefModal.innerHTML = `
      <div class="brief-modal-backdrop" data-brief-close></div>
      <section class="brief-modal-dialog" role="dialog" aria-modal="true" aria-labelledby="brief-modal-title">
        <button class="brief-modal-close" type="button" aria-label="Закрыть" data-brief-close>×</button>
        <div class="brief-modal-kicker">THE CASE</div>
        <h2 id="brief-modal-title" class="brief-modal-title">Отправить ТЗ</h2>
        <p class="brief-modal-copy">Оставьте контакты и опишите задачу. ТЗ придёт напрямую в THE CASE.</p>
        <form class="brief-form" id="brief-form">
          <label class="brief-field">
            <span class="brief-name-label">Имя</span>
            <input class="brief-name-input" type="text" name="name" autocomplete="name" maxlength="100" required />
          </label>
          <label class="brief-field">
            <span class="brief-email-label">Почта</span>
            <input class="brief-email-input" type="email" name="email" autocomplete="email" inputmode="email" maxlength="160" required />
          </label>
          <label class="brief-field">
            <span class="brief-task-label">Техническое задание</span>
            <textarea class="brief-task-input" name="message" rows="7" maxlength="10000" required></textarea>
          </label>
          <input class="brief-honey" type="text" name="_honey" tabindex="-1" autocomplete="off" aria-hidden="true" />
          <button class="brief-submit" type="submit">Отправить →</button>
          <div class="brief-form-status" role="status" aria-live="polite"></div>
        </form>
      </section>
    `;
    document.body.appendChild(briefModal);

    const briefForm = briefModal.querySelector('.brief-form');
    const briefDialog = briefModal.querySelector('.brief-modal-dialog');
    const briefTitle = briefModal.querySelector('.brief-modal-title');
    const briefCopy = briefModal.querySelector('.brief-modal-copy');
    const briefNameLabel = briefModal.querySelector('.brief-name-label');
    const briefEmailLabel = briefModal.querySelector('.brief-email-label');
    const briefTaskLabel = briefModal.querySelector('.brief-task-label');
    const briefNameInput = briefModal.querySelector('.brief-name-input');
    const briefEmailInput = briefModal.querySelector('.brief-email-input');
    const briefTaskInput = briefModal.querySelector('.brief-task-input');
    const briefSubmit = briefModal.querySelector('.brief-submit');
    const briefStatus = briefModal.querySelector('.brief-form-status');
    const briefClose = briefModal.querySelector('.brief-modal-close');
    let lastBriefTrigger = null;

    const openBriefModal = trigger => {
      lastBriefTrigger = trigger || document.activeElement;
      briefModal.classList.add('is-open');
      briefModal.setAttribute('aria-hidden', 'false');
      document.body.classList.add('brief-modal-open');
      briefStatus.textContent = '';
      window.setTimeout(() => briefNameInput?.focus(), 40);
    };

    const closeBriefModal = () => {
      briefModal.classList.remove('is-open');
      briefModal.setAttribute('aria-hidden', 'true');
      document.body.classList.remove('brief-modal-open');
      if (lastBriefTrigger && typeof lastBriefTrigger.focus === 'function') lastBriefTrigger.focus();
    };

    if (servicesCta) {
      servicesCta.removeAttribute('target');
      servicesCta.removeAttribute('rel');
      servicesCta.addEventListener('click', event => {
        event.preventDefault();
        openBriefModal(servicesCta);
      });
    }

    briefModal.querySelectorAll('[data-brief-close]').forEach(el => {
      el.addEventListener('click', closeBriefModal);
    });

    document.addEventListener('keydown', event => {
      if (event.key === 'Escape' && briefModal.classList.contains('is-open')) closeBriefModal();
    });

    briefForm?.addEventListener('submit', async event => {
      event.preventDefault();
      if (!briefForm.reportValidity()) return;

      const honey = briefForm.querySelector('[name="_honey"]')?.value?.trim();
      if (honey) return;

      const name = briefNameInput.value.trim();
      const email = briefEmailInput.value.trim();
      const message = briefTaskInput.value.trim();
      const previousText = briefSubmit.textContent;

      briefSubmit.disabled = true;
      briefSubmit.textContent = document.documentElement.lang === 'en' ? 'Sending…' : 'Отправляем…';
      briefStatus.className = 'brief-form-status';
      briefStatus.textContent = '';

      try {
        const response = await fetch(`https://formsubmit.co/ajax/${contactEmail}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          body: JSON.stringify({
            name,
            email,
            message,
            _replyto: email,
            _subject: 'Новое ТЗ с сайта THE CASE',
            _template: 'table',
            _captcha: 'false',
            source: window.location.href
          })
        });

        const result = await response.json().catch(() => ({}));
        if (!response.ok || result.success === false) throw new Error(result.message || 'Submit failed');

        briefForm.reset();
        briefStatus.classList.add('is-success');
        briefStatus.textContent = document.documentElement.lang === 'en'
          ? 'Brief sent. We will contact you by email.'
          : 'ТЗ отправлено. Свяжемся с вами по почте.';
      } catch (_) {
        briefStatus.classList.add('is-error');
        briefStatus.textContent = document.documentElement.lang === 'en'
          ? `Could not send. Please email ${contactEmail}.`
          : `Не удалось отправить. Напишите на ${contactEmail}.`;
      } finally {
        briefSubmit.disabled = false;
        briefSubmit.textContent = previousText;
      }
    });

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
      setText(servicesPrice, lang, '10 000 рублей (до 15 слайдов)', 'from €100 (up to 15 slides)');
      setText(servicesCta, lang, 'Отправить ТЗ →', 'Send brief →');
      setText(portfolioLinkLabel, lang, 'БОЛЬШЕ РАБОТ', 'MORE WORK');

      setText(briefTitle, lang, 'Отправить ТЗ', 'Send brief');
      setText(briefCopy, lang, 'Оставьте контакты и опишите задачу. ТЗ придёт напрямую в THE CASE.', 'Leave your contacts and describe the task. Your brief will go directly to THE CASE.');
      setText(briefNameLabel, lang, 'Имя', 'Name');
      setText(briefEmailLabel, lang, 'Почта', 'Email');
      setText(briefTaskLabel, lang, 'Техническое задание', 'Brief');
      setText(briefSubmit, lang, 'Отправить →', 'Send →');
      if (briefClose) briefClose.setAttribute('aria-label', lang === 'en' ? 'Close' : 'Закрыть');
      if (briefNameInput) briefNameInput.placeholder = lang === 'en' ? 'Your name' : 'Ваше имя';
      if (briefEmailInput) briefEmailInput.placeholder = 'name@example.com';
      if (briefTaskInput) briefTaskInput.placeholder = lang === 'en' ? 'Describe the presentation or website you need' : 'Опишите, какая презентация или сайт вам нужны';

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
      .services-master-title{margin:0 0 48px!important;font-family:var(--u)!important;font-size:clamp(26px,2.4vw,34px)!important;font-weight:300!important;line-height:1!important;letter-spacing:-.05em!important;text-transform:uppercase!important;color:#12171b!important}
      .services-master-subhead{margin:0!important;max-width:1120px!important;font-family:var(--u)!important;font-size:clamp(38px,4.6vw,66px)!important;font-weight:300!important;line-height:1.04!important;letter-spacing:-.055em!important;color:#12171b!important}
      .services-master-copy{margin:24px 0 0!important;max-width:920px!important;font-family:var(--m)!important;font-size:clamp(17px,1.55vw,22px)!important;line-height:1.48!important;color:#59656d!important}
      .services-master-price{margin:10px 0 0!important;font-family:var(--m)!important;font-size:clamp(12px,1vw,14px)!important;line-height:1.4!important;font-weight:500!important;color:#d85c2b!important}
      .services-master-visual{display:block!important;width:100%!important;height:auto!important;min-height:0!important;margin:36px 0 0!important;padding:0!important;background:transparent!important;border:0!important;border-radius:0!important;box-shadow:none!important;overflow:visible!important}
      .services-master-visual img{display:block!important;visibility:visible!important;opacity:1!important;width:min(1200px,100%)!important;max-width:100%!important;height:auto!important;max-height:none!important;margin:0 auto!important;padding:0!important;object-fit:contain!important;object-position:center center!important;background:transparent!important;border:0!important;box-shadow:none!important;transform:none!important}
      .services-master-cta{display:flex!important;align-items:center!important;justify-content:center!important;width:min(760px,100%)!important;min-height:68px!important;margin:34px auto 0!important;padding:0 28px!important;border:0!important;border-radius:14px!important;background:#d85c2b!important;color:#fff!important;font-family:var(--m)!important;font-size:18px!important;font-weight:500!important;letter-spacing:-.01em!important;text-decoration:none!important;box-shadow:none!important;transition:transform .18s ease,background .18s ease!important}
      .services-master-cta:hover{transform:translateY(-2px)!important;background:#c94b1d!important}
      body.brief-modal-open{overflow:hidden!important}
      .brief-modal{position:fixed;inset:0;z-index:100000;display:none;align-items:center;justify-content:center;padding:24px;font-family:var(--m)}
      .brief-modal.is-open{display:flex}
      .brief-modal-backdrop{position:absolute;inset:0;background:rgba(18,23,27,.46);backdrop-filter:blur(10px);-webkit-backdrop-filter:blur(10px)}
      .brief-modal-dialog{position:relative;z-index:1;width:min(680px,100%);max-height:min(88dvh,820px);overflow:auto;padding:38px 38px 34px;background:#f8fbfd;border:1px solid rgba(18,23,27,.12);border-radius:24px;box-shadow:0 28px 90px rgba(18,23,27,.20);color:#12171b}
      .brief-modal-close{position:absolute;top:18px;right:18px;width:42px;height:42px;border:1px solid rgba(18,23,27,.16);border-radius:50%;background:transparent;color:#12171b;font-family:var(--m);font-size:28px;font-weight:300;line-height:1;cursor:pointer}
      .brief-modal-kicker{margin:0 0 18px;color:#d85c2b;font-size:12px;font-weight:600;letter-spacing:.12em}
      .brief-modal-title{margin:0;padding-right:56px;font-family:var(--u);font-size:clamp(30px,4vw,46px);font-weight:300;line-height:1.04;letter-spacing:-.05em}
      .brief-modal-copy{margin:16px 0 28px;max-width:540px;color:#59656d;font-size:16px;line-height:1.5}
      .brief-form{display:grid;gap:18px}
      .brief-field{display:grid;gap:8px;color:#12171b;font-size:13px;font-weight:500;letter-spacing:.02em}
      .brief-field input,.brief-field textarea{width:100%;box-sizing:border-box;border:1px solid rgba(18,23,27,.18);border-radius:12px;background:#fff;color:#12171b;font-family:var(--m);font-size:16px;font-weight:400;line-height:1.4;outline:none;transition:border-color .18s ease,box-shadow .18s ease}
      .brief-field input{height:54px;padding:0 16px}
      .brief-field textarea{min-height:180px;padding:15px 16px;resize:vertical}
      .brief-field input:focus,.brief-field textarea:focus{border-color:#d85c2b;box-shadow:0 0 0 3px rgba(216,92,43,.12)}
      .brief-honey{position:absolute!important;left:-9999px!important;width:1px!important;height:1px!important;opacity:0!important;pointer-events:none!important}
      .brief-submit{display:flex;align-items:center;justify-content:center;width:100%;min-height:58px;margin-top:4px;border:0;border-radius:12px;background:#d85c2b;color:#fff;font-family:var(--m);font-size:17px;font-weight:500;cursor:pointer;transition:transform .18s ease,background .18s ease,opacity .18s ease}
      .brief-submit:hover{transform:translateY(-1px);background:#c94b1d}
      .brief-submit:disabled{opacity:.62;cursor:wait;transform:none}
      .brief-form-status{min-height:22px;font-size:14px;line-height:1.45;color:#59656d}
      .brief-form-status.is-success{color:#2f6f4e}
      .brief-form-status.is-error{color:#a43822}
      @media(max-width:900px){.services{padding:68px 0 78px!important}.services>.shell{width:auto!important;margin-left:30px!important;margin-right:30px!important}.services-master-title{margin-bottom:36px!important}.services-master-visual{margin-top:38px!important;padding:0!important;background:transparent!important}}
      @media(max-width:640px){
        .hero h1 .hero-accent{font-size:9px!important;line-height:1.3!important;max-width:72vw!important;margin:0 0 10px 1px!important}
        html[lang="ru"] .hero-copy{width:76vw!important;padding-top:104px!important}
        html[lang="ru"] .hero-title{font-size:clamp(29px,8.7vw,37px)!important;line-height:1.01!important;max-width:76vw!important}
        .hero-actions{margin-top:16px!important;width:58vw!important}
        .hero-bottom-line{left:0!important;bottom:17px!important;width:calc(100vw - 28px)!important;font-size:clamp(10.5px,3.25vw,13px)!important;line-height:1!important;letter-spacing:-.035em!important;white-space:nowrap!important}
        .services{padding:56px 0 66px!important}
        .services>.shell{margin-left:24px!important;margin-right:24px!important}
        .services-master-title{margin-bottom:30px!important;font-size:clamp(24px,7vw,30px)!important;line-height:1!important;letter-spacing:-.055em!important}
        .services-master-subhead{max-width:100%!important;font-size:clamp(29px,8.8vw,39px)!important;line-height:1.06!important;letter-spacing:-.05em!important}
        .services-master-copy{margin-top:20px!important;font-size:16px!important;line-height:1.48!important;max-width:100%!important}
        .services-master-visual{width:calc(100% + 16px)!important;margin:28px -8px 0!important;padding:0!important;min-height:0!important;background:transparent!important}
        .services-master-visual img{display:block!important;visibility:visible!important;opacity:1!important;width:100%!important;max-width:none!important;height:auto!important;max-height:none!important;margin:0!important;object-fit:contain!important}
        .services-master-cta{width:100%!important;min-height:60px!important;margin-top:26px!important;border-radius:12px!important;font-size:17px!important}
        .brief-modal{align-items:flex-end;padding:12px}
        .brief-modal-dialog{width:100%;max-height:92dvh;padding:28px 20px 22px;border-radius:20px}
        .brief-modal-close{top:14px;right:14px;width:40px;height:40px}
        .brief-modal-title{font-size:30px;padding-right:48px}
        .brief-modal-copy{margin:13px 0 22px;font-size:14px}
        .brief-form{gap:15px}
        .brief-field input{height:52px}
        .brief-field textarea{min-height:150px}
      }
    `;
    document.head.appendChild(style);
  };

  document.head.appendChild(original);
})();