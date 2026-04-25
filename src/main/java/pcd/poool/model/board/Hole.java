package pcd.poool.model.board;

import pcd.poool.model.ball.Ball;
import pcd.poool.util.P2d;

/**
 * Represents a hole on the board. A ball is considered "in the hole"
 * when its center lies within the hole's radius.
 */
public record Hole(P2d center, double radius) {

    /**
     * Creates a hole with center and radius.
     *
     * @param center hole center in board coordinates
     * @param radius hole capture radius
     */
    public Hole {
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
    @Override
    public P2d center() {
        return center;
    }

    /**
     * Returns hole radius.
     *
     * @return hole radius
     */
    @Override
    public double radius() {
        return radius;
    }
}
