package pcd.poool.model.board.configuration;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.board.Hole;
import pcd.poool.model.board.Boundary;
import pcd.poool.util.P2d;
import pcd.poool.util.V2d;

import java.util.ArrayList;
import java.util.List;

public class LargeBoardConf implements BoardConf {

    private static final Boundary BOUNDS = new Boundary(-1.5, -1.0, 1.5, 1.0);
    private static final double PLAYER_RADIUS = 0.05;
    private static final double PLAYER_MASS = 1.5;
    private static final double HOLE_RADIUS = 0.15;

    @Override
    public Boundary getBoundary() {
        return BOUNDS;
    }

    @Override
    public Ball getPlayerBall() {
        return new Ball(new P2d(-0.8, -0.75), PLAYER_RADIUS, PLAYER_MASS, new V2d(0, 0));
    }

    @Override
    public Ball getBotBall() {
        return new Ball(new P2d(0.8, -0.75), PLAYER_RADIUS, PLAYER_MASS, new V2d(0, 0));
    }

    @Override
    public List<Ball> getSmallBalls() {
        var balls = new ArrayList<Ball>(400);
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 20; col++) {
                double px = -0.25 + col * 0.025;
                double py = row * 0.025;
                balls.add(new Ball(new P2d(px, py), 0.01, 0.25, new V2d(0, 0)));
            }
        }
        return balls;
    }

    @Override
    public List<Hole> getHoles() {
        return List.of(
                new Hole(new P2d(BOUNDS.x0(), BOUNDS.y1()), HOLE_RADIUS),
                new Hole(new P2d(BOUNDS.x1(), BOUNDS.y1()), HOLE_RADIUS)
        );
    }
}
