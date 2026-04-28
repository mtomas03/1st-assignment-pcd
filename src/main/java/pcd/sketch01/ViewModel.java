package pcd.sketch01;

import java.util.ArrayList;
import java.util.Objects;

final class BallViewInfo {
    private final P2d pos;
    private final double radius;

    BallViewInfo(P2d pos, double radius) {
        this.pos = pos;
        this.radius = radius;
    }

    public P2d pos() {
        return pos;
    }

    public double radius() {
        return radius;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BallViewInfo) obj;
        return Objects.equals(this.pos, that.pos) &&
                Double.doubleToLongBits(this.radius) == Double.doubleToLongBits(that.radius);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, radius);
    }

    @Override
    public String toString() {
        return "BallViewInfo[" +
                "pos=" + pos + ", " +
                "radius=" + radius + ']';
    }

}

public class ViewModel {

    private ArrayList<BallViewInfo> balls;
    private BallViewInfo player;
    private int framePerSec;

    public ViewModel() {
        balls = new ArrayList<BallViewInfo>();
        framePerSec = 0;
    }

    public synchronized void update(Board board, int framePerSec) {
        balls.clear();
        for (var b : board.getBalls()) {
            balls.add(new BallViewInfo(b.getPos(), b.getRadius()));
        }
        this.framePerSec = framePerSec;
        var p = board.getPlayerBall();
        player = new BallViewInfo(p.getPos(), p.getRadius());
    }

    public synchronized ArrayList<BallViewInfo> getBalls() {
        var copy = new ArrayList<BallViewInfo>();
        copy.addAll(balls);
        return copy;

    }

    public synchronized int getFramePerSec() {
        return framePerSec;
    }

    public synchronized BallViewInfo getPlayerBall() {
        return player;
    }

}
