package pcd.poool.model.collision.resolver;

import pcd.poool.model.ball.Ball;

import java.util.List;

/**
 * Strategy interface for small-ball vs small-ball collision resolution.
 *
 * <p>Implementations range from a simple O(n²) serial pass to fully parallel
 * multithreaded and task-based approaches. The interface is kept intentionally
 * narrow: the resolver only handles small-ball interactions; player and bot
 * collisions are handled separately by the board update pipeline.
 */
public interface CollisionResolver {

    /**
     * Resolves all pairwise collisions among the given list of small balls.
     *
     * @param balls the list of small balls
     * @throws InterruptedException if the current thread is interrupted
     */
    void resolve(List<Ball> balls) throws InterruptedException;
}
