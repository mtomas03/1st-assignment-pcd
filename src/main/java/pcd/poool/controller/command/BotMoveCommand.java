package pcd.poool.controller.command;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.board.Board;
import pcd.poool.util.V2d;

/**
 * Applies a velocity impulse to the bot ball.
 */
public class BotMoveCommand implements Command {
    private final V2d impulse;

    BotMoveCommand(V2d impulse) {
        this.impulse = impulse;
    }

    @Override
    public void execute(Board board) {
        Ball bb = board.getBotBall();
        bb.kick(bb.getVel().sum(impulse));
    }
}
