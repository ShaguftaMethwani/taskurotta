package ru.taskurotta.service.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.executor.Operation;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 24.12.13
 * Time: 12:29
 */
public class RecoveryOperation implements Operation {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryOperation.class);

    private UUID processId;

    private RecoveryProcessService recoveryProcessService;

    public RecoveryOperation(UUID processId) {
        this.processId = processId;
    }

    @Override
    public void init(Object nativePoint) {
        this.recoveryProcessService = (RecoveryProcessService) nativePoint;
    }

    @Override
    public void run() {

        try {
            recoveryProcessService.resurrect(processId);
        } catch (Throwable e) {
            logger.error("Error on recovery operation: " + processId, e);
        }
    }

    public UUID getProcessId() {
        return processId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecoveryOperation that = (RecoveryOperation) o;

        return !(processId != null ? !processId.equals(that.processId) : that.processId != null);
    }

    @Override
    public int hashCode() {
        return processId != null ? processId.hashCode() : 0;
    }
}