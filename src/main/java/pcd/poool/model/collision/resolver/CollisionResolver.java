package pcd.poool.model.collision.resolver;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.ball.BallCollision;

import java.util.List;

/**
 * Strategy for resolving collisions among balls.
 * Implementations define how small-ball collisions are processed.
 */
@FunctionalInterface
public interface CollisionResolver {

    /**
     * Resolves all pairwise collisions among the given list of small balls.
     *
     * @param balls the list of small balls
     * @throws InterruptedException if the current thread is interrupted
     */
    void resolve(List<Ball> balls) throws InterruptedException;

    /**
     * Resolves collisions between one big ball (player or bot) and all small balls.
     *
     * <p>The default implementation is sequential and keeps behavior compatible
     * with serial resolvers. Parallel resolvers can override this method to run
     * the same logic using their own map-reduce strategy.</p>
     *
     * @param bigBall big ball involved in collisions
     * @param hitter hitter attribution to write on impacted small balls
     * @param smallBalls mutable list of small balls
     * @throws InterruptedException if the execution strategy is interrupted
     */
    default void resolveBigBallCollisions(Ball bigBall, Ball.HitBy hitter, List<Ball> smallBalls)
            throws InterruptedException {
        if (hitter == Ball.HitBy.NONE) {
            throw new IllegalArgumentException("NONE is not a valid hitter for a big ball");
        }
        for (Ball small : smallBalls) {
            if (BallCollision.resolve(bigBall, small)) {
                small.setLastHitBy(hitter);
            }
        }
    }
}
