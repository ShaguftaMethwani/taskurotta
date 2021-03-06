package ru.taskurotta.recipes.retrypolicy.workers;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.TaskConfig;

/**
 * User: stukushin
 * Date: 11.04.13
 * Time: 20:04
 */
@WorkerClient(worker = SumWorker.class)
public interface SumWorkerClient {
    public Promise<Integer> sum(int a,int b);

    public Promise<Integer> sum(int a,int b, TaskConfig taskConfig);
}
