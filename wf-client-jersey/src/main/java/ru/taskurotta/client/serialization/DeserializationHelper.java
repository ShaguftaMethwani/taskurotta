package ru.taskurotta.client.serialization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.ArgType;

import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.server.transport.ArgContainer;
import ru.taskurotta.server.transport.TaskOptionsContainer;

import com.fasterxml.jackson.databind.JsonNode;

public class DeserializationHelper implements Constants {
	
	private static final Logger logger = LoggerFactory.getLogger(DeserializationHelper.class);
		
	public static UUID extractId(JsonNode idNode, UUID defVal) {
		UUID result = defVal;
		if(idNode!=null && !idNode.isNull()) {
			result = UUID.fromString(idNode.textValue());
		} else {
			logger.debug("Cannot extract UUID from node [{}]", idNode);
		}
		return result;
	}
	
	public static TaskTarget extractTaskTarget(JsonNode targetNode, TaskTarget defVal) {
		TaskTarget result = defVal;
		if(targetNode!=null && !targetNode.isNull()) {

			String taskType = targetNode.get(TASK_TARGET_TYPE).textValue();
			TaskType tasktypeEnumVal = TaskType.valueOf(taskType);
			String taskMethod = targetNode.get(TASK_TARGET_METHOD).textValue();
			String taskName = targetNode.get(TASK_TARGET_NAME).textValue();
			String taskVersion = targetNode.get(TASK_TARGET_VERSION).textValue();
			
			result = new TaskTargetImpl(tasktypeEnumVal, taskName, taskVersion, taskMethod);
		} else {
			logger.debug("Cannot extract TaskTarget from node [{}]", targetNode);
		}
		return result;
	}
	
	public static ArgContainer[] extractArgs(JsonNode argsNode, ArgContainer[] defVal) {
		ArgContainer[] result = defVal;
		if(argsNode!=null && !argsNode.isNull() && argsNode.isArray()) {
			Iterator<JsonNode> argsIterator = argsNode.elements();
			List<ArgContainer> argumentsList = new ArrayList<ArgContainer>();
			while(argsIterator.hasNext()) {
				JsonNode arg = argsIterator.next();
				
				argumentsList.add(parseArgument(arg));
			}
			result = argumentsList.toArray(new ArgContainer[argumentsList.size()]);
		} else {
			logger.debug("Cannot extract task args from node[{}]", argsNode);
		}
		
		return result;
	}

	public static TaskOptionsContainer extractOptions(JsonNode optionsNode, TaskOptionsContainer defValue) {
		TaskOptionsContainer result = null;
		if (optionsNode != null && !optionsNode.isNull()) {
			JsonNode typesNode = optionsNode.get(OPTIONS_ARG_TYPES);
			if (typesNode != null && !typesNode.isNull() && typesNode.isArray()) {
				Iterator<JsonNode> typesIterator = typesNode.elements();
				List<ArgType> argTypes = new ArrayList<ArgType>(typesNode.size());
				while (typesIterator.hasNext()) {
					argTypes.add(ArgType.fromInt(typesIterator.next().intValue()));
				}
				result = new TaskOptionsContainer(argTypes.toArray(new ArgType[argTypes.size()]));
			}
		}
		return result;
	}
	
	public static ArgContainer parseArgument(JsonNode arg) {
		ArgContainer result = null;
		if(arg==null || arg.isNull()) {
			return result;
		} else {
			String className = arg.get(ARG_CLASSNAME).textValue();
			Boolean isPromise = arg.get(ARG_IS_PROMISE).booleanValue();
			UUID taskId = extractId(arg.get(ARG_TASK_ID), null);
			Boolean isReady = arg.get(ARG_IS_READY).booleanValue();
			String json = arg.get(ARG_JSON_VALUE).textValue();
			
			return new ArgContainer(className, isPromise, taskId, isReady, json);
		}
	}
		
}
