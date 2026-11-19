"""Tests for portrait-specific recoloring and complete label coverage."""
import pathlib
import sys
import unittest
import numpy as np

sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]))
from retouch_lanterns import retouch_lantern_portrait
from pbn_creator import components,layouts,PALETTE_DEFAULT

class RetouchTests(unittest.TestCase):
    def test_optional_artwork_is_palette_indexed(self):
        palette=np.asarray(PALETTE_DEFAULT,dtype=np.uint8)
        before=np.full((958,639),6,dtype=np.uint8)
        after=retouch_lantern_portrait(before)
        self.assertEqual(after.shape,before.shape)
        self.assertTrue(np.array_equal(before[:150],after[:150]))
        self.assertTrue(np.all(after>=0))
        self.assertTrue(np.all(after<=18))
        regions,info=layouts(after,palette)
        self.assertEqual(np.count_nonzero(regions==0),0)
        self.assertEqual(len(info)-1,int(regions.max()))

    def test_wrong_portrait_dimensions_are_rejected(self):
        with self.assertRaises(ValueError):
            retouch_lantern_portrait(np.zeros((25,25),dtype=np.uint8))

if __name__=='__main__':unittest.main()
