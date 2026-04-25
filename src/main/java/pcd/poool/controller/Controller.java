package pcd.poool.controller;

import pcd.poool.controller.command.Command;
import pcd.poool.controller.command.CommandQueue;
import pcd.poool.model.board.Board;
import pcd.poool.util.RenderSynch;
import pcd.poool.view.View;

/**
 * The Controller is the active component that drives the entire
 * game loop. It runs in its own thread and is the only
 * component allowed to modify the {@link Board}.
 *
 * <h3>Main loop</h3>
 * At each iteration the controller:
 * <ol>
 *   <li>drains and executes pending commands in the {@link CommandQueue},</li>
 *   <li>computes elapsed time since the previous iteration,</li>
 *   <li>updates the Board’s state,</li>
 *   <li>updates the ViewModel and triggers a synchronous render.</li>
 * </ol>
 *
 * Rendering is performed synchronously: the Controller blocks until the
 * EDT finishes painting (via {@link RenderSynch}). This keeps the ViewModel
 * consistent, at the cost of coupling frame-rate to paint latency.
 */
public class Controller extends Thread {

    private static final boolean HOLES_ENABLED = true;

    private final Board board;
    private final View view;
    private final CommandQueue cmdQueue;
    private final Bot bot;

    private volatile boolean running = true;

    /**
     * Creates the controller thread.
     *
     * @param board game model updated by this controller
     * @param view view facade used for snapshot publication and rendering
     * @param cmdQueue command queue consumed each frame
     * @param bot optional bot controller to stop on game end
     */
    public Controller(Board board,
                      View view,
                      CommandQueue cmdQueue,
                      Bot bot) {
        this.board = board;
        this.view = view;
        this.cmdQueue = cmdQueue;
        this.bot = bot;
        setDaemon(true);
    }

    @Override
    public void run() {
        long lastUpdateTime = System.currentTimeMillis();
        long t0 = lastUpdateTime;
        int nFrames = 0;

        while (running && board.getStatus() == Board.GameStatus.PLAYING) {
            try {
                processCommands();

                long now = System.currentTimeMillis();
                long elapsed = now - lastUpdateTime;
                lastUpdateTime = now;

                board.step(elapsed, HOLES_ENABLED);

                nFrames++;
                long dt = System.currentTimeMillis() - t0;
                int fps = dt > 0 ? (int) (nFrames * 1000L / dt) : 0;
                view.getViewModel().update(board, fps);
                view.render();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Game over: push final state one more time
        try {
            view.getViewModel().update(board, 0);
            view.render();
        } catch (InterruptedException ignored) {
        }

        if (bot != null) bot.stopBot();
    }

    /**
     * Executes all queued commands in FIFO order until the queue is empty.
     */
    private void processCommands() {
        Command command;
        while ((command = cmdQueue.poll()) != null) {
            command.execute(board);
        }
    }
}
