package ru.taskurotta.service.storage;

import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:11 PM
 */
public interface TaskService {

    public void startProcess(TaskContainer taskContainer);


    /**
     * All resolved promise arguments should be swapped to original value objects.
     *
     * @param taskId - ID of the task
     * @return TaskContainer with the task
     */
    public TaskContainer getTaskToExecute(UUID taskId, UUID processId, boolean simulate);


    /**
     * Return task as it was registered
     *
     * @param taskId - ID of the task
     * @return TaskContainer with the task
     */
    public TaskContainer getTask(UUID taskId, UUID processId);


    public boolean finishTask(DecisionContainer taskDecision);

    public boolean retryTask(UUID taskId, UUID processId, long timeToStart);

    public boolean restartTask(UUID taskId, UUID processId, long timeToStart, boolean force);

    /**
     * Idempotent getter for task decisions
     */
    public DecisionContainer getDecision(UUID taskId, UUID processId);

    public List<TaskContainer> getAllRunProcesses();


    /**
     * Return all decisions for particular process in the right chronological order.
     *
     * @param processId - ID of the process
     * @return List of DecisionContainer with all decisions for particular process in the right chronological order.
     */
    public List<DecisionContainer> getAllTaskDecisions(UUID processId);

    /**
     * Clean up resources after process.
     * Service should avoid synchronous removing artifacts due to performance issues.
     *
     * @param processId - ID of the process
     * @param finishedTaskIds - all task UUIDs of finished process
     */
    public void finishProcess(UUID processId, Collection<UUID> finishedTaskIds);

    public void updateTaskDecision(DecisionContainer taskDecision);
}
