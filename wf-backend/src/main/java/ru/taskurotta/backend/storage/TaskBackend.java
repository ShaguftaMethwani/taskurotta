package ru.taskurotta.backend.storage;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.checkpoint.CheckpointServiceProvider;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:11 PM
 */
public interface TaskBackend extends CheckpointServiceProvider {

    public void startProcess(TaskContainer taskContainer);


    /**
     * All resolved promise arguments should be swapped to original value objects.
     * Task state should be changed to "process"
     *
     * @param taskId
     * @return
     */
    public TaskContainer getTaskToExecute(UUID taskId);


    /**
     * Return task as it was registered
     *
     * @param taskId
     * @return
     */
    public TaskContainer getTask(UUID taskId);


    /**
     * Create RELEASE_TIMEOUT checkpoint
     *
     * @param taskDecision
     */
    public void addDecision(DecisionContainer taskDecision);


    /**
     * Delete RELEASE_TIMEOUT checkpoint
     *
     * @param taskDecision
     */
    public void addDecisionCommit(DecisionContainer taskDecision);



    public List<TaskContainer> getAllRunProcesses();


    /**
     * Return all decisions for particular process in the right chronological order.
     *
     * @param processId
     * @return
     */
    public List<DecisionContainer> getAllTaskDecisions(UUID processId);

    /**
     * @param actorId return only tasks for this actorId
     * @param timeFrom return only tasks with processing start time greater then given
     * @param timeTill return only tasks with processing start time less then given
     * @return list of tasks wich are currently processing by some remote actors
     */
    //public List<TaskDefinition> getActiveTasks(String actorId, long timeFrom, long timeTill);

    /**
     * Removes this task definitions from list of active tasks
     * @param tasks
     * @return number of removed tasks
     */
    //public int resetActiveTasks(List<TaskDefinition> tasks);

}
