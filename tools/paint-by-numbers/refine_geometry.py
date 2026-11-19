"""Upsample the indexed paint map before vector tracing, without adding colors.

Every supersampled pixel gets one of the original palette indices. No RGB
interpolation is used in the AUTHORITATIVE paint map. The continuous preview
may use antialiasing, but the region map remains a disjoint 19-class partition.
"""
from __future__ import annotations
import cv2
import numpy as np


def supersampled_partition(index: np.ndarray, scale: int=2, blur: float=1.15) -> np.ndarray:
    if scale == 1: return index.copy()
    if scale not in (2,3): raise ValueError('Allowed supersampling factors: 1, 2, 3')
    if index.dtype != np.uint8 or index.ndim != 2 or np.max(index) >= 19:
        raise ValueError('Expected uint8 palette indices 0..18')
    h,w=index.shape
    scores = np.empty((19,h*scale,w*scale), np.float32)
    for k in range(19):
        mask=(index==k).astype(np.float32)
        enlarged=cv2.resize(mask,(w*scale,h*scale),interpolation=cv2.INTER_CUBIC)
        scores[k]=cv2.GaussianBlur(enlarged,(0,0),sigmaX=blur,sigmaY=blur)
    # Exactly one winner for each pixel (np.argmax has stable tie breaking).
    out=np.argmax(scores,axis=0).astype(np.uint8)
    assert out.shape==(h*scale,w*scale)
    assert np.all((out>=0)&(out<19))
    return out
