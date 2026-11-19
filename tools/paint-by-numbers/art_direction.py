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
    # V7: redraw the face as an intentional, continuous illustrated profile.
    # Apply the cheek first; subsequent eyelids/iris/mouth are their own paint
    # cells. This also repairs the accidental blue-gray slash at the nose.
    a.shape(6, 'M337,336 C353,324 369,320 387,314 C402,306 415,310 424,321 '
               'C428,330 428,346 430,354 C435,359 439,363 436,368 '
               'C432,374 423,377 420,385 C419,399 414,407 401,409 '
               'C377,407 357,401 339,392 C333,378 335,363 337,336 Z')
    # Soft rose edge under the brow: subtle anatomy, not a flat face patch.
    a.shape(7, 'M346,336 C355,326 368,319 386,320 C371,324 360,330 351,338 Z')
    a.shape(7, 'M405,321 C412,314 422,317 426,323 C418,321 412,324 407,329 Z')
    # Large visible eye: shaped almond, ivory sclera, luminous amber iris,
    # pupil, glints, lid and a delicate lower-lid shadow (independent zones).
    a.shape(0,'M355,330 C362,323 375,320 387,326 C384,334 375,340 364,338 C359,337 356,333 355,330 Z')
    a.ellipse(3,373,330,7.1,8.4,-9)
    a.ellipse(8,373,330,4.7,6.7,-9)
    a.ellipse(18,373.1,329.4,2.7,4.3,-8)
    a.ellipse(0,376.3,326.4,2.25,2.35)
    a.shape(18,'M353,328 C361,317 378,318 390,326 L386,329 C377,322 364,324 356,332 Z')
    # Small lashes are connected to the dark lid, never left floating.
    a.shape(18,'M356,328 L352,321 L362,325 Z')
    a.shape(18,'M362,324 L361,318 L369,323 Z')
    a.shape(12,'M361,340 C371,345 382,340 387,333 C383,342 372,347 363,343 Z')
    a.shape(11,'M354,315 C366,308 381,308 390,315 C379,312 369,314 356,321 Z')
    # Foreshortened far eye: narrower, darker, behind the warm edge lighting.
    a.shape(0,'M410,328 C415,322 422,321 426,327 C427,333 422,338 416,336 Z')
    a.ellipse(3,419.2,329.2,3.6,6.4,-4)
    a.ellipse(18,419.1,329.1,2.1,4.3,-5)
    a.ellipse(0,420.7,326.1,1.5,1.8)
    a.shape(18,'M409,327 C413,320 422,319 428,326 L426,329 C420,323 415,325 411,330 Z')
    a.shape(11,'M411,316 C417,310 424,313 429,320 C423,317 419,316 411,321 Z')
    # A restrained cheek contour, nose bridge, nostril and shading: sculpted
    # as actual colored cells so all detail receives paint numbers.
    a.shape(7,'M390,347 C396,345 400,346 403,349 C399,348 394,350 390,350 Z')
    a.shape(11,'M421,356 C423,355 426,355 427,357 C424,357 423,358 421,357 Z')
    a.shape(7,'M343,360 C346,355 350,355 351,358 C349,362 348,368 347,370 Z')
    # Mouth occupies only ~20 px, softly turned upward and cleanly bounded.
    a.shape(9,'M398,381 C405,378 411,375 418,374 C416,379 410,383 403,384 Z')
    a.shape(6,'M402,384 C409,384 415,381 419,378 C416,388 409,390 402,384 Z')
    a.shape(11,'M398,381 C407,381 416,375 420,373 C416,380 405,385 398,382 Z')
    # Ear sculpting, inner fold and hairline—replaces an empty oval.
    a.shape(6,'M294,338 C293,325 299,314 308,317 C317,317 328,326 328,342 '
               'C329,354 320,369 308,367 C297,362 294,351 294,338 Z')
    a.shape(7,'M299,336 C302,324 314,323 320,337 C316,331 306,334 306,346 '
               'C307,352 312,356 315,353 C307,365 299,349 299,336 Z')
    # Hair highlights: two slender, tapered bands instead of broad gray slashes.
    for d in [
        'M240,315 C265,291 302,269 326,267 C300,282 276,307 244,321 Z',
        'M268,330 C300,306 326,291 349,289 C330,303 302,327 272,335 Z',
        'M322,301 C351,284 379,279 402,281 C375,288 352,305 325,306 Z',
    ]: a.shape(13,d)
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
        (291,564,14,7,6,3,10),(391,601,16,6,7,3,-18),
    ]:a.flower(x,y,r,col=pe,inner=inn,core=core,angle=rotation)
    # The kimono has a dark-to-light collar accent and an intentionally sharp edge.
    a.shape(6,'M259,406 C295,408 319,425 343,443 C318,426 287,414 257,415 Z')
    # LANTERNS: light panels remain bright; motifs have real flower petals and stems.
    # Middle lantern x450..524, y420..529 — clip purely inside its illuminated face.
    a.blossom_sprig(482,479,1.12,stem=3,petal=4,flower_core=2)
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
    return idx,{'art_primitives':len(artwork.primitives),'art_pixels':int(changed.sum()),'artwork_version':'authored-curves-2-v7'}
