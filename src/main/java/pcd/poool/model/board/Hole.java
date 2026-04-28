package pcd.poool.model.board;

import pcd.poool.model.ball.Ball;
import pcd.poool.util.P2d;

import java.util.Objects;

/**
 * Represents a hole on the board. A ball is considered "in the hole"
 * when its center lies within the hole's radius.
 */
public final class Hole {
    private final P2d center;
    private final double radius;


    /**
     * Creates a hole with center and radius.
     *
     * @param center hole center in board coordinates
     * @param radius hole capture radius
     */
    public Hole(P2d center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    /**
     * Returns {@code true} if the ball's center is inside this hole.
     */
    public boolean contains(Ball b) {
        double dx = b.getPos().x() - center.x();
        double dy = b.getPos().y() - center.y();
        return Math.hypot(dx, dy) < radius;
    }

    /**
     * Returns hole center.
     *
     * @return hole center
     */
    public P2d center() {
        return center;
    }

    /**
     * Returns hole radius.
     *
     * @return hole radius
     */
    public double radius() {
        return radius;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Hole) obj;
        return Objects.equals(this.center, that.center) &&
                Double.doubleToLongBits(this.radius) == Double.doubleToLongBits(that.radius);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, radius);
    }

    @Override
    public String toString() {
        return "Hole[" +
                "center=" + center + ", " +
                "radius=" + radius + ']';
    }

}
