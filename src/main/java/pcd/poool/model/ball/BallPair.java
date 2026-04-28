package pcd.poool.model.ball;

import java.util.Objects;

/**
 * Immutable pair of indexes into the small-ball list.
 *
 */
public final class BallPair {
    private final int i;
    private final int j;


    /**
     * Validates that the pair references two distinct balls.
     */
    public BallPair(int i, int j) {
        if (i == j) {
            throw new IllegalArgumentException("A pair must reference two distinct balls");
        }
        this.i = i;
        this.j = j;
    }

    public int i() {
        return i;
    }

    public int j() {
        return j;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BallPair) obj;
        return this.i == that.i &&
                this.j == that.j;
    }

    @Override
    public int hashCode() {
        return Objects.hash(i, j);
    }

    @Override
    public String toString() {
        return "BallPair[" +
                "i=" + i + ", " +
                "j=" + j + ']';
    }

}

