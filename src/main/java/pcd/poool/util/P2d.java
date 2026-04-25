package pcd.poool.util;

public record P2d(double x, double y) {
    public P2d sum(V2d v) {
        return new P2d(x + v.x(), y + v.y());
    }

    @Override
    public String toString() {
        return "P2d(" + x + "," + y + ")";
    }
}
