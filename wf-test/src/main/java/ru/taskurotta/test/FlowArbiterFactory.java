package ru.taskurotta.test;

/**
 * Created by void 04.04.13 13:49
 */
public class FlowArbiterFactory {
    private static FlowArbiter instance;

    public FlowArbiter getInstance() {
        synchronized (FlowArbiterFactory.class) {
            try {
                while (null == instance) {
                    FlowArbiterFactory.class.wait();
                }
            } catch (InterruptedException e) {
                // go out
            }
        }
        return instance;
    }

    public void setInstance(FlowArbiter instance) {
        synchronized (FlowArbiterFactory.class) {
            FlowArbiterFactory.instance = instance;
            FlowArbiterFactory.class.notifyAll();
        }
    }
}
