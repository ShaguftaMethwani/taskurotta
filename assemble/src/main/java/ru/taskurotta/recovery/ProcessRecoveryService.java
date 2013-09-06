package ru.taskurotta.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.recovery.RecoveryTask;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.TaskDao;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Scans process backend for incomplete processes and tries to recover them.
 * Only processes belonging to local member node are exposed to this service
 * User: stukushin, dudin
 * Date: 15.08.13
 * Time: 16:42
 */
public class ProcessRecoveryService {

    private static Logger logger = LoggerFactory.getLogger(ProcessRecoveryService.class);

    private DataSource dataSource;
    private HazelcastInstance hazelcastInstance;

    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private TaskDao taskDao;
    private ProcessBackend processBackend;
    private TaskBackend taskBackend;

    private long recoveryProcessTimeOut = 180000l;
    private int threadCount = 8;
    private boolean useDaemonThreads = true;
    private int processBatchSize = 1000;

    private ExecutorService executorService;

    @PostConstruct
    private void init() {
        executorService = Executors.newFixedThreadPool(threadCount, new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(useDaemonThreads);
                thread.setName("Recovery-" + counter++);
                return thread;
            }
        });
        int result = findAndSubmitRecovery();
        logger.info("[{}] processes submitted to recovery at startup", result);
    }

    public int findAndSubmitRecovery() {
        logger.debug("Try to find incomplete processes with [{}] batch size and [{}]threads", processBatchSize, threadCount);

        int result = 0;
        Collection <UUID> processIds = findIncompleteProcesses();

        if (processIds!=null && !processIds.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found [{}] incomplete processes(cluster wide) for recovery", processIds.size());
            }

            for(UUID processId: processIds) {
                if (isLocalItem(processId)) {//filter only local processes for recovery. Every node recovers its own processes
                    executorService.submit(new RecoveryTask(queueBackend, dependencyBackend, taskDao, processBackend, taskBackend, recoveryProcessTimeOut, processId));
                    result++;
                }
            }
        }

        logger.info("Found and submitted [{}] tasks for local processes recovery", result);

        return result;
    }

    private boolean isLocalItem(UUID id) {
       Partition partition = hazelcastInstance.getPartitionService().getPartition(id);
       return partition.getOwner().localMember();
    }

    private Collection<UUID> findIncompleteProcesses() {
        Collection<UUID> processIds = new ArrayList<>();

        long fromTime = System.currentTimeMillis();

        if (logger.isInfoEnabled()) {
            logger.info("Try to find incomplete processes, was started before [{} ({})]", fromTime, new Date(fromTime));
        }

        String query = "SELECT * FROM (SELECT process_id FROM process p WHERE state = ? AND start_time < ? ORDER BY start_time) WHERE ROWNUM <= ?";

        int incompleteProcessCount = 0;

        try (Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, 0);
            preparedStatement.setLong(2, fromTime);
            preparedStatement.setInt(3, processBatchSize);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                incompleteProcessCount++;

                UUID processId = UUID.fromString(resultSet.getString("process_id"));

                processIds.add(processId);

                logger.debug("Found incomplete processId [{}]", processId);
            }

        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        }

        logger.info("Found [{}] incomplete processes", incompleteProcessCount);

        return processIds;
    }

    public void setProcessBatchSize(int processBatchSize) {
        this.processBatchSize = processBatchSize;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    @Required
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Required
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Required
    public void setQueueBackend(QueueBackend queueBackend) {
        this.queueBackend = queueBackend;
    }

    @Required
    public void setDependencyBackend(DependencyBackend dependencyBackend) {
        this.dependencyBackend = dependencyBackend;
    }

    @Required
    public void setTaskDao(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    @Required
    public void setProcessBackend(ProcessBackend processBackend) {
        this.processBackend = processBackend;
    }

    @Required
    public void setTaskBackend(TaskBackend taskBackend) {
        this.taskBackend = taskBackend;
    }

    public void setRecoveryProcessTimeOut(long recoveryProcessTimeOut) {
        this.recoveryProcessTimeOut = recoveryProcessTimeOut;
    }

    public void setUseDaemonThreads(boolean useDaemonThreads) {
        this.useDaemonThreads = useDaemonThreads;
    }
}