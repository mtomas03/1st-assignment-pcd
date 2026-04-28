package pcd.poool.util;

import java.util.Objects;

public final class P2d {
    private final double x;
    private final double y;

    public P2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public P2d sum(V2d v) {
        return new P2d(x + v.x(), y + v.y());
    }

    @Override
    public String toString() {
        return "P2d(" + x + "," + y + ")";
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
        var that = (P2d) obj;
        return Double.doubleToLongBits(this.x) == Double.doubleToLongBits(that.x) &&
                Double.doubleToLongBits(this.y) == Double.doubleToLongBits(that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

}
