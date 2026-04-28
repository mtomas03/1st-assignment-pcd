package pcd.poool.model.ball;

import pcd.poool.model.collision.util.CollisionAccumulator;
import pcd.poool.util.P2d;
import pcd.poool.util.V2d;

import java.util.Objects;

/**
 * Utility class for ball-ball collision handling.
 *
 * <p>Provides two collision pipelines:
 * <ul>
 *   <li><b>Direct resolution</b> via {@link #resolve(Ball, Ball)}:
 *       mutates positions and velocities of both balls immediately.</li>
 *   <li><b>Accumulator-based resolution</b> via
 *       {@link #resolveAccumulator(Ball, Ball, CollisionAccumulator, CollisionAccumulator)}:
 *       computes deltas without mutating balls, for map-reduce workflows.</li>
 * </ul>
 */
public final class BallCollision {
    private static final double RESTITUTION_FACTOR = 0.8;
    private static final double EPSILON = 1e-6;
    private static final double MAX_ACCUMULATED_POSITION_SHIFT = 0.03;
    private static final double MAX_ACCUMULATED_DELTA_SPEED = 0.7;
    private static final double MAX_SPEED_AFTER_ACCUMULATOR = 2.0;

    private BallCollision() {
    }

    /**
     * Resolves collision directly by updating the state of both balls.
     *
     * <p>When an overlap occurs, this method:
     * <ol>
     *   <li>separates balls to remove the overlap,</li>
     *   <li>applies elastic impulse along collision normal.</li>
     * </ol>
     *
     * @param a first ball
     * @param b second ball
     */
    public static boolean resolve(Ball a, Ball b) {
        CollisionData c = compute(a, b);
        if (c == null) return false;

        a.setPosition(new P2d(a.getPos().x() - c.af() * c.nx(), a.getPos().y() - c.af() * c.ny()));
        b.setPosition(new P2d(b.getPos().x() + c.bf() * c.nx(), b.getPos().y() + c.bf() * c.ny()));

        a.kick(new V2d(a.getVel().x() + c.adVx(), a.getVel().y() + c.adVy()));
        b.kick(new V2d(b.getVel().x() + c.bdVx(), b.getVel().y() + c.bdVy()));
        return true;
    }

    /**
     * Computes collision deltas and accumulates them without mutating balls.
     *
     * <p>Useful in parallel map phases where each thread/worker writes only to local
     * accumulators; application of deltas is deferred to a reduce phase.
     *
     * @param a    first ball
     * @param b    second ball
     * @param accA accumulator for {@code a}
     * @param accB accumulator for {@code b}
     */
    public static boolean resolveAccumulator(Ball a, Ball b,
                                          CollisionAccumulator accA,
                                          CollisionAccumulator accB) {
        CollisionData c = compute(a, b);
        if (c == null) return false;

        accA.add(-c.af() * c.nx(), -c.af() * c.ny(), c.adVx(), c.adVy());
        accB.add(c.bf() * c.nx(), c.bf() * c.ny(), c.bdVx(), c.bdVy());
        return true;
    }

    /**
     * Applies accumulated position and velocity deltas to a ball.
     *
     * @param ball target ball to mutate
     * @param acc  accumulated delta; if {@code null}, no changes are applied
     */
    public static void applyAccumulator(Ball ball, CollisionAccumulator acc) {
        if (acc == null) return;

        V2d positionShift = clampVector(new V2d(acc.getDx(), acc.getDy()), MAX_ACCUMULATED_POSITION_SHIFT);
        V2d velocityDelta = clampVector(new V2d(acc.getDvx(), acc.getDvy()), MAX_ACCUMULATED_DELTA_SPEED);

        ball.setPosition(new P2d(ball.getPos().x() + positionShift.x(), ball.getPos().y() + positionShift.y()));

        V2d nextVelocity = new V2d(ball.getVel().x() + velocityDelta.x(), ball.getVel().y() + velocityDelta.y());
        ball.kick(clampVector(nextVelocity, MAX_SPEED_AFTER_ACCUMULATOR));
    }

    private static V2d clampVector(V2d vector, double maxMagnitude) {
        double magnitude = vector.abs();
        if (magnitude <= maxMagnitude || magnitude < EPSILON) {
            return vector;
        }
        return vector.mul(maxMagnitude / magnitude);
    }

    private static CollisionData compute(Ball a, Ball b) {
        double dx = b.getPos().x() - a.getPos().x();
        double dy = b.getPos().y() - a.getPos().y();
        double dist = Math.hypot(dx, dy);
        double minD = a.getRadius() + b.getRadius();
        if (dist >= minD || dist < EPSILON) return null;

        double nx = dx / dist;
        double ny = dy / dist;
        double overlap = minD - dist;
        double totalM = a.getMass() + b.getMass();

        double af = overlap * b.getMass() / totalM;
        double bf = overlap * a.getMass() / totalM;

        double dvx = b.getVel().x() - a.getVel().x();
        double dvy = b.getVel().y() - a.getVel().y();
        double dvn = dvx * nx + dvy * ny;

        double adVx = 0, adVy = 0, bdVx = 0, bdVy = 0;
        if (dvn <= 0) {
            double imp = -(1 + RESTITUTION_FACTOR) * dvn / (1.0 / a.getMass() + 1.0 / b.getMass());
            adVx = -imp / a.getMass() * nx;
            adVy = -imp / a.getMass() * ny;
            bdVx = imp / b.getMass() * nx;
            bdVy = imp / b.getMass() * ny;
        }

        return new CollisionData(nx, ny, af, bf, adVx, adVy, bdVx, bdVy);
    }

    private static final class CollisionData {
        private final double nx;
        private final double ny;
        private final double af;
        private final double bf;
        private final double adVx;
        private final double adVy;
        private final double bdVx;
        private final double bdVy;

        private CollisionData(
                double nx, double ny,
                double af, double bf,
                double adVx, double adVy,
                double bdVx, double bdVy) {
            this.nx = nx;
            this.ny = ny;
            this.af = af;
            this.bf = bf;
            this.adVx = adVx;
            this.adVy = adVy;
            this.bdVx = bdVx;
            this.bdVy = bdVy;
        }

        public double nx() {
            return nx;
        }

        public double ny() {
            return ny;
        }

        public double af() {
            return af;
        }

        public double bf() {
            return bf;
        }

        public double adVx() {
            return adVx;
        }

        public double adVy() {
            return adVy;
        }

        public double bdVx() {
            return bdVx;
        }

        public double bdVy() {
            return bdVy;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (CollisionData) obj;
            return Double.doubleToLongBits(this.nx) == Double.doubleToLongBits(that.nx) &&
                    Double.doubleToLongBits(this.ny) == Double.doubleToLongBits(that.ny) &&
                    Double.doubleToLongBits(this.af) == Double.doubleToLongBits(that.af) &&
                    Double.doubleToLongBits(this.bf) == Double.doubleToLongBits(that.bf) &&
                    Double.doubleToLongBits(this.adVx) == Double.doubleToLongBits(that.adVx) &&
                    Double.doubleToLongBits(this.adVy) == Double.doubleToLongBits(that.adVy) &&
                    Double.doubleToLongBits(this.bdVx) == Double.doubleToLongBits(that.bdVx) &&
                    Double.doubleToLongBits(this.bdVy) == Double.doubleToLongBits(that.bdVy);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nx, ny, af, bf, adVx, adVy, bdVx, bdVy);
        }

        @Override
        public String toString() {
            return "CollisionData[" +
                    "nx=" + nx + ", " +
                    "ny=" + ny + ", " +
                    "af=" + af + ", " +
                    "bf=" + bf + ", " +
                    "adVx=" + adVx + ", " +
                    "adVy=" + adVy + ", " +
                    "bdVx=" + bdVx + ", " +
                    "bdVy=" + bdVy + ']';
        }

    }
}