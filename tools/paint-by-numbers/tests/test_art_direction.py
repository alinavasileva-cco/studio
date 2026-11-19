"""Contract: all authorial contours become indexed real-color paint areas."""
import pathlib,sys,unittest
import numpy as np
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]))
from art_direction import WIDTH,HEIGHT,retouch_art,make_lantern_artwork
from pbn_creator import PALETTE_DEFAULT,layouts

class AuthorArtTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        base=np.full((HEIGHT,WIDTH),11,dtype=np.uint8)
        cls.before=base
        cls.after,cls.metrics=retouch_art(base)
    def test_shapes_are_palette_indexed_and_total(self):
        self.assertGreater(self.metrics['art_primitives'],100)
        self.assertGreater(self.metrics['art_pixels'],5000)
        self.assertEqual(self.after.shape,(HEIGHT,WIDTH))
        self.assertTrue(np.all(self.after<19))
    def test_outside_author_art_is_unchanged(self):
        self.assertTrue(np.array_equal(self.after[:150],self.before[:150]))
    def test_faces_and_fabric_are_different_closed_color_fields(self):
        for (x0,y0,x1,y1) in [(355,320,388,340),(409,323,428,340),
                             (270,485,309,538),(457,449,512,509),
                             (500,855,610,935)]:
            self.assertGreater(len(np.unique(self.after[y0:y1,x0:x1])),1)
        regions,info=layouts(self.after,np.asarray(PALETTE_DEFAULT,np.uint8))
        self.assertEqual(int((regions==0).sum()),0)
        self.assertEqual(len(info)-1,len(np.unique(regions)))
    def test_other_dimensions_rejected(self):
        with self.assertRaises(ValueError):retouch_art(np.ones((20,20),dtype=np.uint8))

if __name__=='__main__':unittest.main()
