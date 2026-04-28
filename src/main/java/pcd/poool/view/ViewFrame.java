package pcd.poool.view;

import pcd.poool.controller.command.CommandQueue;
import pcd.poool.model.board.Board;
import pcd.poool.util.RenderSynch;
import pcd.poool.view.info.BallInfo;
import pcd.poool.view.info.HoleInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Main Swing window of the game.
 *
 * <p>This class bridges two threads:
 * <ul>
 *   <li>the controller thread requests frames via {@link #render()},</li>
 *   <li>the Swing EDT executes painting and keyboard input handling.</li>
 * </ul>
 *
 * <p>{@link RenderSynch} enforces a synchronous render contract: each call to
 * {@link #render()} blocks until the EDT has finished painting the requested
 * frame, so the controller can safely produce the next snapshot afterward.
 */
public class ViewFrame extends JFrame {

    private static final Color COLOR_OVERLAY = new Color(0, 0, 0, 180);

    private static final int W = 1200, H = 800;

    private final ViewModel viewModel;
    private final CommandQueue cmdQueue;
    private final RenderSynch sync = new RenderSynch();
    private final VisualiserPanel panel;

    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;

    /**
     * Creates the main window and registers keyboard handlers.
     *
     * @param viewModel immutable state source consumed by the paint routine
     * @param cmdQueue queue where input commands are enqueued
     */
    public ViewFrame(ViewModel viewModel, CommandQueue cmdQueue) {
        super("Poool");
        this.viewModel = viewModel;
        this.cmdQueue = cmdQueue;

        setSize(W, H + 25);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.panel = new VisualiserPanel();
        getContentPane().add(panel);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                updatePressedState(e.getKeyCode(), true);
                enqueueCurrentMovement();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                updatePressedState(e.getKeyCode(), false);
                enqueueCurrentMovement();
            }
        });
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
    }

    private void updatePressedState(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                upPressed = pressed;
                return;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                downPressed = pressed;
                return;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                leftPressed = pressed;
                return;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                rightPressed = pressed;
                return;
            default:
        }
    }

    private void enqueueCurrentMovement() {
        double vx = 0;
        double vy = 0;

        if (upPressed) vy += 1;
        if (downPressed) vy -= 1;
        if (leftPressed) vx -= 1;
        if (rightPressed) vx += 1;

        if (vx != 0 || vy != 0) {
            cmdQueue.playerMove(vx, vy);
        }
    }

    /**
     * Requests a repaint and waits until the EDT has completed the frame.
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void render() throws InterruptedException {
        long frame = sync.nextFrameToRender();
        panel.repaint();
        sync.waitForFrameRendered(frame);
    }

    private class VisualiserPanel extends JPanel {

        VisualiserPanel() {
            setSize(W, H);
        }

        /**
         * Paints the complete frame using the latest immutable snapshot.
         *
         * @param g Swing graphics context provided by the EDT
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();
            int ox = width / 2;
            int oy = height / 2;
            int scale = Math.min(ox, oy);

            g2.setColor(Color.LIGHT_GRAY);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(ox, 0, ox, oy * 2);
            g2.drawLine(0, oy, ox * 2, oy);

            ViewModel.Snapshot s = viewModel.getSnapshot();

            g2.setColor(Color.BLACK);
            for (HoleInfo h : s.holes()) {
                int hx = ox + (int) (h.center().x() * scale);
                int hy = oy - (int) (h.center().y() * scale);
                int hr = (int) (h.radius() * scale);
                g2.fillOval(hx - hr, hy - hr, hr * 2, hr * 2);
            }

            g2.setStroke(new BasicStroke(1));
            for (BallInfo b : s.smallBalls()) {
                drawBall(g2, b, ox, oy, scale);
            }

            BallInfo pb = s.playerBall();
            if (pb != null) {
                drawLabeledBall(g2, pb, "P", ox, oy, scale);
            }

            BallInfo bb = s.botBall();
            if (bb != null) {
                drawLabeledBall(g2, bb, "B", ox, oy, scale);
            }

            drawHud(g2, s.smallBalls().size(), s.fps(), s.playerScore(), s.botScore());

            if (s.status() != Board.GameStatus.PLAYING) {
                drawGameOverOverlay(g2, s.status(), s.playerScore(), s.botScore());
            }

            sync.notifyFrameRendered();
        }

        private void drawBall(Graphics2D g2, BallInfo b, int ox, int oy, int scale) {
            int x = ox + (int) (b.pos().x() * scale);
            int y = oy - (int) (b.pos().y() * scale);
            int r = Math.max(1, (int) (b.radius() * scale));
            g2.drawOval(x - r, y - r, r * 2, r * 2);
        }

        private void drawLabeledBall(Graphics2D g2, BallInfo b, String label, int ox, int oy, int scale) {
            int x = ox + (int) (b.pos().x() * scale);
            int y = oy - (int) (b.pos().y() * scale);
            int r = Math.max(10, (int) (b.radius() * scale));

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(x - r, y - r, r * 2, r * 2);

            Font old = g2.getFont();
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, r)));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x - fm.stringWidth(label) / 2;
            int ty = y + (fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(label, tx, ty);
            g2.setFont(old);
        }

        private void drawHud(Graphics2D g2, int numSmallBalls, int fps, int playerScore, int botScore) {
            g2.setColor(Color.BLUE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));

            FontMetrics fm = g2.getFontMetrics();
            String[] lines = {
                    "Num small balls: " + numSmallBalls,
                    "FPS: " + fps,
                    "Player: " + playerScore,
                    "Bot: " + botScore
            };

            int y = 30;
            int lineSpacing = 20;
            for (String line : lines) {
                int x = (getWidth() - fm.stringWidth(line)) / 2;
                g2.drawString(line, x, y);
                y += lineSpacing;
            }
        }

        private void drawGameOverOverlay(Graphics2D g2, Board.GameStatus status, int playerScore, int botScore) {
            g2.setColor(COLOR_OVERLAY);
            g2.fillRect(0, 0, getWidth(), getHeight());

            String msg;
            switch (status) {
                case PLAYER_WINS:
                    msg = "Victory: Player wins the match!";
                    break;
                case BOT_WINS:
                    msg = "Defeat: Bot wins the match!";
                    break;
                case DRAW:
                    msg = "Match ended in a draw.";
                    break;
                case PLAYER_DEAD:
                    msg = "Defeat: Player eliminated!";
                    break;
                case BOT_DEAD:
                    msg = "Victory: Bot eliminated!";
                    break;
                default:
                    msg = "Game over!";
                    break;
            }

            g2.setFont(new Font("SansSerif", Font.BOLD, 48));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(msg)) / 2;
            int ty = (getHeight() + fm.getAscent()) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(msg, tx, ty);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
            String sub = "Final score — Player: " + playerScore
                    + "   Bot: " + botScore;
            FontMetrics fm2 = g2.getFontMetrics();
            g2.setColor(new Color(200, 200, 200));
            g2.drawString(sub, (getWidth() - fm2.stringWidth(sub)) / 2, ty + 60);
        }
    }
}
