package pcd.poool.view.info;

import pcd.poool.util.P2d;

/**
 * Immutable visual snapshot of a single ball.
 *
 * @param pos ball center in board coordinates
 * @param radius ball radius in board units
 */
public record BallInfo(P2d pos, double radius) {
}
