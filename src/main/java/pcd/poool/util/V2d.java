package pcd.poool.util;

import java.util.Objects;

public final class V2d {
    private final double x;
    private final double y;

    public V2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

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

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (V2d) obj;
        return Double.doubleToLongBits(this.x) == Double.doubleToLongBits(that.x) &&
                Double.doubleToLongBits(this.y) == Double.doubleToLongBits(that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

}
