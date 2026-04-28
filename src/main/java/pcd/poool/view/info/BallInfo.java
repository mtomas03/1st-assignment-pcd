package pcd.poool.view.info;

import pcd.poool.util.P2d;
import java.util.Objects;

/**
 * Immutable visual snapshot of a single ball.
 *
 */
public final class BallInfo {
    private final P2d pos;
    private final double radius;

    /**
     * @param pos    ball center in board coordinates
     * @param radius ball radius in board units
     */
    public BallInfo(P2d pos, double radius) {
        this.pos = pos;
        this.radius = radius;
    }

    public P2d pos() {
        return pos;
    }

    public double radius() {
        return radius;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BallInfo) obj;
        return Objects.equals(this.pos, that.pos) &&
                Double.doubleToLongBits(this.radius) == Double.doubleToLongBits(that.radius);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, radius);
    }

    @Override
    public String toString() {
        return "BallInfo[" +
                "pos=" + pos + ", " +
                "radius=" + radius + ']';
    }

}
