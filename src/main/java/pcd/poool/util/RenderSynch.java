package pcd.poool.util;

public class RenderSynch {

    private long nextFrameToRender = 0;
    private long lastFrameRendered = -1;

    public synchronized long nextFrameToRender() {
        return nextFrameToRender++;
    }

    public synchronized void notifyFrameRendered() {
        lastFrameRendered++;
        notifyAll();
    }

    public synchronized void waitForFrameRendered(long frame) throws InterruptedException {
        while (lastFrameRendered < frame) wait();
    }
}
