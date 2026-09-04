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
})();
