package pcd.poool.controller.command;

import pcd.poool.util.BoundedBuffer;
import pcd.poool.util.V2d;

/**
 * Typed command queue providing factory methods for all game commands.
 * <p>
 * Implements the producer-consumer pattern: the GUI EDT and the Bot thread
 * are producers; the Controller is the sole consumer.
 */
public class CommandQueue {

    private static final double IMPULSE = 0.8;
    private final BoundedBuffer<Command> buffer = new BoundedBuffer<>(256);

    /**
     * Enqueues a player movement command using a direction vector.
     *
     * <p>Diagonal inputs are supported by passing both components.
     * The vector is normalized so diagonal movement does not travel
     * faster than axis-aligned movement.</p>
     *
     * @param dx horizontal movement component
     * @param dy vertical movement component
     */
    public void playerMove(double dx, double dy) {
        var direction = new V2d(dx, dy);
        if (direction.abs() < 1e-9) {
            return;
        }
        put(new PlayerMoveCommand(direction.normalized().mul(IMPULSE)));
    }

    public void playerUp() {
        playerMove(0, 1);
    }

    public void playerDown() {
        playerMove(0, -1);
    }

    public void playerLeft() {
        playerMove(-1, 0);
    }

    public void playerRight() {
        playerMove(1, 0);
    }

    public void botMove(V2d impulse) {
        put(new BotMoveCommand(impulse.mul(IMPULSE)));
    }

    private void put(Command command) {
        try {
            buffer.put(command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Non-blocking drain: returns {@code null} when the queue is empty.
     */
    public Command poll() {
        return buffer.poll();
    }
}
