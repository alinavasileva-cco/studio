"""Reference-specific, editable facial artwork for the lantern-kimono example.

This is intentionally an OPTIONAL user-directed art pass, not a generic face
recognizer. All additions are painted in existing palette IDs, so the regular
connected-components + numbering pipeline treats each as a real paint region.
Coordinates are anchored to the 639 x 958 improved reference illustration.
"""
from __future__ import annotations
import cv2
import numpy as np


def retouch_lantern_portrait(index:np.ndarray)->np.ndarray:
    if index.shape!=(958,639):
        raise ValueError('The reference-specific retouch is intended for a 639x958 poster')
    work=index.copy()
    def poly(coords, color):
        cv2.fillPoly(work,[np.asarray(coords,np.int32)],int(color),lineType=cv2.LINE_8)
    def ell(cx,cy,rx,ry,c,angle=0):
        cv2.ellipse(work,(cx,cy),(rx,ry),angle,0,360,int(c),-1,lineType=cv2.LINE_8)
    def line(pts,c,width=1):
        cv2.polylines(work,[np.asarray(pts,np.int32)],False,int(c),width,lineType=cv2.LINE_8)

    # Main eye (white sclera + copper iris + almost-black pupil + glints).
    # The polygon follows the photograph's lid angle instead of an invented
    # large anime eye. Every colored subdivision remains a numbered area.
    poly([(353,323),(359,321),(365,320),(375,319),(386,320),(392,323),
          (387,331),(383,335),(372,338),(362,335)],0)
    ell(371,328,9,10,3,angle=12)          # warm amber iris
    ell(371,326,4,7,18,angle=7)           # pupil
    ell(374,324,2,3,0)                    # eye catchlight
    # Lid strip and lashes joined to dark hair where possible.
    line([(351,324),(359,319),(371,317),(382,318),(390,321),(394,325)],18,2)
    line([(357,321),(354,317)],18,1)
    line([(362,319),(361,315)],18,1)
    line([(382,318),(387,315)],18,1)
    line([(364,337),(373,340),(384,336)],7,1)

    # Foreshortened second eye: shorter and partly hidden by the profile.
    poly([(410,324),(414,319),(422,319),(426,323),(427,328),
          (424,337),(417,337),(411,333)],0)
    ell(419,329,5,8,3,angle=5)
    ell(418,327,2,5,18)
    ell(420,324,1,2,0)
    line([(408,326),(412,320),(418,318),(423,320),(429,326)],18,2)
    line([(415,336),(422,337)],7,1)

    # Nose, lip shadow and warm lip color - actual independent palette regions.
    line([(426,352),(427,358),(421,360)],7,1)
    poly([(391,377),(404,376),(414,374),(424,371),(427,372),
          (420,379),(410,384),(399,383)],9)
    poly([(400,381),(410,383),(421,379),(414,387),(404,388)],6)
    line([(391,377),(403,379),(416,377),(423,372)],11,1)
    return work
