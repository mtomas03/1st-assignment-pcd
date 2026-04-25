package pcd.poool.model.collision.util;

/**
 * Accumulates position and velocity deltas produced during the Map phase of
 * the parallel collision resolver. Each thread owns its own per-ball
 * accumulator map, so no synchronization is required on this class.
 */
public class CollisionAccumulator {

    private double dx, dy, dvx, dvy;

    /**
     * Adds position and velocity deltas to this accumulator.
     *
     * @param dx position delta on x
     * @param dy position delta on y
     * @param dvx velocity delta on x
     * @param dvy velocity delta on y
     */
    public void add(double dx, double dy, double dvx, double dvy) {
        this.dx += dx;
        this.dy += dy;
        this.dvx += dvx;
        this.dvy += dvy;
    }

    /**
     * Returns accumulated x position delta.
     *
     * @return accumulated dx
     */
    public double getDx() {
        return dx;
    }

    /**
     * Returns accumulated y position delta.
     *
     * @return accumulated dy
     */
    public double getDy() {
        return dy;
    }

    /**
     * Returns accumulated x velocity delta.
     *
     * @return accumulated dvx
     */
    public double getDvx() {
        return dvx;
    }

    /**
     * Returns accumulated y velocity delta.
     *
     * @return accumulated dvy
     */
    public double getDvy() {
        return dvy;
    }

}
