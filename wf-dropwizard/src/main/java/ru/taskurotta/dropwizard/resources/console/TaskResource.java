package ru.taskurotta.dropwizard.resources.console;

import ru.taskurotta.transport.model.TaskContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * Resource for retrieving tasks
 * User: dimadin
 * Date: 23.05.13 15:29
 */
@Path("/console/task/{id}")
public class TaskResource extends BaseResource {

    @GET
    public Response getTask(@PathParam("id")String id) {

        try {
            TaskContainer taskContainer = consoleManager.getTask(UUID.fromString(id));
            logger.debug("Task getted by id[{}] is  [{}]", id, taskContainer);
            return Response.ok(taskContainer, MediaType.APPLICATION_JSON).build();
        } catch(Throwable e) {
            logger.error("Error at getting task by id["+id+"]", e);
            return Response.serverError().build();
        }

    }

}