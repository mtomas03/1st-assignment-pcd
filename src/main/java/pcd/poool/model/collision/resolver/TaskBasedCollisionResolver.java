package pcd.poool.model.collision.resolver;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.ball.BallCollision;
import pcd.poool.model.collision.util.BallPair;
import pcd.poool.model.collision.util.UniformGridBroadPhase;
import pcd.poool.model.collision.util.CollisionAccumulator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Each task accumulates collision deltas into a private map (map phase), and
 * the caller merges and applies those deltas once per ball (reduce phase).
 */
public class TaskBasedCollisionResolver implements CollisionResolver, AutoCloseable {

    private static final double DEFAULT_CELL_SIZE = 0.02;

    private final ExecutorService executor;
    private final int taskCount;
    private final UniformGridBroadPhase broadPhase;

    /**
     * Creates a resolver using a fixed thread pool with default broad-phase cell size.
     *
     * @param taskCount number of map tasks (must be greater than zero)
     */
    public TaskBasedCollisionResolver(int taskCount) {
        this(taskCount, DEFAULT_CELL_SIZE);
    }

    /**
     * Creates a resolver using a fixed thread pool and an explicit cell size.
     *
     * @param taskCount number of map tasks (must be greater than zero)
     * @param cellSize uniform-grid cell size used by the broad phase
     */
    public TaskBasedCollisionResolver(int taskCount, double cellSize) {
        if (taskCount <= 0) {
            throw new IllegalArgumentException("taskCount must be > 0");
        }
        this.executor = Executors.newFixedThreadPool(taskCount);
        this.taskCount = taskCount;
        this.broadPhase = new UniformGridBroadPhase(cellSize);
    }

    /**
     * Shuts down the internal executor service.
     *
     * <p>If graceful termination does not complete within a short timeout,
     * running tasks are cancelled via {@code shutdownNow()}.
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
    public void resolve(List<Ball> balls) throws InterruptedException {
        List<BallPair> candidates = broadPhase.computeCandidates(balls);
        int m = candidates.size();
        if (m == 0) return;

        List<Future<Map<Ball, CollisionAccumulator>>> futures = new ArrayList<>(taskCount);

        for (int t = 0; t < taskCount; t++) {
            final int tid = t;
            Callable<Map<Ball, CollisionAccumulator>> task = () -> {
                Map<Ball, CollisionAccumulator> localMap = new HashMap<>();
                for (int k = tid; k < m; k += taskCount) {
                    BallPair pair = candidates.get(k);
                    Ball a = balls.get(pair.i());
                    Ball b = balls.get(pair.j());
                    CollisionAccumulator accA = localMap.computeIfAbsent(a, ignored -> new CollisionAccumulator());
                    CollisionAccumulator accB = localMap.computeIfAbsent(b, ignored -> new CollisionAccumulator());
                    BallCollision.resolveAccumulator(a, b, accA, accB);
                }
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

        for (Ball ball : balls) {
            double dx = 0, dy = 0, dvx = 0, dvy = 0;
            for (Map<Ball, CollisionAccumulator> map : results) {
                CollisionAccumulator acc = map.get(ball);
                if (acc != null) {
                    dx += acc.getDx();
                    dy += acc.getDy();
                    dvx += acc.getDvx();
                    dvy += acc.getDvy();
                }
            }
            if (dx != 0 || dy != 0 || dvx != 0 || dvy != 0) {
                CollisionAccumulator merged = new CollisionAccumulator();
                merged.add(dx, dy, dvx, dvy);
                BallCollision.applyAccumulator(ball, merged);
            }
        }
    }
}
