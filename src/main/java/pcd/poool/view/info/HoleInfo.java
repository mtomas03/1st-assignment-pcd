package pcd.poool.view.info;

import pcd.poool.util.P2d;

/**
 * Immutable visual snapshot of a board hole.
 *
 * @param center hole center in board coordinates
 * @param radius hole radius in board units
 */
public record HoleInfo(P2d center, double radius) {
}
