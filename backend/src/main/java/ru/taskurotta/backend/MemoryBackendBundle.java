package ru.taskurotta.backend;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.impl.MemoryConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.GeneralDependencyBackend;
import ru.taskurotta.backend.dependency.links.MemoryGraphDao;
import ru.taskurotta.backend.gc.MemoryGCBackend;
import ru.taskurotta.backend.process.BrokenProcessBackend;
import ru.taskurotta.backend.process.MemoryBrokenProcessBackend;
import ru.taskurotta.backend.queue.MemoryQueueBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.GeneralTaskBackend;
import ru.taskurotta.backend.storage.MemoryProcessBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.TaskDao;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:30 PM
 */
public class MemoryBackendBundle implements BackendBundle {

    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;
    private MemoryGraphDao memoryGraphDao;
    private BrokenProcessBackend brokenProcessBackend;
    private MemoryGCBackend gcBackend;

    public MemoryBackendBundle(int pollDelay, TaskDao taskDao) {
        this.processBackend = new MemoryProcessBackend();
        this.taskBackend = new GeneralTaskBackend(taskDao);
        this.queueBackend = new MemoryQueueBackend(pollDelay);
        this.memoryGraphDao = new MemoryGraphDao();
        this.dependencyBackend = new GeneralDependencyBackend(memoryGraphDao);
        this.configBackend = new MemoryConfigBackend();
        this.brokenProcessBackend = new MemoryBrokenProcessBackend();
        this.gcBackend = new MemoryGCBackend(processBackend, memoryGraphDao, taskDao);
    }

    @Override
    public ProcessBackend getProcessBackend() {
        return processBackend;
    }

    @Override
    public TaskBackend getTaskBackend() {
        return taskBackend;
    }

    @Override
    public QueueBackend getQueueBackend() {
        return queueBackend;
    }

    @Override
    public DependencyBackend getDependencyBackend() {
        return dependencyBackend;
    }

    @Override
    public ConfigBackend getConfigBackend() {
        return configBackend;
    }

    @Override
    public BrokenProcessBackend getBrokenProcessBackend() {
        return brokenProcessBackend;
    }

    public MemoryGraphDao getMemoryGraphDao() {
        return memoryGraphDao;
    }

    @Override
    public MemoryGCBackend getGCBackend() {
        return gcBackend;
    }
}
