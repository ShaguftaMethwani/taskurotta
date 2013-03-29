package ru.taskurotta.internal.core;

import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskTarget;

import java.util.Arrays;
import java.util.UUID;

/**
 * User: stukushin, romario
 * Date: 28.12.12
 * Time: 16:30
 */
public class TaskImpl implements Task {

    private UUID uuid;
    private TaskTarget taskTarget;
    private long startTime;
    private int numberOfAttempts = 0;
    private Object[] args;
	private TaskOptions taskOptions;


    public TaskImpl(UUID uuid, TaskTarget taskTarget, long startTime, int numberOfAttempts, Object[] args, TaskOptions taskOptions) {

        if (uuid == null) {
            throw new IllegalArgumentException("uuid can not be null!");
        }

        this.uuid = uuid;

        if (taskTarget == null) {
            throw new IllegalArgumentException("taskTarget can not be null!");
        }

        this.taskTarget = taskTarget;
        this.startTime = startTime;
        this.numberOfAttempts = numberOfAttempts;

        this.args = args;

		if (taskOptions == null) {
			this.taskOptions = new TaskOptions(null);
		} else {
			this.taskOptions = taskOptions;
		}
    }


    @Override
    public UUID getId() {
        return uuid;
    }


    @Override
    public TaskTarget getTarget() {
        return taskTarget;
    }


    @Override
    public Object[] getArgs() {
        return args;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public int getNumberOfAttempts() {
        return numberOfAttempts;
    }

    public TaskOptions getTaskOptions() {
		return taskOptions;
	}

	@Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof Task)) return false;

        Task that = (Task) o;

        if (!uuid.equals(that.getId())) return false;
        if (!taskTarget.equals(that.getTarget())) return false;

        Object[] thatArgs = that.getArgs();

        // if (args == null && thatArgs) we assume that it is empty
        if ((args == null && thatArgs != null)
                || (args != null && (thatArgs == null || !Arrays.deepEquals(args, thatArgs)))) return false;

		if (!taskOptions.equals(that.getTaskOptions()))
			return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + taskTarget.hashCode();
        result = 31 * result + Arrays.deepHashCode(args);
		result = 31 * result + taskOptions.hashCode();

        return result;
    }


	@Override
    public String toString() {
        return "TaskImpl{" +
                "uuid=" + uuid +
                ", taskTarget='" + taskTarget + '\'' +
                ", args=" + (args == null ? "null" : Arrays.toString(args)) +
                "}";
    }

}
