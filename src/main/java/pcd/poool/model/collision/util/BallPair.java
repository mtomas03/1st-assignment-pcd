package pcd.poool.model.collision.util;

/**
 * Immutable pair of indexes into the small-ball list.
 *
 * @param i index of the first ball
 * @param j index of the second ball
 */
public record BallPair(int i, int j) {

    /**
     * Validates that the pair references two distinct balls.
     */
    public BallPair {
        if (i == j) {
            throw new IllegalArgumentException("A pair must reference two distinct balls");
        }
    }
}

