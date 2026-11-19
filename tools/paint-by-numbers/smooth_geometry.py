"""Shared, smooth vector boundaries for a fully labeled paint-by-numbers partition.

Each boundary edge is created *once* from an integer label map. Rather than
rounding each region outline independently (which creates visible seams), this
module traces an undirected embedded planar boundary graph and smooths its
maximal chains, keeping all branch/junction vertices unchanged.

The authoritative segmentation remains the unambiguous integer region raster.
Vector curves are a visual approximation, not a new reassignment of pixels.
"""
from __future__ import annotations
from collections import defaultdict
import math

import numpy as np
import cv2
from scipy.ndimage import gaussian_filter1d


def build_boundary_chains(region_map: np.ndarray):
    """Trace transitions into non-duplicated chains, including closed loops.

    Returns (list of Nx2 float64 chains, number_of_original_edges).
    The outer canvas rectangle is drawn separately by the main renderer.
    """
    h, w = region_map.shape
    # Vertex=(x,y), using integer grid; outer endpoints are included.
    edges = []
    adj = defaultdict(list)
    def add(a,b):
        n = len(edges)
        edges.append((a,b))
        adj[a].append(n)
        adj[b].append(n)

    # Horizontal edges separating vertically adjacent pixels.
    ys, xs = np.nonzero(region_map[:-1] != region_map[1:])
    for y,x in zip(ys.tolist(), xs.tolist()):
        a=(x,y+1); b=(x+1,y+1)
        add(a,b)
    ys, xs = np.nonzero(region_map[:,:-1] != region_map[:,1:])
    for y,x in zip(ys.tolist(), xs.tolist()):
        a=(x+1,y); b=(x+1,y+1)
        add(a,b)
    seen=np.zeros(len(edges),dtype=bool)
    chains=[]

    def follow(first, edge):
        points=[first]
        cur=first
        while True:
            if seen[edge]:
                break
            seen[edge]=True
            a,b=edges[edge]
            nxt = b if a == cur else a
            points.append(nxt)
            if nxt == first or len(adj[nxt]) != 2:
                break
            opts=adj[nxt]
            edge=opts[1] if opts[0]==edge else opts[0]
            cur=nxt
        return points

    # At degree!=2 vertices, every edge is a separate chain.
    for vertex, neighbors in adj.items():
        if len(neighbors)!=2:
            for e in neighbors:
                if not seen[e]:chains.append(follow(vertex,e))
    # Remaining edges form closed loops.
    for n,(a,b) in enumerate(edges):
        if not seen[n]: chains.append(follow(a,n))
    if not bool(seen.all()):
        raise AssertionError('Not every boundary edge has been traced')
    return [np.asarray(p,dtype=np.float64) for p in chains],len(edges)


def smooth_chain(points: np.ndarray, sigma:float=1.4, epsilon:float=0.8):
    """Average tiny grid zigzags while retaining branch endpoints exactly."""
    if len(points)<4 or sigma<=0:
        return points.copy()
    closed=np.array_equal(points[0],points[-1])
    if closed:
        body=points[:-1]
        if len(body)<8:
            return points.copy()
        sm=np.column_stack([gaussian_filter1d(body[:,axis],sigma,mode='wrap') for axis in (0,1)])
        arr=cv2.approxPolyDP(sm.astype(np.float32),float(epsilon),True).reshape(-1,2)
        if len(arr)<4:
            return points.copy()
        return np.vstack([arr,arr[0]]).astype(float)
    sm=points.copy()
    # Gaussian smooth only the interior. A slight endpoint taper makes the
    # first few control points approach an unchanged junction gracefully.
    for axis in (0,1):
        data=gaussian_filter1d(points[:,axis],sigma,mode='nearest')
        sm[:,axis]=data
    sm[0]=points[0];sm[-1]=points[-1]
    # Keep immediate neighboring vertices close to endpoints to avoid
    # self-crossings around thin topological junctions.
    if len(sm)>=6:
        sm[1]=0.55*points[1]+0.45*sm[1]
        sm[-2]=0.55*points[-2]+0.45*sm[-2]
    arr=cv2.approxPolyDP(sm.astype(np.float32),float(epsilon),False).reshape(-1,2).astype(float)
    if len(arr)<2: return np.vstack([points[0],points[-1]])
    arr[0]=points[0];arr[-1]=points[-1]
    return arr


def chain_svg(points: np.ndarray, strength:float=0.34):
    """Render smooth Catmull-Rom-style cubic segments with bounded handles.

    For short sharp graph junctions we draw straight lines. For long chains
    smooth curves share immutable branch points, so no doubled contours occur.
    """
    fmt=lambda v:f'{v:.2f}'.rstrip('0').rstrip('.')
    if len(points)<2:return ''
    closed=np.allclose(points[0],points[-1])
    p=points[:-1] if closed else points
    if len(p)<3:
        return 'M'+fmt(p[0,0])+','+fmt(p[0,1])+' '+ ' '.join(f'L{fmt(x)},{fmt(y)}' for x,y in p[1:])
    result=[f'M{fmt(p[0,0])},{fmt(p[0,1])}']
    n=len(p)
    pairs=n if closed else n-1
    for i in range(pairs):
        j=(i+1)%n
        a=p[i];b=p[j]
        prev=p[(i-1)%n] if closed or i>0 else a
        following=p[(j+1)%n] if closed or j<n-1 else b
        length=float(np.linalg.norm(b-a))
        if length<1e-5:continue
        t0=(b-prev);t1=(following-a)
        # A fixed fraction of segment length prevents long cubic handles from
        # looping into neighboring, narrow regions.
        d0=np.linalg.norm(t0);d1=np.linalg.norm(t1)
        maxhandle=length*strength
        c0=a+(t0/(d0+1e-12))*min(maxhandle,d0/6)
        c1=b-(t1/(d1+1e-12))*min(maxhandle,d1/6)
        result.append(f'C{fmt(c0[0])},{fmt(c0[1])} {fmt(c1[0])},{fmt(c1[1])} {fmt(b[0])},{fmt(b[1])}')
    if closed: result.append('Z')
    return ' '.join(result)


def smoothed_boundaries(region_map, sigma=1.4,epsilon=.8):
    chains,original_edges=build_boundary_chains(region_map)
    paths=[]
    for chain in chains:
        rounded=smooth_chain(chain,sigma=sigma,epsilon=epsilon)
        paths.append(chain_svg(rounded))
    return paths,{'grid_boundary_edges':original_edges,'shared_chains':len(chains), 'drawn_chains':len(paths),'sigma':sigma,'epsilon':epsilon}
