package pcd.poool.model.board;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.ball.BallCollision;
import pcd.poool.model.board.configuration.BoardConf;
import pcd.poool.model.collision.resolver.CollisionResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the full game state (balls, holes, scores, status)
 * and provides the physics-update step.
 *
 * <p> Only the Controller thread modifies this object directly.
 * The View accesses it via a snapshot (ViewModel),
 * so no synchronization on Board itself is needed.
 */
public class Board {

    public enum GameStatus {PLAYING, PLAYER_WINS, BOT_WINS, DRAW, PLAYER_DEAD, BOT_DEAD}

    private final List<Ball> smallBalls;
    private final Ball playerBall;
    private final Ball botBall;
    private final List<Hole> holes;
    private final Boundary bounds;

    private final CollisionResolver collisionResolver;

    private int playerScore = 0;
    private int botScore = 0;
    private GameStatus status = GameStatus.PLAYING;

    /**
     * Creates a new board state from configuration and collision strategy.
     *
     * @param conf board configuration containing initial entities and boundaries
     * @param collisionResolver strategy used for small-ball collisions
     */
    public Board(BoardConf conf, CollisionResolver collisionResolver) {
        this.smallBalls = new ArrayList<>(conf.getSmallBalls());
        this.playerBall = conf.getPlayerBall();
        this.botBall = conf.getBotBall();
        this.holes = conf.getHoles();
        this.bounds = conf.getBoundary();
        this.collisionResolver = collisionResolver;
    }

    /**
     * The step updates movement, resolves collisions, and optionally evaluates
     * hole interactions and scoring.
     *
     * @param dt elapsed time in milliseconds
     * @param holesEnabled whether hole scoring/elimination must be evaluated
     * @throws InterruptedException if collision resolution is interrupted
     */
    public void step(long dt, boolean holesEnabled) throws InterruptedException {
        if (status != GameStatus.PLAYING) return;

        updatePositions(dt);
        checkCollisions();

        if (holesEnabled) {
            checkHoles();
        }
    }

    /**
     * Updates the positions and velocities of all balls (friction + boundary).
     * Collision resolution is handled separately by the {@code CollisionResolver}.
     */
    private void updatePositions(long dt) {
        playerBall.updateState(dt, bounds);
        botBall.updateState(dt, bounds);
        for (Ball b : smallBalls) b.updateState(dt, bounds);
    }

    /**
     * Resolves player-small, bot-small, and small-small collisions.
     *
     * @throws InterruptedException if the configured small-ball resolver is interrupted
     */
    private void checkCollisions() throws InterruptedException {
        resolveBigBallCollisions(Ball.HitBy.PLAYER);
        resolveBigBallCollisions(Ball.HitBy.BOT);
        collisionResolver.resolve(smallBalls);
    }

    /**
     * Resolves collisions between one big ball (player or bot) and all small balls.
     *
     * @param hitter identifies which big ball is used and recorded as last hitter
     */
    private void resolveBigBallCollisions(Ball.HitBy hitter) {
        Ball bigBall = switch (hitter) {
            case PLAYER -> playerBall;
            case BOT -> botBall;
            case NONE -> throw new IllegalArgumentException("NONE is not a valid player");
        };

        for (Ball b : smallBalls) {
            boolean hit = BallCollision.resolve(bigBall, b);
            if (hit) {
                b.setLastHitBy(hitter);
            }
        }
    }

    /**
     * Checks whether any ball has entered a hole, updates scores, removes
     * scored balls, and evaluates win conditions.
     */
    public void checkHoles() {
        if (status != GameStatus.PLAYING) return;

        // Check player / bot balls in holes first (immediate loss)
        for (Hole h : holes) {
            if (h.contains(playerBall)) {
                status = GameStatus.PLAYER_DEAD;
                return;
            }
            if (h.contains(botBall)) {
                status = GameStatus.BOT_DEAD;
                return;
            }
        }

        // Check small balls in holes
        List<Ball> toRemove = new ArrayList<>();
        for (Ball b : smallBalls) {
            for (Hole h : holes) {
                if (h.contains(b)) {
                    toRemove.add(b);
                    switch (b.getLastHitBy()) {
                        case PLAYER -> playerScore++;
                        case BOT -> botScore++;
                        default -> { /* no score change */ }
                    }
                    break; // each ball can only enter one hole
                }
            }
        }
        smallBalls.removeAll(toRemove);

        // End of game when no small balls remain
        if (smallBalls.isEmpty()) {
            if (playerScore > botScore) status = GameStatus.PLAYER_WINS;
            else if (botScore > playerScore) status = GameStatus.BOT_WINS;
            else status = GameStatus.DRAW;
        }
    }

    /**
     * Returns the mutable list of small balls currently on the board.
     *
     * @return small-ball list
     */
    public List<Ball> getSmallBalls() {
        return smallBalls;
    }

    /**
     * Returns the player's main ball.
     *
     * @return player ball
     */
    public Ball getPlayerBall() {
        return playerBall;
    }

    /**
     * Returns the bot's main ball.
     *
     * @return bot ball
     */
    public Ball getBotBall() {
        return botBall;
    }

    /**
     * Returns all holes configured on the board.
     *
     * @return hole list
     */
    public List<Hole> getHoles() {
        return holes;
    }

    /**
     * Returns board boundaries used by movement and bounce logic.
     *
     * @return boundary rectangle
     */
    public Boundary getBounds() {
        return bounds;
    }

    /**
     * Returns current player score.
     *
     * @return player score
     */
    public int getPlayerScore() {
        return playerScore;
    }

    /**
     * Returns current bot score.
     *
     * @return bot score
     */
    public int getBotScore() {
        return botScore;
    }

    /**
     * Returns current game status.
     *
     * @return status enum value
     */
    public GameStatus getStatus() {
        return status;
    }
}
