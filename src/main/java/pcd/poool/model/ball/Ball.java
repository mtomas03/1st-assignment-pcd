package pcd.poool.model.ball;

import pcd.poool.model.board.Boundary;
import pcd.poool.util.P2d;
import pcd.poool.util.V2d;

/**
 * Class representing a Ball in the game.
 *
 * <p>Each instance stores radius, mass, position, and velocity. During each
 * update of its state, the ball applies friction, integrates motion, and bounces
 * against board boundaries.
 *
 * <p>The {@code lastHitBy} field tracks which ball (player or bot or a small one)
 * last touched this ball, so scoring can be attributed correctly when a small ball
 * falls into a hole.
 */
public class Ball {
    private static final double FRICTION_FACTOR = 0.25;

    private final double radius;
    private final double mass;
    private P2d pos;
    private V2d vel;

    /**
     * Last hitter attribution used for scoring when a small ball enters a hole.
     */
    public enum HitBy {NONE, PLAYER, BOT}

    private volatile HitBy lastHitBy = HitBy.NONE;

    /**
     * Creates a ball with the provided physical state.
     *
     * @param pos initial center position
     * @param radius ball radius
     * @param mass ball mass
     * @param vel initial velocity
     */
    public Ball(P2d pos, double radius, double mass, V2d vel) {
        this.pos = pos;
        this.radius = radius;
        this.mass = mass;
        this.vel = vel;
    }

    /**
     * Advances this ball by one simulation step.
     *
     * <p>Applies friction, integrates position, and reflects against board boundaries.
     *
     * @param dt elapsed time in milliseconds
     * @param bounds board limits used for boundary reflection
     */
    public void updateState(long dt, Boundary bounds) {
        double dtSec = dt * 0.001;
        double speed = vel.abs();
        if (speed > 0.001) {
            double dec = FRICTION_FACTOR * dtSec;
            double factor = Math.max(0, speed - dec) / speed;
            vel = vel.mul(factor);
        } else {
            vel = new V2d(0, 0);
        }
        pos = pos.sum(vel.mul(dtSec));
        applyBoundary(bounds);
    }

    private void applyBoundary(Boundary b) {
        if (pos.x() + radius > b.x1()) {
            pos = new P2d(b.x1() - radius, pos.y());
            vel = vel.swapX();
        } else if (pos.x() - radius < b.x0()) {
            pos = new P2d(b.x0() + radius, pos.y());
            vel = vel.swapX();
        }
        if (pos.y() + radius > b.y1()) {
            pos = new P2d(pos.x(), b.y1() - radius);
            vel = vel.swapY();
        } else if (pos.y() - radius < b.y0()) {
            pos = new P2d(pos.x(), b.y0() + radius);
            vel = vel.swapY();
        }
    }

    /**
     * Sets the ball velocity (used after kicks and impulse application).
     *
     * @param v new velocity
     */
    public void kick(V2d v) {
        this.vel = v;
    }

    /**
     * Returns current center position.
     *
     * @return current position
     */
    public P2d getPos() {
        return pos;
    }

    /**
     * Sets current center position.
     *
     * @param position new center position
     */
    public void setPosition(P2d position) {
        this.pos = position;
    }

    /**
     * Returns current velocity.
     *
     * @return velocity vector
     */
    public V2d getVel() {
        return vel;
    }

    /**
     * Returns ball radius.
     *
     * @return radius in board units
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Returns ball mass.
     *
     * @return mass value used by collision equations
     */
    public double getMass() {
        return mass;
    }

    /**
     * Returns the last hitter attribution.
     *
     * @return hitter flag used by scoring logic
     */
    public HitBy getLastHitBy() {
        return lastHitBy;
    }

    /**
     * Updates the last hitter attribution.
     *
     * @param h new hitter attribution
     */
    public void setLastHitBy(HitBy h) {
        this.lastHitBy = h;
    }
}
