package pcd.poool;

import pcd.poool.model.collision.resolver.NaiveSerialCollisionResolver;
import pcd.poool.controller.Bot;
import pcd.poool.controller.command.CommandQueue;
import pcd.poool.controller.Controller;
import pcd.poool.model.board.Board;
import pcd.poool.model.board.configuration.MassiveBoardConf;
import pcd.poool.model.collision.resolver.UniformGridSerialCollisionResolver;
import pcd.poool.view.View;
import pcd.poool.view.ViewModel;

/**
 * Application entry point for the Poool game.
 */
public class Poool {

    /**
     * Bootstraps MVC components, opens the GUI, and starts controller and bot threads.
     *
     * @param args command-line arguments (currently unused)
     */
    public static void main(String[] args) {
        var board = new Board(new MassiveBoardConf(),
                new UniformGridSerialCollisionResolver());

        var viewModel = new ViewModel();
        var cmdQueue = new CommandQueue();
        var view = new View(viewModel, cmdQueue);
        view.show();

        var bot = new Bot(board, cmdQueue);
        var controller = new Controller(board, view, cmdQueue, bot);

        controller.start();
        bot.start();
    }
}
