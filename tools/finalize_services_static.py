from pathlib import Path
import re

p = Path('index.html')
s = p.read_text(encoding='utf-8')

# Remove all visible section numbering.
s = re.sub(r'<span class="section-index">[^<]*</span>', '', s)

# Remove animated duplicate layer from markup.
s = re.sub(r'<img class="services-decor services-decor-wind"[^>]*>', '', s)

marker = '/* SERVICES_STATIC_FINAL */'
css = '''
/* SERVICES_STATIC_FINAL */
.section-index{display:none!important}
.section-head{justify-content:flex-start}
.services-showcase{background:linear-gradient(180deg,#eef3f6 0%,#e9eff3 100%)}
.services-stage{min-height:600px}
.services-panel{border-radius:30px;background:rgba(251,253,255,.88);border:1px solid rgba(18,23,27,.12);box-shadow:0 22px 55px rgba(32,44,52,.09),inset 0 1px 0 rgba(255,255,255,.9);backdrop-filter:none;-webkit-backdrop-filter:none}
.services-panel:before{border-width:1px;border-color:rgba(216,92,43,.68)}
.services-panel:after{display:none}
.service-icon{width:56px;height:56px;border-radius:50%;background:rgba(251,253,255,.92);border:1px solid rgba(18,23,27,.12);box-shadow:none}
.service-icon svg{width:29px;height:29px;stroke-width:1.55}
.service-offer h3{font-weight:300;letter-spacing:-.045em}
.service-desc{color:#495760}
.service-pill{background:transparent;border:1px solid rgba(216,92,43,.32);font-weight:500}
.services-decor{animation:none!important;transform:none!important;will-change:auto!important;opacity:1!important;image-rendering:auto}
.services-decor-base{z-index:1;filter:none!important}
.services-decor-hand{z-index:4;filter:none!important}
.services-decor-wind{display:none!important;animation:none!important}
@media(max-width:640px){.services-showcase{background:linear-gradient(180deg,#eef3f6 0%,#e9eff3 100%)}.services-panel{border-radius:26px;background:rgba(251,253,255,.92);box-shadow:0 18px 38px rgba(32,44,52,.08)}.service-offer{padding-left:58px}.service-icon{width:48px;height:48px;border-radius:50%}.services-decor{filter:none!important;animation:none!important;transform:none!important}.services-decor-base{opacity:1!important}.services-decor-hand{opacity:1!important}}
'''

if marker not in s:
    s = s.replace('</style>', css + '</style>', 1)

html_without_style = re.sub(r'<style>.*?</style>', '', s, flags=re.S)
if 'services-decor-wind' in html_without_style:
    raise SystemExit('animated services layer still present')
if re.search(r'<span class="section-index">', s):
    raise SystemExit('section numbering still present')

p.write_text(s, encoding='utf-8')
