package pcd.poool.model.board;

/**
 * Immutable axis-aligned board boundary rectangle.
 *
 * @param x0 minimum x coordinate
 * @param y0 minimum y coordinate
 * @param x1 maximum x coordinate
 * @param y1 maximum y coordinate
 */
public record Boundary(double x0, double y0, double x1, double y1) {
}
