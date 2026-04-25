package pcd.poool.controller.command;

import pcd.poool.model.board.Board;

/**
 * Command interface: encapsulates an action to perform on the game model.
 */
public interface Command {
    void execute(Board board);
}