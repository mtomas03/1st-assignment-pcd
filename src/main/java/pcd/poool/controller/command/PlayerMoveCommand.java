package pcd.poool.controller.command;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.board.Board;
import pcd.poool.util.V2d;

/**
 * Applies a velocity impulse to the player ball (human input).
 * Arrow keys translate into one of four unit-direction impulses.
 */
public class PlayerMoveCommand implements Command {
    private final V2d impulse;

    PlayerMoveCommand(V2d impulse) {
        this.impulse = impulse;
    }

    @Override
    public void execute(Board board) {
        Ball pb = board.getPlayerBall();
        pb.kick(pb.getVel().sum(impulse));
    }
}
