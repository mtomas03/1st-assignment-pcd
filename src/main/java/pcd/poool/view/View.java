package pcd.poool.view;

import pcd.poool.controller.command.CommandQueue;

import javax.swing.*;

/**
 * Thin facade that wraps the {@link ViewFrame}.
 *
 * <p>Responsible for creating the window on the EDT and exposing a
 * {@link #render()} call that can be invoked from the Controller thread.
 */
public class View {

    private final ViewFrame frame;
    private final ViewModel viewModel;

    /**
     * Creates a view facade bound to the provided model and command queue.
     *
     * @param viewModel immutable-snapshot provider consumed by the Swing UI
     * @param cmdQueue shared command queue receiving user input commands
     */
    public View(ViewModel viewModel, CommandQueue cmdQueue) {
        this.frame = new ViewFrame(viewModel, cmdQueue);
        this.viewModel = viewModel;
    }

    /**
     * Makes the window visible on the EDT.
     */
    public void show() {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
            frame.requestFocusInWindow();
        });
    }

    /**
     * Called by the Controller thread to request a synchronous repaint.
     */
    public void render() throws InterruptedException {
        frame.render();
    }

    /**
     * Returns the view model currently exposed by this facade.
     *
     * @return view model used by the backing frame
     */
    public ViewModel getViewModel() {
        return this.viewModel;
    }
}
