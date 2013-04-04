package ru.taskurotta.backend.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.core.TaskType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 9:34 PM
 */
public class MemoryTaskBackend implements TaskBackend {

    private final static Logger logger = LoggerFactory.getLogger(MemoryTaskBackend.class);

    private Map<UUID, TaskContainer> id2TaskMap = new ConcurrentHashMap<UUID, TaskContainer>();
    private Map<UUID, DecisionContainer> id2TaskDecisionMap = new ConcurrentHashMap<UUID, DecisionContainer>();
    private Map<UUID, Boolean> id2ProgressMap = new ConcurrentHashMap<UUID, Boolean>();

    @Override
    public void startProcess(TaskContainer taskContainer) {
        id2TaskMap.put(taskContainer.getTaskId(), taskContainer);
    }

    @Override
    public TaskContainer getTaskToExecute(UUID taskId) {

        logger.debug("getTaskToExecute() taskId = [{}]", taskId);

        TaskContainer task = getTask(taskId);

        ArgContainer[] args = task.getArgs();

        if (args != null) {

            for (int i = 0; i < args.length; i++) {
                ArgContainer arg = args[i];
                if (arg.isPromise()) {
                    if (!TaskType.DECIDER_ASYNCHRONOUS.equals(task.getTarget().getType())) {
                        ArgContainer value = getTaskValue(arg.getTaskId());
                        args[i] = value;
                    } else {
                        if (arg.getJSONValue() == null) {
                            // resolved Promise. value may be null for NoWait promises

                            ArgContainer value = getTaskValue(arg.getTaskId());
                            if (value != null) {
                                arg.setJSONValue(value.getJSONValue());
                                arg.setClassName(value.getClassName());
                                arg.setReady(true);
                            }
                        }
                    }
                }
            }

        }

        id2ProgressMap.put(taskId, true);

        return task;
    }


    private ArgContainer getTaskValue(UUID taskId) {

        logger.debug("getTaskValue() taskId = [{}]", taskId);

        DecisionContainer taskDecision = id2TaskDecisionMap.get(taskId);

        if (taskDecision == null) {
            return null;
        }

        ArgContainer argContainer = taskDecision.getValue();

        if (argContainer == null) {
            return null;
        }

        if (!argContainer.isPromise()) {
            return argContainer;
        }

        if (argContainer.isPromise() && !argContainer.isReady()) {
            return getTaskValue(argContainer.getTaskId());
        }

        return argContainer;
    }


    @Override
    public TaskContainer getTask(UUID taskId) {
        return id2TaskMap.get(taskId);
    }

    @Override
    public void addError(UUID taskId, ErrorContainer asyncTaskError, boolean shouldBeRestarted) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addDecision(DecisionContainer taskDecision) {

        logger.debug("addDecision() taskDecision = [{}]", taskDecision);

        UUID taskId = taskDecision.getTaskId();

        id2ProgressMap.remove(taskId);
        id2TaskDecisionMap.put(taskId, taskDecision);

        TaskContainer[] taskContainers = taskDecision.getTasks();
        if (taskContainers == null) {
            return;
        }

        for (TaskContainer taskContainer : taskContainers) {
            id2TaskMap.put(taskContainer.getTaskId(), taskContainer);
        }
    }

    @Override
    public void addDecisionCommit(UUID taskId) {
    }

    @Override
    public void addErrorCommit(UUID taskId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<TaskContainer> getAllRunProcesses() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<DecisionContainer> getAllTaskDecisions(UUID processId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isTaskInProgress(UUID taskId) {
        return id2ProgressMap.containsKey(taskId);
    }

    public boolean isTaskReleased(UUID taskId) {
        return id2TaskDecisionMap.containsKey(taskId);
    }
}