package pcd.poool.model.board;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.ball.BallCollision;
import pcd.poool.model.board.configuration.BoardConf;
import pcd.poool.model.collision.resolver.CollisionResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the full game state (balls, holes, scores, status)
 * and coordinates the physics-update step.
 *
 * <p>The board owns the game entities and is the only component that modifies
 * them during the game loop. The View accesses the state only through
 * immutable snapshots, so no synchronization on the board itself is needed.
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
     * The step first updates the position of all balls, then resolves all
     * collision phases, and finally evaluates hole interactions if enabled.
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

    private void updatePositions(long dt) {
        playerBall.updateState(dt, bounds);
        botBall.updateState(dt, bounds);
        for (Ball b : smallBalls) b.updateState(dt, bounds);
    }

    private void checkCollisions() throws InterruptedException {
        resolvePlayerBotCollision();
        collisionResolver.resolveBigBallCollisions(playerBall, Ball.HitBy.PLAYER, smallBalls);
        collisionResolver.resolveBigBallCollisions(botBall, Ball.HitBy.BOT, smallBalls);
        collisionResolver.resolve(smallBalls);
    }

    private void resolvePlayerBotCollision() {
        BallCollision.resolve(playerBall, botBall);
    }


    private void checkHoles() {
        if (status != GameStatus.PLAYING) return;

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

        List<Ball> toRemove = new ArrayList<>();
        for (Ball b : smallBalls) {
            for (Hole h : holes) {
                if (h.contains(b)) {
                    toRemove.add(b);
                    switch (b.getLastHitBy()) {
                        case PLAYER:
                            playerScore++;
                            break;
                        case BOT:
                            botScore++;
                            break;
                        default:
                            break;
                    }
                    break; // each ball can only enter one hole
                }
            }
        }
        smallBalls.removeAll(toRemove);

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
