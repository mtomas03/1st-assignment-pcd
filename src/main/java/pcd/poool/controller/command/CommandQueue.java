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

    public void playerUp() {
        put(new PlayerMoveCommand(new V2d(0, IMPULSE)));
    }

    public void playerDown() {
        put(new PlayerMoveCommand(new V2d(0, -IMPULSE)));
    }

    public void playerLeft() {
        put(new PlayerMoveCommand(new V2d(-IMPULSE, 0)));
    }

    public void playerRight() {
        put(new PlayerMoveCommand(new V2d(IMPULSE, 0)));
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
