package pcd.poool.view;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.board.Board;
import pcd.poool.model.board.Hole;
import pcd.poool.view.info.BallInfo;
import pcd.poool.view.info.HoleInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe view model based on immutable snapshots.
 *
 * <p>Swing EDT must read a consistent game state
 * for each frame, while the controller thread keeps producing new states.
 * Instead of synchronizing many fine-grained getters,
 * this class builds a complete immutable {@link Snapshot} in {@link #update}
 * and then publishes it atomically.
 */
public class ViewModel {

    private volatile Snapshot snapshot = new Snapshot(
            List.of(),
            List.of(),
            null,
            null,
            0,
            0,
            0,
            Board.GameStatus.PLAYING
    );

    public record Snapshot(
            List<BallInfo> smallBalls,
            List<HoleInfo> holes,
            BallInfo playerBall,
            BallInfo botBall,
            int playerScore,
            int botScore,
            int fps,
            Board.GameStatus status
    ) {
    }

    /**
     * Builds and publishes a new immutable snapshot of the current board state.
     *
     * <p>Called by the controller thread after each simulation step.
     * The method is synchronized to ensure snapshot construction is serialized.
     */
    public synchronized void update(Board board, int fps) {
        List<BallInfo> small = new ArrayList<>(board.getSmallBalls().size());
        for (Ball b : board.getSmallBalls()) {
            small.add(new BallInfo(b.getPos(), b.getRadius()));
        }

        Ball pb = board.getPlayerBall();
        BallInfo player = new BallInfo(pb.getPos(), pb.getRadius());

        Ball bb = board.getBotBall();
        BallInfo bot = new BallInfo(bb.getPos(), bb.getRadius());

        List<HoleInfo> holes = snapshot.holes();
        if (holes.isEmpty()) {
            List<HoleInfo> hs = new ArrayList<>(board.getHoles().size());
            for (Hole h : board.getHoles()) {
                hs.add(new HoleInfo(h.center(), h.radius()));
            }
            holes = List.copyOf(hs);
        }

        snapshot = new Snapshot(
                List.copyOf(small),
                holes,
                player,
                bot,
                board.getPlayerScore(),
                board.getBotScore(),
                fps,
                board.getStatus()
        );
    }

    /**
     * Returns the latest published immutable snapshot.
     *
     * <p>No synchronization is required here: reading a volatile reference is a single
     * atomic read, and volatile semantics guarantee visibility of the latest published
     * snapshot. Note that volatile does not make compound operations atomic.
     */
    public Snapshot getSnapshot() {
        return snapshot;
    }
}
