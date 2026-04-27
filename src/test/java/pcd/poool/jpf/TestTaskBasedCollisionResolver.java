package pcd.poool.jpf;

import pcd.poool.model.ball.Ball;
import pcd.poool.model.collision.resolver.TaskBasedCollisionResolver;
import pcd.poool.model.collision.resolver.UniformGridSerialCollisionResolver;
import pcd.poool.util.P2d;
import pcd.poool.util.V2d;

import java.util.ArrayList;
import java.util.List;

public class TestTaskBasedCollisionResolver {
    private static final double EPS = 1.0e-6;

    public static void main(String[] args) throws InterruptedException {
        List<Ball> initial = initBallsPositions();
        List<Ball> serialResult = copyBalls(initial);
        List<Ball> taskResult = copyBalls(initial);

        new UniformGridSerialCollisionResolver().resolve(serialResult);

        try (TaskBasedCollisionResolver resolver = new TaskBasedCollisionResolver(2)) {
            resolver.resolve(taskResult);
        }

        verifyStateIsFinite(taskResult);
        verifyEquivalentToSerial(serialResult, taskResult);
    }

    private static List<Ball> initBallsPositions() {
        List<Ball> balls = new ArrayList<>();
        balls.add(new Ball(new P2d(0.40, 0.50), 0.05, 1.0, new V2d(0.12, 0.00)));
        balls.add(new Ball(new P2d(0.47, 0.50), 0.05, 1.0, new V2d(0.00, 0.00)));
        balls.add(new Ball(new P2d(0.54, 0.50), 0.05, 1.0, new V2d(-0.02, 0.00)));
        balls.add(new Ball(new P2d(0.61, 0.50), 0.05, 1.0, new V2d(0.00, 0.01)));
        return balls;
    }

    private static List<Ball> copyBalls(List<Ball> source) {
        List<Ball> copy = new ArrayList<>(source.size());
        for (Ball ball : source) {
            Ball cloned = new Ball(
                    new P2d(ball.getPos().x(), ball.getPos().y()),
                    ball.getRadius(),
                    ball.getMass(),
                    new V2d(ball.getVel().x(), ball.getVel().y())
            );
            cloned.setLastHitBy(ball.getLastHitBy());
            copy.add(cloned);
        }
        return copy;
    }

    private static void verifyStateIsFinite(List<Ball> balls) {
        if (balls.size() != 4) {
            throw new AssertionError("Unexpected ball count: " + balls.size());
        }
        for (Ball ball : balls) {
            if (!Double.isFinite(ball.getPos().x()) || !Double.isFinite(ball.getPos().y())) {
                throw new AssertionError("Non-finite position detected");
            }
            if (!Double.isFinite(ball.getVel().x()) || !Double.isFinite(ball.getVel().y())) {
                throw new AssertionError("Non-finite velocity detected");
            }
        }
    }

    private static void verifyEquivalentToSerial(List<Ball> expected, List<Ball> actual) {
        if (expected.size() != actual.size()) {
            throw new AssertionError("Mismatched list size");
        }
        for (int i = 0; i < expected.size(); i++) {
            Ball e = expected.get(i);
            Ball a = actual.get(i);
            assertClose(e.getPos().x(), a.getPos().x(), "pos.x", i);
            assertClose(e.getPos().y(), a.getPos().y(), "pos.y", i);
            assertClose(e.getVel().x(), a.getVel().x(), "vel.x", i);
            assertClose(e.getVel().y(), a.getVel().y(), "vel.y", i);
        }
    }

    private static void assertClose(double expected, double actual, String label, int index) {
        if (Math.abs(expected - actual) > EPS) {
            throw new AssertionError("Mismatch on " + label + " for ball " + index
                    + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
