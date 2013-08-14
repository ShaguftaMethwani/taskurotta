package ru.taskurotta.restarter.workers;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.hz.dependency.StartProcessTask;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.restarter.ProcessVO;
import ru.taskurotta.transport.model.ActorSchedulingOptionsContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 17:38
 */
public class RestarterImpl implements Restarter {

    private static Logger logger = LoggerFactory.getLogger(RestarterImpl.class);

    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private TaskDao taskDao;
    private ProcessBackend processBackend;

    private HazelcastInstance hzInstance;
    private String executorServiceName;

    @Override
    public void restart(List<ProcessVO> processes) {
        logger.info("Start restarting [{}] processes", processes);

        for (ProcessVO process : processes) {

            Collection<TaskContainer> taskContainers = findIncompleteTaskContainers(process);

            for (TaskContainer taskContainer : taskContainers) {

                if (taskContainer == null) {
                    continue;
                }

                String taskList = null;
                TaskOptionsContainer taskOptionsContainer = taskContainer.getOptions();
                if (taskOptionsContainer != null) {
                    ActorSchedulingOptionsContainer actorSchedulingOptionsContainer = taskOptionsContainer.getActorSchedulingOptions();
                    if (actorSchedulingOptionsContainer != null) {
                        taskList = actorSchedulingOptionsContainer.getTaskList();
                    }
                }

                logger.debug("Add task container [{}] to queue backend", taskContainer);

                queueBackend.enqueueItem(taskContainer.getActorId(), taskContainer.getTaskId(), taskContainer.getProcessId(), taskContainer.getStartTime(), taskList);
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Finish restarting [{}] processes", processes.size());
        }
    }

    private Collection<TaskContainer> findIncompleteTaskContainers(ProcessVO process) {

        UUID processId = process.getId();

        Graph graph = dependencyBackend.getGraph(processId);
        if (graph == null) {
            logger.warn("For processId [{}] not found graph", processId);

            TaskContainer startTaskContainer = restartProcess(processId);

            return Arrays.asList(startTaskContainer);
        }

        Set<UUID> notFinishedTaskIds = graph.getNotFinishedItems();
        if (logger.isDebugEnabled()) {
            logger.debug("For processId [{}] found [{}] not finished taskIds", processId, notFinishedTaskIds.size());
        }

        Collection<TaskContainer> taskContainers = new ArrayList<>(notFinishedTaskIds.size());
        for (UUID taskId : notFinishedTaskIds) {

            TaskContainer taskContainer = taskDao.getTask(taskId, processId);

            if (taskContainer == null) {
                logger.warn("Not found task container [{}] in task repository", taskId);

                TaskContainer startTaskContainer = restartProcess(processId);

                return Arrays.asList(startTaskContainer);
            }

            logger.debug("Found not finished task container [{}]", taskId, taskContainer);
            taskContainers.add(taskContainer);
        }

        if (logger.isInfoEnabled()) {
            logger.info("For processId [{}] found [{}] not finished task containers", processId, taskContainers.size());
        }

        return taskContainers;
    }

    private TaskContainer restartProcess(UUID processId) {
        TaskContainer startTaskContainer = processBackend.getStartTask(processId);
        logger.info("For processId [{}] get start task [{}]", processId, startTaskContainer);

        // emulate TaskServer.startProcess()
        taskDao.addTask(startTaskContainer);
        hzInstance.getExecutorService(executorServiceName).submit(new StartProcessTask(startTaskContainer));

        logger.info("Restart process [{}] from start task [{}]", startTaskContainer.getProcessId(), startTaskContainer);

        return startTaskContainer;
    }

    public void setQueueBackend(QueueBackend queueBackend) {
        this.queueBackend = queueBackend;
    }

    public void setDependencyBackend(DependencyBackend dependencyBackend) {
        this.dependencyBackend = dependencyBackend;
    }

    public void setTaskDao(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public void setProcessBackend(ProcessBackend processBackend) {
        this.processBackend = processBackend;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    public void setExecutorServiceName(String executorServiceName) {
        this.executorServiceName = executorServiceName;
    }
}