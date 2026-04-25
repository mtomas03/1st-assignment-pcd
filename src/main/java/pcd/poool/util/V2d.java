package pcd.poool.util;

public record V2d(double x, double y) {
    public V2d sum(V2d v) {
        return new V2d(x + v.x(), y + v.y());
    }

    public double abs() {
        return Math.sqrt(x * x + y * y);
    }

    public V2d normalized() {
        double m = abs();
        return m > 1e-9 ? new V2d(x / m, y / m) : new V2d(0, 0);
    }

    public V2d mul(double f) {
        return new V2d(x * f, y * f);
    }

    public V2d swapX() {
        return new V2d(-x, y);
    }

    public V2d swapY() {
        return new V2d(x, -y);
    }

    @Override
    public String toString() {
        return "V2d(" + x + "," + y + ")";
    }
}
