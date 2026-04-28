package pcd.poool.benchmark;

import pcd.poool.controller.Bot;
import pcd.poool.controller.command.Command;
import pcd.poool.controller.command.CommandQueue;
import pcd.poool.model.board.Board;
import pcd.poool.model.board.configuration.MassiveBoardConf;
import pcd.poool.model.collision.resolver.*;
import pcd.poool.view.View;
import pcd.poool.view.ViewModel;

import java.util.Optional;
import java.util.Random;
import javax.swing.SwingUtilities;

/**
 * End-to-end benchmark for collision resolver implementations.
 *
 * <p>The benchmark executes the same game loop used at runtime to compare
 * measured FPS across resolver strategies under comparable conditions.
 *
 * <p>Reported metrics:
 * <ul>
 *   <li>{@code FPS}: average rendered frames per second for one run,</li>
 *   <li>{@code Speedup}: ratio against serial baseline FPS,</li>
 *   <li>{@code Efficiency}: speedup divided by thread/task count.</li>
 * </ul>
 */
public class CollisionResolverBenchmark {

    private static final long BENCH_DURATION_MS = 60_000L;
    private static final boolean ENABLE_SYNTHETIC_PLAYER = true;
    private static final long PLAYER_INPUT_INTERVAL_MS = 1000L;
    private static final boolean HOLES_ENABLED = false;

    /**
     * Runs the full benchmark suite and prints comparative results.
     *
     * @param args command-line arguments (currently unused)
     * @throws InterruptedException if the benchmark thread is interrupted
     */
    public static void main(String[] args) throws InterruptedException {

        System.out.println("=".repeat(70));
        System.out.println("  Poool - Collision Resolver Benchmark");
        System.out.printf("  Configuration: MassiveBoardConf (4 500 balls) | Duration: %d s%n",
                BENCH_DURATION_MS / 1000);
        System.out.println("=".repeat(70));
        System.out.printf("%-10s %-42s %8s %8s %10s%n",
                "Threads", "Resolver", "FPS", "Speedup", "Efficiency");

        double naiveSerialFps = runBenchmark(new NaiveSerialCollisionResolver());
        printRow(1, "NaiveSerialCollisionResolver", naiveSerialFps, null, null);

        double uniformGridSerialFps = runBenchmark(new UniformGridSerialCollisionResolver());
        printRow(1, "UniformGridSerialCollisionResolver (baseline)", uniformGridSerialFps,
                1.0, 1.0);

        for (int n : new int[]{1, 2, 4, 8, 16}) {
            double fps = runBenchmark(new ThreadedCollisionResolver(n));
            double speedup = fps / uniformGridSerialFps;
            double eff = speedup / n;
            printRow(n, "ThreadedCollisionResolver", fps, speedup, eff);
        }

        for (int n : new int[]{1, 2, 4, 8, 16}) {
            double fps = runBenchmark(new TaskBasedCollisionResolver(n));
            double speedup = fps / uniformGridSerialFps;
            double eff = speedup / n;
            printRow(n, "TaskBasedCollisionResolver", fps, speedup, eff);
        }

        System.out.println("=".repeat(70));
    }

    /**
     * Runs the given resolver with the same main-loop sequence as the runtime
     * controller: command drain, elapsed-time update, board step, ViewModel update
     * and synchronous render.
     *
     * <p>Each run creates a fresh board and UI, starts a bot, optionally starts a
     * synthetic player input thread, and ensures all resources are released in a
     * {@code finally} block.
     *
     * @param resolver collision resolver implementation under test
     * @return measured average FPS for the whole run
     * @throws InterruptedException if interrupted while running or joining helper threads
     */
    private static double runBenchmark(CollisionResolver resolver)
            throws InterruptedException {

        var board = new Board(new MassiveBoardConf(), resolver);
        var cmdQueue = new CommandQueue();
        var viewModel = new ViewModel();
        var view = new View(viewModel, cmdQueue);

        view.show();
        // Let Swing realize the frame before entering synchronous render.
        Thread.sleep(150);

        var bot = new Bot(board, cmdQueue);
        bot.start();

        Thread playerInputThread = null;
        if (ENABLE_SYNTHETIC_PLAYER) {
            playerInputThread = startSyntheticPlayer(cmdQueue);
        }

        long now = System.currentTimeMillis();
        long t0 = now;
        long lastUpdateTime = now;
        long frames = 0;

        try {
            while (System.currentTimeMillis() - t0 < BENCH_DURATION_MS
                    && board.getStatus() == Board.GameStatus.PLAYING) {

                processCommands(cmdQueue, board);

                now = System.currentTimeMillis();
                long elapsed = Math.max(1L, now - lastUpdateTime);
                lastUpdateTime = now;

                board.step(elapsed, HOLES_ENABLED);

                frames++;
                long dt = Math.max(1L, System.currentTimeMillis() - t0);
                int fps = (int) (frames * 1000L / dt);

                viewModel.update(board, fps);
                view.render();
            }
        } finally {
            bot.stopBot();
            bot.join(500);

            if (resolver instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) resolver).close();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to close resolver", e);
                }
            }

            if (playerInputThread != null) {
                playerInputThread.interrupt();
                playerInputThread.join(500);
            }

            disposeBenchmarkWindows();
        }

        long elapsedTotal = System.currentTimeMillis() - t0;
        return elapsedTotal > 0 ? (double) frames / (elapsedTotal / 1000.0) : 0.0;
    }

    private static void processCommands(CommandQueue cmdQueue, Board board) {
        Command command;
        while ((command = cmdQueue.poll()) != null) {
            command.execute(board);
        }
    }

    private static Thread startSyntheticPlayer(CommandQueue cmdQueue) {
        Thread player = new Thread(() -> {
            Random rng = new Random(42);
            while (!Thread.currentThread().isInterrupted()) {
                int choice = rng.nextInt(4);
                switch (choice) {
                    case 0:
                        cmdQueue.playerUp();
                        break;
                    case 1:
                        cmdQueue.playerDown();
                        break;
                    case 2:
                        cmdQueue.playerLeft();
                        break;
                    default:
                        cmdQueue.playerRight();
                        break;
                }
                try {
                    Thread.sleep(PLAYER_INPUT_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        player.setDaemon(true);
        player.setName("BenchmarkSyntheticPlayer");
        player.start();
        return player;
    }

    private static void disposeBenchmarkWindows() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.Window window : java.awt.Window.getWindows()) {
                    if (window.isDisplayable()) {
                        window.dispose();
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static void printRow(int n, String name, double fps,
                                 Double speedup, Double eff) {
        System.out.printf("%-10d %-42s %8.2f %8s %10s%n",
                n, name, fps,
                speedup != null ? String.format("%.2fx", speedup) : "-",
                eff != null ? String.format("%.2f", eff) : "-");
    }
}
