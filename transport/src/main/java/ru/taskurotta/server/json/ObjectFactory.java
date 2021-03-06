package ru.taskurotta.server.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.Fail;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.RetryPolicyConfig;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskConfig;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.exception.SerializationException;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ArgContainer.ValueType;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 4:20 PM
 */
public class ObjectFactory {

    private static final Logger logger = LoggerFactory.getLogger(ObjectFactory.class);

    private ObjectMapper mapper;

    public ObjectFactory() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public Object parseArg(ArgContainer argContainer) {

        if (argContainer == null) {
            return null;
        }

        try {

            if (argContainer.isPromise()) {
                Promise promise = Promise.createInstance(argContainer.getTaskId());
                if (argContainer.containsError()) {
                    ErrorContainer errorContainer = argContainer.getErrorContainer();
                    promise.setFail(new Fail(errorContainer.getClassNames(), errorContainer.getMessage()));
                }
                if (argContainer.isReady()) {
                    promise.set(extractValue(argContainer));
                }
                logger.debug("ArgContainer[{}] parsed into promise[{}]", argContainer, promise);
                return promise;

            } else {//not a Promise, just some POJO or a primitive
                Object result = extractValue(argContainer);
                logger.debug("ArgContainer[{}] parsed into object[{}]", argContainer, result);
                return result;
            }

        } catch (Exception e) {
            throw new SerializationException("Cannot deserialize arg[" + argContainer + "]", e);
        }

    }


    private Object extractValue(ArgContainer arg) throws Exception {
        Object result = null;
        if (arg.isNull()) { //null object
            // just do nothing
        } else if (arg.isPlain()) { //simple object or primitive value
            result = getSimpleValue(arg.getJSONValue(), arg.getDataType());

        } else if (arg.isArray()) {//array of custom POJO objects or primitives
            result = getArrayValue(arg.getJSONValue(), arg.getDataType());

        } else if (arg.isCollection()) {//collection
            result = getCollectionValue(arg.getCompositeValue(), arg.getDataType());

        } else {
            throw new SerializationException("Unsupported or null value type for arg[" + arg + "]!");
        }
        return result;
    }

    private Object getSimpleValue(String json, String valueClass) throws ClassNotFoundException, IOException {
        Class loadedClass = Thread.currentThread().getContextClassLoader().loadClass(valueClass);
        return mapper.readValue(json, loadedClass);
    }

    private Object getArrayValue(String json, String arrayItemClassName) throws Exception {
        JsonNode node = mapper.readTree(json);

        Object array = ArrayFactory.newInstance(arrayItemClassName, node.size());
        Class<?> componentType = array.getClass().getComponentType();

        for (int i = 0; i < node.size(); i++) {
            Array.set(array, i, mapper.readValue(node.get(i).asText(), componentType));
        }
        return array;
    }

    private Object getCollectionValue(ArgContainer[] items, String collectionClassName) throws Exception {
        Class collectionClass = Thread.currentThread().getContextClassLoader().loadClass(collectionClassName);
        Collection result = (Collection) collectionClass.newInstance();

        for (ArgContainer item : items) {
            result.add(parseArg(item));
        }

        return result;
    }

    public ArgContainer dumpArg(Object arg) {

        ArgContainer result = new ArgContainer();

        try {
            if (arg instanceof Promise) {
                Promise pArg = (Promise) arg;
                result.setPromise(true);
                result.setReady(pArg.isReady());
                result.setTaskId(pArg.getId());
                if (pArg.isReady()) {
                    setArgContainerValue(result, pArg.get());
                }
            } else {
                result.setPromise(false);
                result.setReady(true);//just in case
                result.setTaskId(null);

                setArgContainerValue(result, arg);
            }

        } catch (Exception e) {
            throw new SerializationException("Cannot dump arg [" + arg + "]", e);
        }

        logger.debug("Object [{}] dumped into an ArgContainer[{}]", arg, result);
        return result;
    }

    private void setArgContainerValue(ArgContainer target, Object value) throws Exception {

        if (value == null) {
            target.setJSONValue(getPlainJson(value));
            target.setDataType(Object.class.getName());
            return;
        }

        ValueType type = SerializationUtils.extractValueType(value.getClass());
        target.setValueType(type);

        if (ValueType.PLAIN.equals(type)) {
            target.setJSONValue(getPlainJson(value));
            target.setDataType(value.getClass().getName());
        } else if (ValueType.ARRAY.equals(type)) {
            target.setJSONValue(getArrayJson(value));
            target.setDataType(value.getClass().getComponentType().getName());
        } else if (ValueType.COLLECTION.equals(type)) {
            target.setCompositeValue(parseCollectionValues(value));
            target.setDataType(value.getClass().getName());

        } else {
            throw new SerializationException("Cannot determine value type to set for object " + value);
        }
    }

    public Task parseTask(TaskContainer taskContainer) {

        if (taskContainer == null) {
            return null;
        }

        UUID processId = taskContainer.getProcessId();
        UUID taskId = taskContainer.getTaskId();
        ActorDefinition actorDef = ActorUtils.getActorDefinition(taskContainer.getActorId());

        TaskType taskType = taskContainer.getType();
        if (TaskType.WORKER_SCHEDULED.equals(taskContainer.getType())) {
            taskType = TaskType.WORKER;//scheduled worker is worker actor
        }
        TaskTarget taskTarget = new TaskTargetImpl(taskType, actorDef.getName(), actorDef.getVersion(), taskContainer.getMethod());
        Object[] args = null;

        ArgContainer[] argContainers = taskContainer.getArgs();

        if (argContainers != null) {
            args = new Object[argContainers.length];

            try {
                int i = 0;
                for (ArgContainer argContainer : argContainers) {
                    args[i++] = parseArg(argContainer);
                }

            } catch (SerializationException ex) {
                ex.setTaskId(taskId);
                ex.setProcessId(processId);
                ex.setPass(taskContainer.getPass());
                throw ex;
            }
        }

        TaskOptions opts = parseTaskOptions(taskContainer.getOptions());//TODO: may be parsing options is redundant? Client's ActorExecutor doesn't use them anyway

        return new TaskImpl(taskId, processId, taskContainer.getPass(), taskTarget, taskContainer.getStartTime(),
                taskContainer.getErrorAttempts(), args, opts, taskContainer.isUnsafe(), taskContainer.getFailTypes());
    }


    public TaskOptions parseTaskOptions(TaskOptionsContainer options) {
        if (options == null) {
            return null;
        }
        return new TaskOptions()
                .setArgTypes(options.getArgTypes())
                .setPromisesWaitFor(parsePromisesWaitFor(options.getPromisesWaitFor()))
                .setTaskConfig(parseSchedulingOptions(options.getTaskConfigContainer()));
    }

    public TaskConfig parseSchedulingOptions(TaskConfigContainer actorSchedOptions) {
        if (actorSchedOptions == null) {
            return null;
        }

        return new TaskConfig()
                .setCustomId(actorSchedOptions.getCustomId())
                .setStartTime(actorSchedOptions.getStartTime())
                .setTaskList(actorSchedOptions.getTaskList())
                .setTimeout(actorSchedOptions.getTimeout());
    }

    public Promise<?>[] parsePromisesWaitFor(ArgContainer[] args) {
        if (args == null) {
            return null;
        }

        Promise<?>[] result = new Promise<?>[args.length];
        int i = 0;
        for (ArgContainer arg : args) {
            result[i++] = (Promise) parseArg(arg); //TODO: method may return plain object. But waitForPromises args array should contain only promises
        }

        return result;
    }

    public TaskContainer dumpTask(Task task) {
        UUID taskId = task.getId();
        UUID processId = task.getProcessId();
        TaskTarget target = task.getTarget();
        ArgContainer[] argContainers = null;

        Object[] args = task.getArgs();

        if (args != null) {
            argContainers = new ArgContainer[args.length];

            int i = 0;
            for (Object arg : args) {
                argContainers[i++] = dumpArg(arg);
            }
        }

        TaskOptionsContainer taskOptionsContainer = dumpTaskOptions(task.getTaskOptions());

        return new TaskContainer(taskId, processId, task.getPass(), target.getMethod(), ActorUtils.getActorId(target), target
                .getType(), task.getStartTime(), task.getErrorAttempts(), argContainers, taskOptionsContainer,
                task.isUnsafe(), task.getFailTypes());
    }


    public TaskOptionsContainer dumpTaskOptions(TaskOptions taskOptions) {

        if (taskOptions == null) {
            return null;
        }

        TaskConfigContainer optionsContainer = null;
        TaskConfig taskConfig = taskOptions.getTaskConfig();
        if (taskConfig != null) {
            optionsContainer = new TaskConfigContainer();
            optionsContainer.setStartTime(taskConfig.getStartTime());
            optionsContainer.setCustomId(taskConfig.getCustomId());
            optionsContainer.setTaskList(taskConfig.getTaskList());
            optionsContainer.setTimeout(taskConfig.getTimeout());
            RetryPolicyConfig rp = taskConfig.getRetryPolicyConfig();
            if (rp != null) {
                optionsContainer.setRetryPolicyConfigContainer(new RetryPolicyConfigContainer(
                        rp.getType(),
                        rp.getInitialRetryIntervalSeconds(),
                        rp.getMaximumRetryIntervalSeconds(),
                        rp.getRetryExpirationIntervalSeconds(),
                        rp.getBackoffCoefficient(),
                        rp.getMaximumAttempts(),
                        rp.getExceptionsToRetry(),
                        rp.getExceptionsToExclude()));
            }
        }

        ArgContainer promisesWaitForDumped[] = null;
        Promise<?>[] promisesWaitFor = taskOptions.getPromisesWaitFor();
        if (promisesWaitFor != null) {
            promisesWaitForDumped = new ArgContainer[promisesWaitFor.length];
            for (int i = 0; i < promisesWaitFor.length; i++) {
                promisesWaitForDumped[i] = dumpArg(promisesWaitFor[i]);
            }
        }

        return new TaskOptionsContainer(taskOptions.getArgTypes(), optionsContainer, promisesWaitForDumped);
    }


    public DecisionContainer dumpResult(TaskDecision taskDecision, String actorId) {
        UUID taskId = taskDecision.getId();
        UUID processId = taskDecision.getProcessId();

        try {

            ArgContainer value = dumpArg(taskDecision.getValue());
            ErrorContainer errorContainer = dumpError(taskDecision.getException());
            TaskContainer[] taskContainers = null;

            Task[] tasks = taskDecision.getTasks();

            if (tasks != null) {
                taskContainers = new TaskContainer[tasks.length];

                int i = 0;
                for (Task task : tasks) {
                    taskContainers[i++] = dumpTask(task);
                }

            }

            DecisionContainer result = new DecisionContainer(taskId, processId, taskDecision.getPass(), value,
                    errorContainer, taskDecision.getRestartTime(), taskContainers, actorId,
                    taskDecision.getExecutionTime());

            logger.debug("DECISION: dumpResult for taskDecision[{}] is [{}]", taskDecision, result);
            return result;

        } catch (SerializationException ex) {
            ex.setTaskId(taskId);
            ex.setProcessId(processId);
            ex.setPass(taskDecision.getPass());
            throw ex;
        }
    }

    public ErrorContainer dumpError(Throwable e) {
        if (e == null) {
            return null;
        }

        return new ErrorContainer(e);
    }

    public String getPlainJson(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    private String getArrayJson(Object array) throws JsonProcessingException {
        ArrayNode arrayNode = mapper.createArrayNode();
        if (array != null) {

            int size = Array.getLength(array);
            for (int i = 0; i < size; i++) {
                String itemValue = mapper.writeValueAsString(Array.get(array, i));
                arrayNode.add(itemValue);
            }
        }

        return arrayNode.toString();
    }

    private ArgContainer[] parseCollectionValues(Object collection) {
        List<ArgContainer> result = new ArrayList<ArgContainer>();
        for (Object o : ((Collection) collection)) {
            result.add(dumpArg(o));
        }
        return result.toArray(new ArgContainer[result.size()]);
    }

}
