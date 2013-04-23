package ru.taskurotta.fir.client.workers;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;

/**
 * User: stukushin
 * Date: 23.04.13
 * Time: 11:01
 */

@WorkerClient(worker = FirWorker.class)
public interface FirWorkerClient {
    public void request(int a, int b);
    public Promise<Integer> response();
}
