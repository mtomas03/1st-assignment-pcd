package pcd.poool.view.info;

import pcd.poool.util.P2d;

import java.util.Objects;

/**
 * Immutable visual snapshot of a board hole.
 *
 */
public final class HoleInfo {
    private final P2d center;
    private final double radius;

    /**
     * @param center hole center in board coordinates
     * @param radius hole radius in board units
     */
    public HoleInfo(P2d center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    public P2d center() {
        return center;
    }

    public double radius() {
        return radius;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (HoleInfo) obj;
        return Objects.equals(this.center, that.center) &&
                Double.doubleToLongBits(this.radius) == Double.doubleToLongBits(that.radius);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, radius);
    }

    @Override
    public String toString() {
        return "HoleInfo[" +
                "center=" + center + ", " +
                "radius=" + radius + ']';
    }

}
