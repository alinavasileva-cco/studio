"""Structural invariant tests for all segments, labels, and palette IDs."""
import pathlib
import sys
import unittest
import numpy as np

sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]))
from pbn_creator import components, layouts, svg_paths, PALETTE_DEFAULT

class GeometryTests(unittest.TestCase):
    def test_single_color_is_one_region(self):
        ix=np.zeros((20,30),np.uint8)
        region, info=components(ix)
        self.assertEqual(len(info)-1,1)
        self.assertTrue(np.all(region==1))
        self.assertEqual(info[1]['paint_id'],1)

    def test_distinct_islands_have_distinct_numbers(self):
        ix=np.zeros((20,30),np.uint8)
        ix[2:6,2:6]=4
        ix[12:16,12:16]=4
        region, info=layouts(ix,np.asarray(PALETTE_DEFAULT,np.uint8))
        self.assertEqual(len(info)-1,3)
        self.assertEqual([x['paint_id'] for x in info[1:]].count(5),2)
        self.assertEqual(len(svg_paths(region)),3)
        self.assertEqual(np.count_nonzero(region==0),0)
        for item in info[1:]:
            x=int(item['x']);y=int(item['y'])
            self.assertEqual(region[y,x],item['id'])

    def test_repeated_colors_have_own_region_ids(self):
        ix=np.tile(np.array([0,1,0,2,0],np.uint8),(7,1))
        region,info=components(ix)
        self.assertEqual(len(info)-1,5)
        self.assertTrue({x['paint_id'] for x in info[1:]}.issubset(set(range(1,20))))

if __name__=='__main__': unittest.main()
