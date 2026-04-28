package pcd.sketch01;

import java.util.Objects;

public final class Boundary {
    private final double x0;
    private final double y0;
    private final double x1;
    private final double y1;

    public Boundary(double x0, double y0, double x1, double y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
    }

    public double x0() {
        return x0;
    }

    public double y0() {
        return y0;
    }

    public double x1() {
        return x1;
    }

    public double y1() {
        return y1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Boundary) obj;
        return Double.doubleToLongBits(this.x0) == Double.doubleToLongBits(that.x0) &&
                Double.doubleToLongBits(this.y0) == Double.doubleToLongBits(that.y0) &&
                Double.doubleToLongBits(this.x1) == Double.doubleToLongBits(that.x1) &&
                Double.doubleToLongBits(this.y1) == Double.doubleToLongBits(that.y1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x0, y0, x1, y1);
    }

    @Override
    public String toString() {
        return "Boundary[" +
                "x0=" + x0 + ", " +
                "y0=" + y0 + ", " +
                "x1=" + x1 + ", " +
                "y1=" + y1 + ']';
    }

}
