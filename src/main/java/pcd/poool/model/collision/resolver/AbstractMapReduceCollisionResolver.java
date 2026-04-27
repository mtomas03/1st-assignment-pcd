package pcd.poool.model.collision.resolver;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.ball.BallCollision;
import pcd.poool.model.collision.util.CollisionAccumulator;
import pcd.poool.model.collision.util.UniformGridBroadPhase;
import pcd.poool.model.ball.BallPair;

import java.util.List;
import java.util.Map;

/**
 * Shared support for collision resolvers that follow the same broad-phase plus map-reduce pipeline.
 *
 * <p>This abstract class centralizes the common steps between the threaded and
 * task-based implementations:
 * <ol>
 *   <li>compute broad-phase candidate pairs,</li>
 *   <li>delegate the map phase to the concrete execution strategy,</li>
 *   <li>reduce all local accumulators, and</li>
 *   <li>apply the merged deltas once per ball.</li>
 * </ol>
 * Concrete subclasses only provide the execution mechanism used to build the per-worker accumulator maps.</p>
 */
abstract class AbstractMapReduceCollisionResolver implements CollisionResolver {

    private final UniformGridBroadPhase broadPhase;

    /**
     * Creates a resolver support object using the provided uniform-grid cell size.
     *
     * @param cellSize uniform-grid cell size used by the broad phase
     */
    protected AbstractMapReduceCollisionResolver(double cellSize) {
        this.broadPhase = new UniformGridBroadPhase(cellSize);
    }

    /**
     * Resolves collisions by running the common pipeline and delegating the map phase to subclasses.
     *
     * @param balls mutable list of balls
     * @throws InterruptedException if the concrete execution strategy is interrupted while waiting for workers
     */
    @Override
    public final void resolve(List<Ball> balls) throws InterruptedException {
        List<BallPair> candidates = broadPhase.computeCandidates(balls);
        if (candidates.isEmpty()) {
            return;
        }

        List<Map<Ball, CollisionAccumulator>> accumulatorMaps = computeAccumulatorMaps(balls, candidates);
        reduceAndApply(balls, accumulatorMaps);
    }

    /**
     * Performs the map phase using the execution strategy chosen by the subclass.
     *
     * @param balls mutable list of balls
     * @param candidates broad-phase candidate pairs
     * @return one accumulator map per worker/task
     * @throws InterruptedException if the execution strategy is interrupted while waiting for completion
     */
    protected abstract List<Map<Ball, CollisionAccumulator>> computeAccumulatorMaps(
            List<Ball> balls,
            List<BallPair> candidates
    ) throws InterruptedException;

    /**
     * Reduces all per-worker maps and applies the resulting deltas once per ball.
     *
     * @param balls mutable list of balls to update
     * @param accumulatorMaps per-worker accumulator maps produced by the map phase
     */
    protected final void reduceAndApply(List<Ball> balls, List<Map<Ball, CollisionAccumulator>> accumulatorMaps) {
        for (Ball ball : balls) {
            double dx = 0, dy = 0, dvx = 0, dvy = 0;
            for (Map<Ball, CollisionAccumulator> map : accumulatorMaps) {
                CollisionAccumulator acc = map.get(ball);
                if (acc != null) {
                    dx += acc.getDx();
                    dy += acc.getDy();
                    dvx += acc.getDvx();
                    dvy += acc.getDvy();
                }
            }
            applyMergedDeltas(ball, dx, dy, dvx, dvy);
        }
    }

    /**
     * Applies the merged deltas to a single ball, preserving the same clamping policy used before the refactor.
     *
     * @param ball target ball
     * @param dx total x position delta
     * @param dy total y position delta
     * @param dvx total x velocity delta
     * @param dvy total y velocity delta
     */
    protected final void applyMergedDeltas(Ball ball, double dx, double dy, double dvx, double dvy) {
        if (dx == 0 && dy == 0 && dvx == 0 && dvy == 0) {
            return;
        }

        CollisionAccumulator merged = new CollisionAccumulator();
        merged.add(dx, dy, dvx, dvy);
        BallCollision.applyAccumulator(ball, merged);
    }

    /**
     * Executes a strided work distribution over candidate pairs.
     *
     * <p>Each worker is assigned a subset of candidates using a strided pattern:
     * worker 0 gets indices 0, workerCount, 2*workerCount, etc.
     * worker 1 gets indices 1, 1+workerCount, 1+2*workerCount, etc.
     *
     * @param workerId worker/thread identifier (0 to workerCount-1)
     * @param workerCount total number of workers/threads
     * @param candidates candidate pairs to process
     * @param balls ball list for index lookups
     * @param localMap local accumulator map for this worker
     */
    protected static void processStridedCandidates(
            int workerId,
            int workerCount,
            List<BallPair> candidates,
            List<Ball> balls,
            Map<Ball, CollisionAccumulator> localMap) {
        int m = candidates.size();
        for (int k = workerId; k < m; k += workerCount) {
            BallPair pair = candidates.get(k);
            Ball a = balls.get(pair.i());
            Ball b = balls.get(pair.j());
            CollisionAccumulator accA = localMap.computeIfAbsent(a, ignored -> new CollisionAccumulator());
            CollisionAccumulator accB = localMap.computeIfAbsent(b, ignored -> new CollisionAccumulator());
            BallCollision.resolveAccumulator(a, b, accA, accB);
        }
    }
}


