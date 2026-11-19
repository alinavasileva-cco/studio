"""Protect the indexed partition under 2x/3x anti-staircase tracing."""
import pathlib, sys, unittest
import numpy as np
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]))
from refine_geometry import supersampled_partition
from pbn_creator import components

class SmoothPartitionTests(unittest.TestCase):
    def test_full_coverage_at_each_factor(self):
        a=np.zeros((16,20),np.uint8)
        a[2:10,3:16]=5
        a[9:15,12:19]=12
        a[4:8,9:12]=18
        for factor in (1,2,3):
            out=supersampled_partition(a,factor)
            self.assertEqual(out.shape,(16*factor,20*factor))
            self.assertTrue(np.all(out<19))
            ids,info=components(out)
            self.assertEqual(int((ids==0).sum()),0)
            self.assertEqual(len(info)-1,len(np.unique(ids)))

    def test_single_color_constant(self):
        a=np.full((30,40),7,np.uint8)
        self.assertTrue(np.all(supersampled_partition(a,3)==7))

    def test_determinism(self):
        a=(np.indices((20,30)).sum(axis=0)%19).astype(np.uint8)
        self.assertTrue(np.array_equal(supersampled_partition(a,2),supersampled_partition(a,2)))

if __name__=='__main__': unittest.main()
