package ru.taskurotta.backend.ora;

import junit.framework.Assert;
import org.junit.Test;
import ru.taskurotta.backend.ora.dao.DbConnect;
import ru.taskurotta.backend.ora.storage.OraTaskDao;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;

/**
 * User: moroz
 * Date: 10.04.13
 */
public class OraTaskDaoTest {

    private DbConnect connection = new DbConnect();
    private OraTaskDao dao = new OraTaskDao(connection);

    @Test
    public void addReadTaskTest() {
        TaskContainer task = SerializationTest.createTaskContainer();
        dao.addTask(task);

        TaskContainer task1 = dao.getTask(task.getTaskId());
        Assert.assertEquals(task.getStartTime(), task1.getStartTime());
        Assert.assertEquals(task.getArgs().length, task1.getArgs().length);

    }

    @Test
    public void addReadDecisionTest() {
        DecisionContainer task = SerializationTest.createDecisionContainer(false);
        dao.addDecision(task);

        DecisionContainer task1 = dao.getDecision(task.getTaskId());
        Assert.assertEquals(task.getTaskId(), task1.getTaskId());
        Assert.assertEquals(task.containsError(), task1.containsError());

        Assert.assertTrue(dao.isTaskReleased(task.getTaskId()));

    }
}
