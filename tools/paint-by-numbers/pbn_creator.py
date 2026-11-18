#!/usr/bin/env python3
"""Reproducible paint-by-numbers pipeline with strict 100% region coverage.

Independent Python implementation; not derived from the Qt/C++ ColoringByNumberCreator code.
"""
from __future__ import annotations
import argparse
import csv
import json
import math
import shutil
from collections import Counter
from pathlib import Path
from xml.sax.saxutils import escape
import xml.etree.ElementTree as ET

import cv2
import fitz
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from rasterio.features import shapes
from scipy import ndimage as ndi
from scipy.spatial import cKDTree
import cairosvg
from smooth_geometry import smoothed_boundaries
from retouch_lanterns import retouch_lantern_portrait

PALETTE_DEFAULT = [
    (249,247,215),(253,232,129),(252,192,72),(251,153,47),(248,123,37),
    (240,88,29),(224,142,112),(181,126,124),(139,67,55),(163,29,18),
    (105,15,13),(55,14,16),(98,55,61),(77,56,78),(52,36,51),
    (48,68,121),(51,56,93),(28,35,66),(21,9,16),
]
DESC_DEFAULT = [
    'Слоновая кость','Светлый янтарь','Золотистый','Тёплый янтарь',
    'Ярко-оранжевый','Красно-оранжевый','Тёплый персиковый',
    'Пудровая роза','Терракота','Красный','Тёмно-красный',
    'Вишнёвый','Коричнево-сливовый','Сливовый','Сумеречный фиолетовый',
    'Глубокий синий','Серо-синий','Ночной синий','Почти чёрный',
]
STRUCTURE = ndi.generate_binary_structure(2, 1)
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'

def load_palette(path):
    if not path:
        return np.asarray(PALETTE_DEFAULT,np.uint8), DESC_DEFAULT
    colors, descriptions = [], []
    with open(path,'r',encoding='utf-8-sig',newline='') as f:
        for r in csv.DictReader(f,delimiter=';'):
            h=r['color_hex'].lstrip('#')
            colors.append(tuple(bytes.fromhex(h)))
            descriptions.append(r.get('description',''))
    assert len(colors)==19, 'Palette must contain exactly 19 colors'
    return np.asarray(colors,np.uint8), descriptions

def read_indexed(path, palette, width, smooth_kernel=3):
    src = Image.open(path)
    if width and src.width!=width:
        newsize=(width,round(src.height*width/src.width))
        src=src.resize(newsize,Image.Resampling.NEAREST if src.mode=='P' else Image.Resampling.LANCZOS)
    if src.mode=='P' and len(set(np.asarray(src).ravel().tolist()))<=19:
        idx = np.asarray(src,dtype=np.uint8).copy()
        if np.max(idx)>18: raise ValueError('Indexed color must be in range 0..18')
        # Preserve explicit palette while smoothing tiny stairsteps.
        return cv2.medianBlur(idx,smooth_kernel) if smooth_kernel>=3 else idx
    rgb=np.asarray(src.convert('RGB'),np.uint8)
    # Edge-aware smoothing to avoid fragmented segmentation of photographic noise.
    smooth=cv2.bilateralFilter(rgb,7,40,20)
    smooth=cv2.bilateralFilter(smooth,7,40,20)
    pal_lab=cv2.cvtColor(palette[None,:,:],cv2.COLOR_RGB2LAB)[0].astype(np.float32)
    img_lab=cv2.cvtColor(smooth,cv2.COLOR_RGB2LAB).reshape(-1,3).astype(np.float32)
    tree=cKDTree(pal_lab)
    idx=tree.query(img_lab,workers=-1)[1].reshape(rgb.shape[:2]).astype(np.uint8)
    return cv2.medianBlur(idx,5)

def components(index):
    ids=np.zeros(index.shape,np.int32)
    info=[None]
    for k in range(19):
        lbl, n=ndi.label(index==k,STRUCTURE)
        if n==0: continue
        sizes=np.bincount(lbl.ravel(),minlength=n+1)
        mapping=np.zeros(n+1,np.int32)
        for j in range(1,n+1):
            mapping[j]=len(info)
            info.append({'id':len(info),'paint_id':k+1,'pixels':int(sizes[j])})
        mask=lbl>0
        ids[mask]=mapping[lbl[mask]]
    if np.any(ids==0): raise AssertionError('Unassigned pixels detected')
    return ids,info

def choose_neighbor(idx, mask, palette_lab, paint_idx, bounds):
    y0,y1,x0,x1=bounds
    patch=idx[y0:y1,x0:x1]
    m=mask
    dil=cv2.dilate(m.astype(np.uint8),np.ones((3,3),np.uint8)).astype(bool)
    border=patch[dil&~m]
    if border.size==0: return paint_idx
    vals,counts=np.unique(border,return_counts=True)
    candidates=[(int(v),int(c)) for v,c in zip(vals,counts) if int(v)!=paint_idx]
    if not candidates:return paint_idx
    def score(v,c):
        d=float(np.linalg.norm(palette_lab[v]-palette_lab[paint_idx]))
        return 4.0*math.log1p(c) - 0.06*d
    return max(candidates,key=lambda t:score(*t))[0]

def cleanup(index, palette, min_area=22, min_radius=3.2, passes=5, protected=None):
    idx=index.copy()
    pal_lab=cv2.cvtColor(palette[None,:,:],cv2.COLOR_RGB2LAB)[0].astype(np.float32)
    h,w=idx.shape
    rect=None
    if protected:
        x,y,ww,hh=protected
        rect=(int(x*w),int(y*h),int((x+ww)*w),int((y+hh)*h))
    stats=[]
    for _pass in range(passes):
        regions, info=components(idx)
        sizes=np.bincount(regions.ravel())
        objs=ndi.find_objects(regions)
        planned=[]
        for region in info[1:]:
            rid=region['id']; a=int(sizes[rid]); sl=objs[rid-1]
            if sl is None:continue
            ysl,xsl=sl; y0,y1=ysl.start,ysl.stop; x0,x1=xsl.start,xsl.stop
            # Protect the eyes and face from aggressive merging.
            in_face=rect is not None and (x0+x1)/2>=rect[0] and (x0+x1)/2<rect[2] and (y0+y1)/2>=rect[1] and (y0+y1)/2<rect[3]
            aa=max(7,min_area//3) if in_face else min_area
            rr=max(1.5,min_radius*0.52) if in_face else min_radius
            # Width of bounding box is not enough: a serpentine 1px-wide
            # sliver may span a large box, yet has no room for a paint number.
            local=(regions[ysl,xsl]==rid)
            rmax=float(ndi.distance_transform_edt(np.pad(local,1)).max())
            if a>=aa and rmax>=rr:continue
            yy0=max(0,y0-2);yy1=min(h,y1+2);xx0=max(0,x0-2);xx1=min(w,x1+2)
            pm=(regions[yy0:yy1,xx0:xx1]==rid)
            target=choose_neighbor(idx,pm,pal_lab,region['paint_id']-1,(yy0,yy1,xx0,xx1))
            if target!=region['paint_id']-1:
                planned.append((rid,yy0,yy1,xx0,xx1,pm,target))
        for rid,y0,y1,x0,x1,pm,tgt in planned:
            patch=idx[y0:y1,x0:x1]
            patch[pm]=tgt
        stats.append({'pass':_pass+1,'merged':len(planned)})
        if not planned:break
    return idx,stats

def layouts(index, palette):
    reg,info=components(index)
    if len(info)-1!=int(np.max(reg)): raise AssertionError('Region count mismatch')
    if np.min(reg)<1: raise AssertionError('Gaps exist')
    objs=ndi.find_objects(reg)
    all_positions=[]
    for region in info[1:]:
        rid=region['id']; s=objs[rid-1]
        local=(reg[s]==rid)
        dt=ndi.distance_transform_edt(np.pad(local,1))[1:-1,1:-1]
        yi,xi=np.unravel_index(np.argmax(dt),dt.shape)
        cx,cy=float(s[1].start+xi+0.5),float(s[0].start+yi+0.5)
        region.update({'x':round(cx,2),'y':round(cy,2),
                       'radius':round(float(dt[yi,xi]),3),
                       'bbox':[s[1].start,s[0].start,s[1].stop,s[0].stop],
                       'paint_hex':'#'+''.join(f'{v:02X}' for v in palette[region['paint_id']-1])})
        all_positions.append((cx,cy))
    assert len(all_positions)==len(info)-1
    return reg,info

def svg_paths(reg):
    # Region polygons come from the shared integer grid. Adjacent boundaries
    # coincide exactly; no independently fitted doubled curves or hairline gaps.
    result=[]
    for geo,rid in shapes(reg.astype('int32'),connectivity=4):
        parts=[]
        for ring in geo['coordinates']:
            coords=[(float(x),float(y)) for x,y in ring]
            # Drop points on straight pixel-grid edges without moving vertices.
            cut=[]
            for p in coords:
                while len(cut)>=2:
                    a,b=cut[-2],cut[-1]
                    if (a[0]==b[0]==p[0]) or (a[1]==b[1]==p[1]):cut.pop()
                    else:break
                cut.append(p)
            if cut:
                parts.append('M'+' '.join(f'{x:g},{y:g}' if i==0 else f'L{x:g},{y:g}' for i,(x,y) in enumerate(cut))+'Z')
        if parts:result.append((int(rid),' '.join(parts)))
    return result

def make_svg(reg,info,palette,out,width_cm=40,height_cm=60,label_size=7.8,
             outline_mode='smooth',curve_sigma=1.4,curve_epsilon=0.8):
    h,w=reg.shape
    paths=svg_paths(reg)
    stroke=0.58
    header=(f'<svg xmlns="http://www.w3.org/2000/svg" width="{width_cm}cm" height="{height_cm}cm" '
            f'viewBox="0 0 {w} {h}"><rect width="{w}" height="{h}" fill="white"/>')
    lines=[header]
    # Keep an exact 1:1 region/palette definition and editable segment IDs.
    # The vector geometry of these fills follows the source pixels; the visible
    # contour is a separately traced SHARED graph, smoothed as a single curve.
    # No neighboring region is ever fitted independently.
    for rid,geom in paths:
        lines.append(f'<path id="segment-{rid}" d="{geom}" fill="white" stroke="none" fill-rule="evenodd"/>')
    if outline_mode == 'smooth':
        smooth,geometry_qa=smoothed_boundaries(reg,sigma=curve_sigma,epsilon=curve_epsilon)
        lines.append('<g id="shared-smooth-contours" fill="none" stroke="#303030" stroke-width="0.55" stroke-linecap="round" stroke-linejoin="round">')
        for chain_number, path_d in enumerate(smooth, 1):
            lines.append(f'<path id="shared-chain-{chain_number}" d="{path_d}"/>')
        lines.append('</g>')
    else:
        # Deterministic legacy raster-grid boundaries, intended for regression
        # tests and direct visual comparison only.
        y_changes=(reg[:-1,:]!=reg[1:,:])
        x_changes=(reg[:,:-1]!=reg[:,1:])
        lines.append('<g id="pixel-contours" fill="none" stroke="#303030" stroke-width="0.55">')
        for y in range(h-1):
            ar=np.flatnonzero(y_changes[y])
            if ar.size:
                split=np.flatnonzero(np.diff(ar)>1)+1
                for run in np.split(ar,split):
                    lines.append(f'<path d="M{run[0]},{y+1}H{run[-1]+1}"/>')
        for x in range(w-1):
            ar=np.flatnonzero(x_changes[:,x])
            if ar.size:
                split=np.flatnonzero(np.diff(ar)>1)+1
                for run in np.split(ar,split):
                    lines.append(f'<path d="M{x+1},{run[0]}V{run[-1]+1}"/>')
        lines.append('</g>')
    lines.append(f'<rect x="0.26" y="0.26" width="{w-0.52}" height="{h-0.52}" fill="none" stroke="#333" stroke-width="0.52"/>')
    lines.append('<g id="paint-numbers" font-family="DejaVu Sans,Arial,sans-serif" font-weight="500" fill="#161616" text-anchor="middle" dominant-baseline="middle">')
    for region in info[1:]:
        # All numbers use one typeface and weight. Size may decrease only where
        # the standard 7.8px label physically cannot fit the enclosed region.
        # A label is bounded by the maximal inscribed disk, avoiding collisions.
        label=str(region['paint_id'])
        length_scale=1.12 if len(label)==1 else 1.61
        size=min(float(label_size),max(0.9,float(region['radius'])*1.58/length_scale))
        region['label_size']=round(size,2)
        lines.append(f'<text id="number-{region["id"]}" font-size="{size:.2f}" x="{region["x"]}" y="{region["y"]}">{label}</text>')
    lines.append('</g></svg>')
    Path(out).write_text('\n'.join(lines),encoding='utf-8')
    return len(paths)

def png_preview(reg,info,palette,out,svg_file=None,scale=3):
    """Render the same vector contours used in the PDF (not pixel-grid edges)."""
    h,w=reg.shape
    index=np.zeros_like(reg,dtype=np.uint8)
    for q in info[1:]:index[reg==q['id']]=q['paint_id']-1
    rgb=palette[index]
    Image.fromarray(rgb,'RGB').save(Path(out)/'color_19.png')
    if svg_file is not None:
        cairosvg.svg2png(url=str(svg_file),write_to=str(Path(out)/'numbered_19.png'),
                        output_width=w*scale,output_height=h*scale)
    else:
        raise ValueError('The numbered preview must be rendered from the SVG itself')
    # Non-authoritative antialiased preview for eyes; color_19.png is the exact
    # 19-color image and region_ids.npz is the exact indexed segmentation.
    Image.fromarray(rgb,'RGB').resize((w*scale,h*scale),Image.Resampling.LANCZOS).save(
        Path(out)/'color_preview_antialiased.png')

def create_pdf(svgfile, out, info,palette,description, regionmap, qa, rows=3,cols=2):
    out=Path(out)
    with fitz.open() as result:
        base=cairosvg.svg2pdf(url=str(svgfile))
        source=fitz.open(stream=base,filetype='pdf')
        result.insert_pdf(source)
        # Color check: exact geometry, no numbers obscuring the artwork.
        data=Image.open(out/'color_19.png')
        pix=fitz.Pixmap(str(out/'color_preview_antialiased.png'))
        p=result.new_page(width=595,height=842)
        r=fitz.Rect(48,42,547,42+748)
        p.insert_image(r,pixmap=pix,keep_proportion=True)
        p.insert_text((40,30),'COLOR PROOF - 19 colors',fontsize=14)
        # Legend deliberately uses only ASCII labels in PDF; UTF8 names also in CSV.
        p=result.new_page(width=595,height=842)
        p.insert_text((42,44),'PAINT PALETTE  01 - 19',fontsize=18)
        for i,(rgb,name) in enumerate(zip(palette,description)):
            y=84+i*36
            if y>807:break
            r=fitz.Rect(50,y-17,125,y+8)
            p.draw_rect(r,color=(0,0,0),fill=tuple(int(z)/255 for z in rgb),width=.5)
            p.insert_text((144,y),f'{i+1:02d}',fontsize=12)
        # The 40x60 reference is split into 6 A4 tiles; no region is cropped
        # from the underlying source. Boundaries and numbers repeat exactly.
        full=source[0]
        ww,hh=full.rect.width,full.rect.height
        for ry in range(rows):
            for cx in range(cols):
                tile=fitz.Rect(cx*ww/cols,ry*hh/rows,(cx+1)*ww/cols,(ry+1)*hh/rows)
                pg=result.new_page(width=595,height=842)
                area=fitz.Rect(28,58,567,798)
                pg.show_pdf_page(area,source,0,clip=tile,keep_proportion=True)
                pg.insert_text((32,38),f'SHEET {ry+1}-{cx+1} / {rows}x{cols}',fontsize=14)
        # High magnification cutouts (eyes, kimono, lantern, water) preserve exact IDs.
        h,w=regionmap.shape
        zones=[('FACE',(.32,.26,.43,.22)),
               ('LEFT_EYE',(.54,.315,.085,.058)),
               ('RIGHT_EYE',(.63,.315,.055,.065)),
               ('LIPS',(.59,.372,.105,.058)),
               ('KIMONO',(.20,.42,.55,.42)),
               ('LANTERN',(.65,.34,.32,.43)),
               ('WATER',(.0,.74,1.0,.26))]
        for name,(x,y,zw,zh) in zones:
            crop=fitz.Rect(x*ww,y*hh,(x+zw)*ww,(y+zh)*hh)
            pg=result.new_page(width=595,height=842)
            pg.insert_text((40,43),f'ENLARGED DETAIL - {name}',fontsize=16)
            pg.show_pdf_page(fitz.Rect(35,70,560,785),source,0,clip=crop,keep_proportion=True)
        p=result.new_page(width=595,height=842)
        p.insert_text((42,45),'QA REPORT / REPRODUCIBILITY',fontsize=15)
        for i, line in enumerate([
            f'Colors: {qa["paint_colors"]}',
            f'Connected regions: {qa["region_count"]}',
            f'Number labels: {qa["number_labels"]}',
            f'Unassigned pixels: {qa["unassigned_pixels"]}',
            f'Multiple labels per region: {qa["multiple_labels_per_region"]}',
            f'Smaller-than-font-radius: {qa["small_label_regions"]}',
            'Note: enlarged detail pages contain the same numbers.',
            'Print readiness requires proof-print inspection.'
        ]): p.insert_text((50,85+i*25),line,fontsize=11)
        result.save(out/'paint_by_numbers_19_full.pdf',garbage=4,deflate=True)
    source.close()

def create(args):
    out=Path(args.out);out.mkdir(parents=True,exist_ok=True)
    palette, descriptions=load_palette(args.palette)
    index=read_indexed(args.input,palette,args.width,args.smooth_kernel)
    protected=tuple(float(x) for x in args.protected.split(',')) if args.protected else None
    edited,history=cleanup(index,palette,args.min_area,args.min_radius,args.passes,protected)
    if args.face_retouch:
        edited=retouch_lantern_portrait(edited)
        edited,face_history=cleanup(edited,palette,min_area=20,min_radius=3.8,passes=4,
                                    protected=(.50,.29,.21,.12))
        history.extend({'pass':'retouch-'+str(item['pass']),'merged':item['merged']} for item in face_history)
    reg,info=layouts(edited,palette)
    h,w=reg.shape
    unique_reg=np.unique(reg)
    assigned={x['id'] for x in info[1:]}
    missing=set(int(r) for r in unique_reg)-assigned
    assert not missing and np.min(reg)>0 and np.max(reg)==len(info)-1
    small=[r for r in info[1:] if r['radius']<5]
    qa={
        'input':str(args.input),'size':[w,h],'paint_colors':int(len(np.unique(edited))),
        'region_count':len(info)-1,'number_labels':len(info)-1,
        'unassigned_pixels':int(np.sum(reg==0)),'multiple_labels_per_region':0,
        'small_label_regions':len(small),'small_label_region_ids':[r['id'] for r in small],
        'cleanup_history':history,'outline_mode':args.outline_mode,
        'curve_sigma':args.curve_sigma,'curve_epsilon':args.curve_epsilon,
        'status':'PIXEL_PARTITION_VALIDATED_VECTOR_VISUAL_QA_REQUIRED',
        'not_verified':'Manual visual QA and physical printing',
    }
    np.savez_compressed(out/'region_ids.npz',segment_id=reg,paint_id=edited+1,palette=palette)
    indexed=Image.fromarray(edited,'P')
    indexed.putpalette([int(v) for row in palette.tolist() for v in row]+[0]*(768-57))
    indexed.save(out/'palette_indices.png')
    n=make_svg(reg,info,palette,out/'numbered_19.svg',args.canvas_width,args.canvas_height,args.label_size,
               args.outline_mode,args.curve_sigma,args.curve_epsilon)
    with (out/'regions.csv').open('w',newline='',encoding='utf-8') as f:
        writer=csv.DictWriter(f,fieldnames=['id','paint_id','pixels','x','y','radius','bbox','paint_hex','label_size'],delimiter=';',extrasaction='ignore')
        writer.writeheader()
        for q in info[1:]:writer.writerow(q)
    with (out/'palette_19.csv').open('w',newline='',encoding='utf-8-sig') as f:
        wr=csv.writer(f,delimiter=';');wr.writerow(['paint_id','color_hex','description'])
        for i,(col,desc) in enumerate(zip(palette,descriptions),1):
            wr.writerow([i,'#'+''.join(f'{int(c):02X}' for c in col),desc])
    qa['reduced_label_size_count']=sum(1 for r in info[1:] if r['label_size'] < args.label_size-0.05)
    qa['minimum_label_size_px']=min(r['label_size'] for r in info[1:])
    qa['face_retouch']=bool(args.face_retouch)
    # Verify the actual serialized SVG, not just in-memory counts.
    tree=ET.parse(out/'numbered_19.svg')
    nodes=list(tree.iter())
    svg_segments=[v for v in nodes if v.attrib.get('id','').startswith('segment-')]
    svg_numbers=[v for v in nodes if v.attrib.get('id','').startswith('number-')]
    qa['svg_segment_paths']=len(svg_segments)
    qa['svg_number_labels']=len(svg_numbers)
    assert len(svg_segments)==len(info)-1==len(svg_numbers)
    assert len({v.attrib['id'] for v in svg_numbers})==len(svg_numbers)
    assert len({v.attrib['id'] for v in svg_segments})==len(svg_segments)
    png_preview(reg,info,palette,out,out/'numbered_19.svg')
    (out/'numbered_19_40x60.pdf').write_bytes(cairosvg.svg2pdf(url=str(out/'numbered_19.svg')))
    create_pdf(out/'numbered_19.svg',out,info,palette,descriptions,reg,qa,args.rows,args.cols)
    (out/'qa_report.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2),encoding='utf-8')
    assert n==len(info)-1, f'Path count differs: {n} != {len(info)-1}'
    return qa

def cli():
    p=argparse.ArgumentParser(description='Create 19-color numbered PBN with strict full-image coverage')
    p.add_argument('--input',required=True,help='RGB or indexed (P mode) PNG/JPEG')
    p.add_argument('--out',default='output')
    p.add_argument('--palette',help='CSV paint_id;color_hex;description (must have 19 colors)')
    p.add_argument('--width',type=int,default=0,help='Working-image width, 0=preserve input size')
    p.add_argument('--protected',default='0.45,0.25,0.26,0.24',help='Normalized protected face rectangle x,y,w,h')
    p.add_argument('--smooth-kernel',type=int,choices=[1,3,5,7],default=3)
    p.add_argument('--min-area',type=int,default=40)
    p.add_argument('--min-radius',type=float,default=3.0)
    p.add_argument('--passes',type=int,default=5)
    p.add_argument('--label-size',type=float,default=7.8)
    p.add_argument('--canvas-width',type=float,default=40.0)
    p.add_argument('--canvas-height',type=float,default=60.0)
    p.add_argument('--face-retouch',action='store_true',
                   help='Restore drawn eyes and lips for the specific 639x958 lantern portrait')
    p.add_argument('--outline-mode',choices=['smooth','pixel'],default='smooth',
                   help='Shared smooth Bézier boundaries (default), or original pixel grid')
    p.add_argument('--curve-sigma',type=float,default=1.4,
                   help='Gaussian strength for smoothing boundary chains')
    p.add_argument('--curve-epsilon',type=float,default=0.8,
                   help='Polyline simplification before cubic smoothing (pixels)')
    p.add_argument('--rows',type=int,default=3)
    p.add_argument('--cols',type=int,default=2)
    args=p.parse_args()
    print(json.dumps(create(args),ensure_ascii=False,indent=2))

if __name__=='__main__':cli()