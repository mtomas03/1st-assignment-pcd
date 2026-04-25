package pcd.poool.controller;

import pcd.poool.controller.command.CommandQueue;
import pcd.poool.model.ball.Ball;
import pcd.poool.model.board.Board;
import pcd.poool.util.V2d;

import java.util.List;
import java.util.Random;

/**
 * The bot runs in its own thread and autonomously generates kick commands for
 * the bot ball. Its behaviour is intentionally simple: it periodically kicks
 * towards the nearest small ball.
 *
 * <p>The bot is a producer in the producer-consumer architecture; it
 * writes commands to the shared {@link CommandQueue} without touching the model
 * directly.
 */
public class Bot extends Thread {

    private static final long THINK_INTERVAL_MS = 1000;
    private static final double KICK_SPEED = 1.4;

    private final CommandQueue queue;
    private final Board board;
    private final Random random = new Random();
    private volatile boolean running = true;

    public Bot(Board board, CommandQueue queue) {
        this.board = board;
        this.queue = queue;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (running && board.getStatus() == Board.GameStatus.PLAYING) {
            try {
                Thread.sleep(THINK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            Ball bb = board.getBotBall();
            if (bb.getVel().abs() < 0.1) {
                // Kick towards the nearest small ball
                V2d direction = pickDirection(bb);
                queue.botMove(direction);
            }
        }
    }

    private V2d pickDirection(Ball botBall) {
        var balls = board.getSmallBalls();
        if (balls.isEmpty()) {
            double angle = random.nextDouble() * 2 * Math.PI;
            return new V2d(Math.cos(angle), Math.sin(angle)).mul(KICK_SPEED);
        }

        var dir = directionToNearestSmallBall(botBall, balls);

        double noiseX = (random.nextDouble() - 0.5) * 0.4;
        double noiseY = (random.nextDouble() - 0.5) * 0.4;
        dir = new V2d(dir.x() + noiseX, dir.y() + noiseY).normalized();

        return dir.mul(KICK_SPEED);
    }

    private V2d directionToNearestSmallBall(Ball botBall, List<Ball> balls) {
        double bx = botBall.getPos().x();
        double by = botBall.getPos().y();

        Ball nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (Ball b : balls) {
            double dx = b.getPos().x() - bx;
            double dy = b.getPos().y() - by;
            double d = dx * dx + dy * dy;
            if (d < bestDist) {
                bestDist = d;
                nearest = b;
            }
        }

        assert nearest != null;

        double dx = nearest.getPos().x() - bx;
        double dy = nearest.getPos().y() - by;
        return new V2d(dx, dy).normalized();
    }

    public void stopBot() {
        running = false;
        interrupt();
    }
}
