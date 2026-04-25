package pcd.poool.model.collision.resolver;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.ball.BallCollision;
import pcd.poool.model.collision.util.BallPair;
import pcd.poool.model.collision.util.UniformGridBroadPhase;

import java.util.List;

/**
 * Serial collision resolver using a uniform-grid broad phase.
 *
 * <p>Candidate pairs are generated once per frame and then resolved
 * sequentially in the caller thread.
 */
public class UniformGridSerialCollisionResolver implements CollisionResolver {

    private static final double DEFAULT_CELL_SIZE = 0.02;

    private final UniformGridBroadPhase broadPhase;

    /**
     * Creates a serial resolver with a default broad-phase cell size.
     */
    public UniformGridSerialCollisionResolver() {
        this(DEFAULT_CELL_SIZE);
    }

    /**
     * Creates a serial resolver with explicit broad-phase cell size.
     *
     * @param cellSize uniform-grid cell size used by the broad phase
     */
    public UniformGridSerialCollisionResolver(double cellSize) {
        this.broadPhase = new UniformGridBroadPhase(cellSize);
    }

    /**
     * Resolves small-ball collisions sequentially for all candidate pairs.
     *
     * @param balls mutable list of small balls
     */
    @Override
    public void resolve(List<Ball> balls) {
        List<BallPair> candidates = broadPhase.computeCandidates(balls);
        for (BallPair pair : candidates) {
            BallCollision.resolve(balls.get(pair.i()), balls.get(pair.j()));
        }
    }
}
