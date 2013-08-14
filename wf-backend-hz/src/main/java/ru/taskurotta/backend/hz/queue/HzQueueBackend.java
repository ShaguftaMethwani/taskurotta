package ru.taskurotta.backend.hz.queue;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.Instance;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.hz.support.HzQueueSpringConfigSupport;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by void, dudin 07.06.13 11:00
 */
public class HzQueueBackend implements QueueBackend, QueueInfoRetriever {

    private final static Logger logger = LoggerFactory.getLogger(HzQueueBackend.class);

    private int pollDelay = 60;
    private TimeUnit pollDelayUnit = TimeUnit.SECONDS;
    private String queueNamePrefix;

    //Hazelcast specific
    private HazelcastInstance hazelcastInstance;

    private Map<String, IQueue<TaskQueueItem>> hzQueues = new ConcurrentHashMap<>();
    private Map<String, IMap<UUID, TaskQueueItem>> hzDelayedQueues = new ConcurrentHashMap<>();

    private HzQueueSpringConfigSupport hzQueueConfigSupport;

    public void setHzQueueConfigSupport(HzQueueSpringConfigSupport hzQueueConfigSupport) {
        this.hzQueueConfigSupport = hzQueueConfigSupport;
    }

    public HzQueueBackend(int pollDelay, TimeUnit pollDelayUnit, HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
        this.pollDelay = pollDelay;
        this.pollDelayUnit = pollDelayUnit;

        logger.debug("HzQueueBackend created and registered as Hazelcast instance listener");
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        List<String> queueNamesList = getTaskQueueNamesByPrefix();

        logger.debug("Stored queue names for queue backend are [{}]", queueNamesList);

        String[] queueNames = queueNamesList.toArray(new String[queueNamesList.size()]);
        List<String> result = new ArrayList<>(pageSize);
        if (queueNames.length > 0) {
            int pageStart = (pageNum - 1) * pageSize;
            int pageEnd = pageSize * pageNum >= queueNames.length ? queueNames.length : pageSize * pageNum;
            result.addAll(Arrays.asList(queueNames).subList(pageStart, pageEnd));
        }
        return new GenericPage<>(prefixStrip(result), pageNum, pageSize, queueNames.length);
    }

    private List<String> getTaskQueueNamesByPrefix() {
        List<String> result = new ArrayList<>();
        for(Instance inst: hazelcastInstance.getInstances()) {
            if(inst.getInstanceType().isQueue()) {
                String name = ((IQueue)inst).getName();
                if(name.startsWith(queueNamePrefix)) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    private List<String> prefixStrip(List<String> target) {
        if (queueNamePrefix == null) {
            return target;
        }
        List<String> result = null;
        if (target != null && !target.isEmpty()) {
            result = new ArrayList<>();
            for (String item : target) {
                result.add(item.substring(queueNamePrefix.length()));
            }
        }
        return result;
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        if (queueNamePrefix != null && !queueName.startsWith(queueNamePrefix)) {
            queueName = queueNamePrefix + queueName;
        }
        return hazelcastInstance.getQueue(queueName).size();
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        if (queueNamePrefix != null && !queueName.startsWith(queueNamePrefix)) {
            queueName = queueNamePrefix + queueName;
        }
        List<TaskQueueItem> result = new ArrayList<>();
        IQueue<TaskQueueItem> queue = hazelcastInstance.getQueue(queueName);
        TaskQueueItem[] queueItems = queue.toArray(new TaskQueueItem[queue.size()]);

        if (queueItems.length > 0) {
            int startIndex = (pageNum - 1) * pageSize;
            int endIndex = (pageSize * pageNum >= queueItems.length) ? queueItems.length : pageSize * pageNum;
            result.addAll(Arrays.asList(queueItems).subList(startIndex, endIndex));
        }
        return new GenericPage<>(result, pageNum, pageSize, queueItems.length);
    }

    @Override
    public TaskQueueItem poll(String actorId, String taskList) {
        IQueue<TaskQueueItem> queue = getHzQueue(createQueueName(actorId, taskList));

        TaskQueueItem result = null;
        try {
            result = queue.poll(pollDelay, pollDelayUnit);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted at poll, releasing it", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("poll() returns taskId [{}]. Queue.size: {}", result, queue.size());
        }

        return result;

    }

    /**
     * This is a cache proxy of hazelcastInstance.getQueue invocations
     *
     * @param queueName
     * @return
     */
    private IQueue<TaskQueueItem> getHzQueue(String queueName) {
        IQueue<TaskQueueItem> queue = hzQueues.get(queueName);

        if (queue != null) {
            return queue;
        }

        synchronized (hzQueues) {
            queue = hzQueues.get(queueName);
            if(queue!=null) {
                return queue;
            }
            if(hzQueueConfigSupport!=null) {
                hzQueueConfigSupport.createQueueConfig(queueName);
            } else {
                logger.warn("WARNING: hzQueueConfigSupport implementation is not set to HzQueueBackend, queues are not persistent!");
            }

            queue = hazelcastInstance.getQueue(queueName);//never null
            hzQueues.put(queueName, queue);
        }

        return queue;
    }

    /**
     * This is a cache proxy of hazelcastInstance.getQueue invocations
     *
     * @param queueName
     * @return
     */
    private IMap<UUID, TaskQueueItem> getHzDelayedQueue(String queueName) {
        IMap<UUID, TaskQueueItem> map = hzDelayedQueues.get(queueName);

        if (map != null) {
            return map;
        }

        map = hazelcastInstance.getMap(queueName);

        if (map != null) {
            hzDelayedQueues.put(queueName, map);
        }

        return map;
    }

    @Override
    public void pollCommit(String actorId, UUID taskId, UUID processId) {
    }

    @Override
    public void enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {

        // set it to current time for precisely repeat
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }

        TaskQueueItem item = new TaskQueueItem();
        item.setTaskId(taskId);
        item.setProcessId(processId);
        item.setStartTime(startTime);
        item.setEnqueueTime(System.currentTimeMillis());
        item.setTaskList(taskList);

        if (item.getStartTime() <= item.getEnqueueTime()) {

            IQueue<TaskQueueItem> queue = getHzQueue(createQueueName(actorId, taskList));
            queue.add(item);

            if(logger.isDebugEnabled()) {
                logger.debug("enqueue item [actorId [{}], taskId [{}], startTime [{}]; Queue.size: {}]", actorId, taskId, startTime, queue.size());
            }

        } else {

            IMap<UUID, TaskQueueItem> map = getHzDelayedQueue(createQueueName(actorId, taskList));
            map.set(taskId, item, 0, TimeUnit.SECONDS);

            if(logger.isDebugEnabled()) {
                logger.debug("Add to waiting set item [actorId [{}], taskId [{}], startTime [{}]; Set.size: {}]", actorId, taskId, startTime, map.size());
            }

        }

    }

    @Override
    public CheckpointService getCheckpointService() {
        return null;
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        return null;
    }

    private String createQueueName(String actorId, String taskList) {

        if (queueNamePrefix == null) {
            if (taskList == null) {
                return actorId;
            }

            return actorId + "#" + taskList;

        } else {
            if (taskList == null) {
                return queueNamePrefix + actorId;
            }

            return queueNamePrefix + actorId + "#" + taskList;
        }

    }

    public void setQueueNamePrefix(String queueNamePrefix) {
        this.queueNamePrefix = queueNamePrefix;
    }

    public void updateDelayedTasks() {
        List<String> queueNamesList = getTaskQueueNamesByPrefix();

        if (logger.isDebugEnabled()) {
            logger.debug("Start update delayed tasks for queues: {}", queueNamesList);
        }

        for (String queueName : queueNamesList) {
            IMap<UUID, TaskQueueItem> waitingItems = hazelcastInstance.getMap(queueName);
            IQueue<TaskQueueItem> queue = hazelcastInstance.getQueue(queueName);

            EntryObject entryObject = new PredicateBuilder().getEntryObject();
            Predicate predicate = entryObject.get("startTime").lessThan(System.currentTimeMillis());
            Collection<TaskQueueItem> readyItems = waitingItems.values(predicate);

            if (logger.isDebugEnabled()) {
                logger.debug("{} ready items for queue [{}]", readyItems.size(), queueName);
            }

            for (TaskQueueItem next : readyItems) {
                waitingItems.remove(next.getTaskId());
                queue.add(next);
            }
        }
    }
}