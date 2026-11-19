"""Editable author-art geometry for the lantern kimono composition (639×958).

Authorial linework is constructed as semantic, closed Bezier paint areas — NOT
floating decoration over a color-by-number sheet. Each shape is rasterized into
one of the 19 palette indices and then enters the unchanged global component,
number placement and 100%-coverage validation pipeline.

Coordinates are in the pixel coordinate system of the approved reference image.
Change the geometry here, rather than changing the final SVG by hand.
"""
from __future__ import annotations
import io
import math
from dataclasses import dataclass, field

import cairosvg
import numpy as np
from PIL import Image

WIDTH, HEIGHT = 639, 958


def pt(cx, cy, length, angle):
    angle=math.radians(angle)
    return (cx+length*math.cos(angle), cy+length*math.sin(angle))


def coords(p):
    return f'{p[0]:.2f},{p[1]:.2f}'


@dataclass
class Artwork:
    primitives: list[tuple[int,str]] = field(default_factory=list)
    def shape(self, paint:int, d:str):
        if not 0 <= paint < 19: raise ValueError('Paint index outside 0–18')
        self.primitives.append((paint,f'<path d="{d}"/>'))
    def ellipse(self, paint:int, x:float,y:float, rx:float,ry:float,rotate:float=0):
        self.primitives.append((paint,f'<ellipse cx="{x:.2f}" cy="{y:.2f}" rx="{rx:.2f}" ry="{ry:.2f}" transform="rotate({rotate:.1f} {x:.2f} {y:.2f})"/>'))
    def ribbon(self, paint:int, a, b, c, d, width=2.6):
        """Closed gently tapered botanical stem or ribbon stroke."""
        self.shape(paint, f'M{coords(a)} C{coords(b)} {coords(c)} {coords(d)} '
                 f'C{coords((d[0]+width,d[1]+width))} {coords((b[0]+width,b[1]+width))} {coords((a[0]+width,a[1]+width))} Z')
    def flower(self, x,y, radius, petals=5, col=6, inner=7, core=3, angle=0):
        """Each petal is its own closed paint region; no open ornamental lines."""
        for i in range(petals):
            theta=angle+i*360/petals
            base=pt(x,y,max(3,radius*0.15),theta-18)
            side1=pt(x,y,radius*.65,theta-33)
            tip=pt(x,y,radius,theta)
            side2=pt(x,y,radius*.65,theta+33)
            end=pt(x,y,max(3,radius*0.15),theta+18)
            # Curved, nearly-elliptical petals with individual pointed tips.
            shape=(f'M{coords(base)} C{coords(side1)} {coords(pt(x,y,radius*.93,theta-22))} {coords(tip)} '
                   f'C{coords(pt(x,y,radius*.93,theta+22))} {coords(side2)} {coords(end)} Z')
            self.shape(col if i%3 else inner,shape)
        self.ellipse(core,x,y,max(3.0,radius*.21),max(3.0,radius*.21))
    def blossom_sprig(self,x,y,scale=1.0,stem=3,petal=4,flower_core=2):
        """Lantern floral motif: continuous closed branches + visible petals."""
        self.ribbon(stem,(x,y+23*scale),(x-1*scale,y+6*scale), (x+3*scale,y-5*scale), (x+6*scale,y-19*scale),2.8*scale)
        self.ribbon(stem,(x+1*scale,y+5*scale),(x-3*scale,y-2*scale),(x-9*scale,y-3*scale),(x-12*scale,y-12*scale),2.6*scale)
        self.ribbon(stem,(x+2*scale,y-2*scale),(x+7*scale,y-10*scale),(x+12*scale,y-7*scale),(x+18*scale,y-13*scale),2.6*scale)
        self.flower(x+6*scale,y-17*scale,8*scale,col=petal,inner=stem,core=flower_core,angle=-80)
        self.flower(x-12*scale,y-12*scale,5.8*scale,col=petal,inner=stem,core=flower_core,angle=-20)
        self.ellipse(stem,x-5*scale,y+5*scale,4*scale,2.6*scale,-40)
        self.ellipse(stem,x+10*scale,y-3*scale,4*scale,2.5*scale,35)
    def mask(self)->np.ndarray:
        # Color channel encodes a *palette index*, not an RGB paint color.
        svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" viewBox="0 0 {WIDTH} {HEIGHT}">']
        for paint,tag in self.primitives:
            val=20+paint*11
            svg.append(f'<g fill="rgb({val},80,110)">{tag}</g>')
        svg.append('</svg>')
        raw=cairosvg.svg2png(bytestring=''.join(svg).encode('utf-8'),output_width=WIDTH*2,output_height=HEIGHT*2)
        rgba=np.asarray(Image.open(io.BytesIO(raw)).convert('RGBA'))
        # For every 2x2 supersample, majority vote for the topmost semantic fill.
        rgba=rgba.reshape(HEIGHT,2,WIDTH,2,4).transpose(0,2,1,3,4).reshape(HEIGHT,WIDTH,4,4)
        # Group the four subpixels of the SAME final pixel (H,W,4).
        r=rgba[:,:,:,0]
        al=rgba[:,:,:,3]
        pick=np.argmax(al,axis=2)
        red=np.take_along_axis(r,pick[:,:,None],axis=2)[:,:,0]
        alpha=np.max(al,axis=2)
        out=np.full((HEIGHT,WIDTH),-1,dtype=np.int16)
        use=alpha>=165
        out[use]=np.clip(np.round((red[use].astype(np.float32)-20)/11),0,18).astype(np.int16)
        return out


def make_lantern_artwork():
    a=Artwork()
    # FACE: two restrained, asymmetrical eyes. Preserve the original side
    # profile; do not superimpose an invented, oversized symmetric face.
    a.shape(7,'M357,329 C363,326 380,323 386,330 C378,339 367,339 359,334 Z')
    a.shape(0,'M359,328 C365,322 378,323 385,328 C382,335 370,338 361,333 Z')
    a.ellipse(3,373,329,6.3,7.1,7)
    a.ellipse(18,372.7,328.9,3,4.5,7)
    a.ellipse(0,375.4,326.5,1.9,2.1)
    a.shape(18,'M357,326 C364,318 378,320 389,327 L386,329 C378,324 366,323 359,330 Z')
    a.shape(12,'M364,340 C373,341 382,339 386,334 C379,342 370,344 365,342 Z')
    # Smaller second eye, visibly foreshortened by the profile.
    a.shape(0,'M412,327 C417,323 421,324 424,328 C425,333 420,336 416,333 Z')
    a.ellipse(3,419,329.5,3.5,5.4)
    a.ellipse(18,419,329,1.9,3.2)
    a.ellipse(0,420.5,326.5,1.4,1.6)
    a.shape(18,'M410,326 C416,321 421,323 426,329 L425,330 C419,326 416,326 413,329 Z')
    # Brow and warm skin shadows are closed paint zones.
    a.shape(12,'M351,315 C361,311 377,311 386,314 C373,314 361,316 352,320 Z')
    a.shape(8,'M412,316 C419,313 424,314 427,319 C422,317 418,318 412,320 Z')
    a.shape(7,'M419,353 C424,352 428,358 429,362 Q425,361 420,359 Z')
    a.shape(12,'M423,363 Q429,363 428,366 Q425,367 422,366 Z')
    # Understated lips — follow the actual photograph (not a broad smile).
    a.shape(9,'M402,379 C408,379 413,375 419,375 C416,381 410,383 404,382 Z')
    a.shape(6,'M405,383 C410,383 415,380 418,379 C415,386 408,387 405,383 Z')
    a.shape(12,'M403,378 C409,378 415,376 419,375 Q411,379 404,380 Z')
    a.shape(6,'M413,354 C416,354 418,351 419,350 L418,357 Q415,357 413,356 Z')
    # Soft hair-braid sheen and hair ornament. Shapes are tapered, no hatch strokes.
    for d in [
        'M230,324 C258,299 291,276 330,265 C295,277 263,311 236,333 Z',
        'M246,338 C281,322 302,301 338,292 C312,312 289,331 251,343 Z',
        'M288,291 C311,269 347,251 386,252 C344,255 312,274 294,299 Z',
        'M326,314 C351,292 373,282 405,282 C378,286 351,310 329,320 Z',
    ]:a.shape(13,d)
    # Silk ribbons: elegant broad curves that remain paintable, unlike 1px hatching.
    for color,d in [
        (11,'M170,711 C217,635 220,545 247,455 C230,537 210,644 181,721 Z'),
        (9,'M208,714 C236,643 252,610 278,580 C266,643 234,691 217,719 Z'),
        (11,'M284,722 C320,677 341,626 361,594 C355,654 325,701 293,731 Z'),
        (11,'M350,732 C391,704 415,676 440,628 C438,688 401,730 355,739 Z'),
        (9,'M240,450 C261,449 276,450 297,458 C272,455 249,454 240,459 Z'),
    ]:a.shape(color,d)
    # Kimono sakura clusters — curved individual petals, distinct centers.
    for x,y,r,pe,inn,core,rotation in [
        (285,509,25,6,7,3,15),(342,540,20,9,10,3,-5),
        (247,648,19,7,6,3,15),(306,680,20,7,6,3,20),
        (344,718,16,6,7,3,16),(228,538,13,6,7,3,-3),
    ]:a.flower(x,y,r,col=pe,inner=inn,core=core,angle=rotation)
    # The kimono has a dark-to-light collar accent and an intentionally sharp edge.
    a.shape(6,'M259,406 C295,408 319,425 343,443 C318,426 287,414 257,415 Z')
    # LANTERNS: light panels remain bright; motifs have real flower petals and stems.
    # Middle lantern x450..524, y420..529 — clip purely inside its illuminated face.
    a.blossom_sprig(480,477,1.08,stem=3,petal=4,flower_core=2)
    # Left lantern: motif is large enough to trace/paint, not featureless speckle.
    a.blossom_sprig(59,614,1.15,stem=4,petal=3,flower_core=1)
    # Foreground lantern: two elegant plants with distinct visible blossoms.
    a.blossom_sprig(529,897,1.24,stem=4,petal=3,flower_core=1)
    a.blossom_sprig(585,894,1.00,stem=4,petal=3,flower_core=1)
    return a


def retouch_art(index:np.ndarray):
    if index.shape!=(HEIGHT,WIDTH):
        raise ValueError(f'Author-art overlay only supports {WIDTH}×{HEIGHT}, got {index.shape[::-1]}')
    artwork=make_lantern_artwork()
    idx=index.copy()
    mask=artwork.mask()
    changed=mask>=0
    idx[changed]=mask[changed].astype(np.uint8)
    return idx,{'art_primitives':len(artwork.primitives),'art_pixels':int(changed.sum()),'artwork_version':'authored-curves-1'}
