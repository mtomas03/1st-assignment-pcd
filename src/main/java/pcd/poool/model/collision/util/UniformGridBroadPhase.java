package pcd.poool.model.collision.util;

import pcd.poool.model.ball.Ball;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uniform-grid broad-phase that generates candidate collision pairs.
 *
 * <p>Each ball is assigned to a cell using its center coordinates.
 * Candidate pairs are produced for balls in the same cell and in forward
 * neighboring cells, which avoids duplicate cross-cell pairs.
 */
public class UniformGridBroadPhase {

    // Same cell + 4 forward neighbors avoids duplicate cross-cell pairs.
    private static final int[][] FORWARD_NEIGHBORS = {
            {1, 0},
            {1, 1},
            {0, 1},
            {-1, 1}
    };

    private final double cellSize;

    /**
     * Creates a broad-phase with the given uniform cell size.
     *
     * @param cellSize size of each grid cell in world coordinates (must be greater than zero)
     */
    public UniformGridBroadPhase(double cellSize) {
        if (cellSize <= 0.0) {
            throw new IllegalArgumentException("cellSize must be > 0");
        }
        this.cellSize = cellSize;
    }

    /**
     * Computes candidate pairs for narrow-phase collision checks.
     *
     * <p>The returned pairs are index pairs into the provided {@code balls} list.
     * Pair ordering is normalized so each pair is emitted at most once.
     *
     * @param balls list of balls to partition in the grid
     * @return candidate pairs for narrow-phase collision resolution
     */
    public List<BallPair> computeCandidates(List<Ball> balls) {
        int n = balls.size();
        if (n < 2) {
            return List.of();
        }

        Map<Long, List<Integer>> grid = new HashMap<>(n * 2);

        for (int i = 0; i < n; i++) {
            Ball b = balls.get(i);
            int cx = cellX(b);
            int cy = cellY(b);
            long key = cellKey(cx, cy);
            grid.computeIfAbsent(key, ignored -> new ArrayList<>()).add(i);
        }

        List<BallPair> pairs = new ArrayList<>();

        for (Map.Entry<Long, List<Integer>> entry : grid.entrySet()) {
            long key = entry.getKey();
            List<Integer> cell = entry.getValue();
            int cx = unpackX(key);
            int cy = unpackY(key);

            // Pairs inside the same cell.
            for (int a = 0; a < cell.size(); a++) {
                int ia = cell.get(a);
                for (int b = a + 1; b < cell.size(); b++) {
                    pairs.add(new BallPair(ia, cell.get(b)));
                }
            }

            // Pairs with forward neighboring cells only (no duplicates).
            for (int[] off : FORWARD_NEIGHBORS) {
                List<Integer> other = grid.get(cellKey(cx + off[0], cy + off[1]));
                if (other == null || other.isEmpty()) {
                    continue;
                }
                for (int ia : cell) {
                    for (int ib : other) {
                        if (ia < ib) {
                            pairs.add(new BallPair(ia, ib));
                        } else {
                            pairs.add(new BallPair(ib, ia));
                        }
                    }
                }
            }
        }

        return pairs;
    }

    private int cellX(Ball ball) {
        return (int) Math.floor(ball.getPos().x() / cellSize);
    }

    private int cellY(Ball ball) {
        return (int) Math.floor(ball.getPos().y() / cellSize);
    }

    private static long cellKey(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }

    private static int unpackX(long key) {
        return (int) (key >> 32);
    }

    private static int unpackY(long key) {
        return (int) key;
    }
}

