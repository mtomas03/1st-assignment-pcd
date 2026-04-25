package pcd.poool.model.board.configuration;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.board.Hole;
import pcd.poool.model.board.Boundary;

import java.util.List;

public interface BoardConf {
    Boundary getBoundary();

    Ball getPlayerBall();

    Ball getBotBall();

    List<Ball> getSmallBalls();

    List<Hole> getHoles();
}
