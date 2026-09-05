(() => {
  const original = document.createElement('script');
  original.src = 'thecase-original.js?v=20260905-pattern';
  original.onload = () => {
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
    `;
    document.head.appendChild(style);
  };
  document.head.appendChild(original);
})();
