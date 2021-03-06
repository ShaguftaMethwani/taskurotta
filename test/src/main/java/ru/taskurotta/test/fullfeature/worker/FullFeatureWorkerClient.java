package ru.taskurotta.test.fullfeature.worker;

import ru.taskurotta.annotation.AcceptFail;
import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.TaskConfig;

/**
 * Created by void 20.12.13 18:03
 */
@WorkerClient(worker = FullFeatureWorker.class)
public interface FullFeatureWorkerClient {

    Promise<Double> sqr(Promise<Double> a);

    Promise<Double> sqr(Promise<Double> a, Promise<?>... waitFor);

    Promise<Double> sqr(Promise<Double> a, TaskConfig options, Promise<?>... waitFor);

    @AcceptFail(type = IllegalArgumentException.class)
    Promise<Double> sqrt(Promise<Double> a, TaskConfig options);

    @AcceptFail(type = IllegalArgumentException.class)
    Promise<Double> sqrt(Promise<Double> a);
}
