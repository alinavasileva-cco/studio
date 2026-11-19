"""Structural tests for deduplicated smooth contours."""
import pathlib
import sys
import unittest
import numpy as np

sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]))
from smooth_geometry import build_boundary_chains, smoothed_boundaries


class SmoothBoundaryTests(unittest.TestCase):
    def test_all_transition_edges_drawn_once(self):
        arr=np.zeros((26,35),dtype=np.int32)
        arr[3:15,4:27]=2
        arr[8:12,10:19]=3
        arr[2:12,30:]=4
        chains, edge_count=build_boundary_chains(arr)
        self.assertEqual(sum(len(chain)-1 for chain in chains),edge_count)
        self.assertGreater(edge_count,10)
        paths, metrics=smoothed_boundaries(arr)
        self.assertEqual(len(paths),len(chains))
        self.assertEqual(metrics['grid_boundary_edges'],edge_count)
        self.assertTrue(any('C' in path for path in paths))

    def test_exact_branch_endpoint_kept_after_smoothing(self):
        # Three colors meet in a topological T-junction.
        arr=np.ones((12,12),dtype=np.int32)
        arr[:5,:6]=2
        arr[:5,6:]=3
        paths,metrics=smoothed_boundaries(arr)
        self.assertGreaterEqual(metrics['shared_chains'],3)
        self.assertEqual(metrics['drawn_chains'],len(paths))
        self.assertTrue(all(path.startswith('M') for path in paths))

if __name__=='__main__':unittest.main()
