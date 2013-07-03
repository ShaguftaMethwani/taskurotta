package ru.taskurotta.backend.console.manager;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.model.ProfileVO;
import ru.taskurotta.backend.console.model.QueueVO;
import ru.taskurotta.backend.console.model.QueuedTaskVO;
import ru.taskurotta.backend.console.model.TaskTreeVO;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * Manager interface, providing aggregated info gathered from concrete retrievers implementations
 * User: dimadin
 * Date: 17.05.13 16:03
 */
public interface ConsoleManager {

    /**
     * @return paginated view for current state of task queues
     */
    public GenericPage<QueueVO> getQueuesState(int pageNumber, int pageSise);

    /**
     * @return list of all existing TaskContainers for a given process
     */
    public List<TaskContainer> getProcessTasks(UUID processUuid);

    /**
     * @return paginated view of a given queue content
     */
    public GenericPage<QueuedTaskVO> getEnqueueTasks(String queueName, int pageNum, int pageSize);

    /**
     * @return TaskContainer for a given guid or null if task not found
     */
    public TaskContainer getTask(UUID taskId);

    /**
     * @return process representation object for a given guid or null if process not found
     */
    public ProcessVO getProcess(UUID processUuid);

    /**
     * @return list of method profiles marked by Profiled annotation
     */
    public List<ProfileVO> getProfilesInfo();

    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize);

    public GenericPage<ProcessVO> listProcesses(int pageNumber, int pageSize);

    public TaskTreeVO getTreeForTask(UUID taskUuid);

    public TaskTreeVO getTreeForProcess(UUID processUuid);

    public List<ProcessVO> findProcesses(String type, String id);

    public List<QueueVO> getQueuesHovering(float periodSize);

    public List<TaskContainer> getRepeatedTasks(int iterationCount);

}
