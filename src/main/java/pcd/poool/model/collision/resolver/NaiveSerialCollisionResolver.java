package pcd.poool.model.collision.resolver;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.ball.BallCollision;

import java.util.List;

/**
 * Serial collision resolver using a naive all-pairs O(n^2) scan.
 *
 * <p>Every unordered pair of balls is checked exactly once per frame in the
 * caller thread.
 */
public class NaiveSerialCollisionResolver implements CollisionResolver {

    /**
     * Creates a serial collision resolver with naive implementation.
     */
    public NaiveSerialCollisionResolver() {}

    /**
     * Resolves small-ball collisions sequentially for all unordered pairs.
     *
     * @param balls mutable list of small balls
     */
    @Override
    public void resolve(List<Ball> balls) {
        for (int i = 0; i < balls.size(); i++) {
            for (int j = i + 1; j < balls.size(); j++) {
                BallCollision.resolve(balls.get(i), balls.get(j));
            }
        }
    }
}