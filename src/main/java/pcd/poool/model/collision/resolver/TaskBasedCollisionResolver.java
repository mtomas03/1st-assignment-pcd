package pcd.poool.model.collision.resolver;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.ball.BallPair;
import pcd.poool.model.collision.util.CollisionAccumulator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Task-based collision resolver using broad-phase candidates.
 *
 * <p>Work is partitioned into callables submitted to an {@link ExecutorService}.
 * Each task accumulates collision deltas into a private map, while the shared
 * base class handles the reduction and application phase.
 */
public class TaskBasedCollisionResolver extends AbstractMapReduceCollisionResolver implements AutoCloseable {

    private record BigBallTaskResult(Map<Ball, CollisionAccumulator> map, Set<Ball> hitSmalls) {}

    private final ExecutorService executor;
    private final int taskCount;

    /**
     * Creates a resolver using a fixed thread pool with default broad-phase cell size.
     *
     * @param taskCount number of map tasks (must be greater than zero)
     */
    public TaskBasedCollisionResolver(int taskCount) {
        this(taskCount, 0.02);
    }

    /**
     * Creates a resolver using a fixed thread pool and an explicit cell size.
     *
     * @param taskCount number of map tasks (must be greater than zero)
     * @param cellSize uniform-grid cell size used by the broad phase
     */
    public TaskBasedCollisionResolver(int taskCount, double cellSize) {
        super(cellSize);
        if (taskCount <= 0) {
            throw new IllegalArgumentException("taskCount must be > 0");
        }
        this.executor = Executors.newFixedThreadPool(taskCount);
        this.taskCount = taskCount;
    }

    /**
     * Shuts down the internal executor service.
     *
     * <p>If graceful termination does not complete within a short timeout,
     * running tasks are canceled via {@code shutdownNow()}.
     */
    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Resolves collisions among small balls using task-based map-reduce.
     *
     * @param balls mutable list of small balls
     * @throws InterruptedException if interrupted while waiting for task results
     */
    @Override
    protected List<Map<Ball, CollisionAccumulator>> computeAccumulatorMaps(List<Ball> balls, List<BallPair> candidates)
            throws InterruptedException {
        List<Future<Map<Ball, CollisionAccumulator>>> futures = new ArrayList<>(taskCount);

        for (int t = 0; t < taskCount; t++) {
            final int tid = t;
            Callable<Map<Ball, CollisionAccumulator>> task = () -> {
                Map<Ball, CollisionAccumulator> localMap = new HashMap<>();
                super.processStridedCandidates(tid, taskCount, candidates, balls, localMap);
                return localMap;
            };
            futures.add(executor.submit(task));
        }

        List<Map<Ball, CollisionAccumulator>> results = new ArrayList<>(taskCount);
        try {
            for (Future<Map<Ball, CollisionAccumulator>> f : futures) {
                results.add(f.get());
            }
        } catch (ExecutionException e) {
            throw new RuntimeException("Error in collision task", e.getCause());
        }

        return results;
    }

    /**
     * Resolves big-ball collisions in parallel using task-local accumulator maps and hit sets.
     *
     * <p>Each task processes a strided subset of small balls. Accumulated deltas are
     * reduced serially and applied once to avoid shared mutation during the map phase.</p>
     *
     * @param bigBall big ball involved in collisions
     * @param hitter hitter attribution to assign to impacted small balls
     * @param smallBalls mutable list of small balls
     * @throws InterruptedException if interrupted while waiting for task completion
     */
    @Override
    public void resolveBigBallCollisions(Ball bigBall, Ball.HitBy hitter, List<Ball> smallBalls)
            throws InterruptedException {
        if (super.validateBigBallInputs(hitter, smallBalls)) {
            return;
        }

        List<Future<BigBallTaskResult>> futures = new ArrayList<>(taskCount);

        for (int t = 0; t < taskCount; t++) {
            final int tid = t;
            Callable<BigBallTaskResult> task = () -> {
                Map<Ball, CollisionAccumulator> localMap = new HashMap<>();
                Set<Ball> localHits = new HashSet<>();
                super.processStridedBigBallCandidates(tid, taskCount, bigBall, smallBalls, localMap, localHits);
                return new BigBallTaskResult(localMap, localHits);
            };
            futures.add(executor.submit(task));
        }

        List<Map<Ball, CollisionAccumulator>> accMaps = new ArrayList<>(taskCount);
        List<Set<Ball>> hitSets = new ArrayList<>(taskCount);
        try {
            for (Future<BigBallTaskResult> future : futures) {
                BigBallTaskResult result = future.get();
                accMaps.add(result.map());
                hitSets.add(result.hitSmalls());
            }
        } catch (ExecutionException e) {
            throw new RuntimeException("Error in big-ball collision task", e.getCause());
        }

        super.applyBigBallResults(bigBall, hitter, smallBalls, accMaps, hitSets);
    }
}
