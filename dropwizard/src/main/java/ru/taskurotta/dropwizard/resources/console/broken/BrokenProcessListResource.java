package ru.taskurotta.dropwizard.resources.console.broken;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;
import ru.taskurotta.backend.process.BrokenProcessBackend;
import ru.taskurotta.backend.process.BrokenProcessVO;
import ru.taskurotta.backend.process.GroupCommand;
import ru.taskurotta.backend.process.ProcessGroupVO;
import ru.taskurotta.backend.process.SearchCommand;
import ru.taskurotta.backend.recovery.RecoveryProcessBackend;
import ru.taskurotta.dropwizard.resources.Action;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: dimadin
 * Date: 16.10.13 11:39
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/process/broken/{action}")
public class BrokenProcessListResource {

    private static final Logger logger = LoggerFactory.getLogger(BrokenProcessListResource.class);
    private static final String DATE_TEMPLATE = "dd.MM.yyyy";
    private static final String DATETIME_TEMPLATE = "dd.MM.yyyy HH:ss";


    private BrokenProcessBackend brokenProcessBackend;
    private RecoveryProcessBackend recoveryProcessBackend;

    private ExecutorService executorService = null;


    public BrokenProcessListResource(int recoveryThreads) {
        executorService = Executors.newFixedThreadPool(recoveryThreads);
    }

    @GET
    public Response getProcesses(@PathParam("action") String action, @QueryParam("dateFrom")Optional<String> dateFromOpt, @QueryParam("dateTo")Optional<String> dateToOpt,
                                 @QueryParam("starterId")Optional<String> starterIdOpt, @QueryParam("actorId")Optional<String> actorIdOpt, @QueryParam("exception")Optional<String> exceptionOpt,
                                 @QueryParam("group")Optional<String> groupOpt) {
        logger.debug("Getting processes by [{}]", action);
        if (Action.GROUP.getValue().equals(action)) {
            long startTime = System.currentTimeMillis();
            GroupCommand command = convertToCommand(starterIdOpt, actorIdOpt, exceptionOpt, dateFromOpt, dateToOpt, groupOpt);
            validateGroupCommand(command);
            Collection <ProcessGroupVO> groups =  getGroupList(command);
            logger.debug("Process groups count got by command [{}] are [{}], total time [{}]", command, (groups!=null?groups.size(): null), System.currentTimeMillis()-startTime);
            return Response.ok(groups, MediaType.APPLICATION_JSON).build();

        } else if (Action.LIST.getValue().equals(action)) {
            GroupCommand command = convertToCommand(starterIdOpt, actorIdOpt, exceptionOpt, dateFromOpt, dateToOpt, groupOpt);
            Collection<BrokenProcessVO> processes = brokenProcessBackend.find(command);
            logger.debug("Processes got by command [{}] are [{}]", command, processes);
            return Response.ok(processes, MediaType.APPLICATION_JSON).build();

        } else {
            logger.error("Unsupported action["+action+"] for GET method");
            return Response.serverError().build();

        }
    }

    public static class ActionCommand {
        protected String[] restartIds;

        public String[] getRestartIds() {
            return restartIds;
        }

        public void setRestartIds(String[] restartIds) {
            this.restartIds = restartIds;
        }

        @Override
        public String toString() {
            return "ActionCommand{" +
                    "restartIds=" + Arrays.toString(restartIds) +
                    "} ";
        }
    }

    @POST
    //TODO: action command should contain searchCommand to be passed as is to a recovery backend. Backend itself should delete recovered values from broken processes store
    public Response executeAction(@PathParam("action") String action, ActionCommand command) {
        logger.debug("Executing action [{}] with command [{}]", action, command);

        if (Action.RESTART.getValue().equals(action)) {
            Collection<UUID> processIds = null;

            if (command.getRestartIds()!=null && command.getRestartIds().length>0) {
                processIds = new ArrayList<>();
                for (String processId : command.getRestartIds()) {
                    processIds.add(UUID.fromString(processId));
                }
            }
            initiateRestart(processIds);
            logger.debug("Process group restarted [{}] of [{}]", (processIds!=null?processIds.size(): null));
            return Response.ok().build();

        } else {
            logger.error("Unsupported action["+action+"] for POST method");
            return Response.serverError().build();

        }
    }

    private void initiateRestart(final Collection<UUID> processIds) {
        if (processIds!=null && !processIds.isEmpty()) {

            executorService.submit(new Runnable() {

                @Override
                public void run() {
                    recoveryProcessBackend.restartProcessCollection(processIds);

                }
            });
//            Future<Integer> futureResult = executorService.submit(new Callable<Integer>() {
//                @Override
//                public Integer call() {
//                    int localResult = 0;
//                    for (UUID processId : processIds) {
//                        try {
//                            //TODO: should some transactions be applied here?
//                            recoveryProcessBackend.restartProcess(processId);
//                            brokenProcessBackend.delete(processId.toString());
//                            localResult++;
//                            logger.debug("Processed processId [{}]", processId);
//                        } catch (Throwable e) {
//                            logger.error("Error executing restart task for processes group", e);
//                        }
//                    }
//                    return localResult;
//                }
//            });
        }
    }

    private String getRequiredOptionalString(Optional<String> target, String errMessage) {
        String result = target.or("");
        if (!StringUtils.hasText(result)) {
            throw new WebApplicationException(new IllegalArgumentException(errMessage));
        }
        return result;
    }

    private void validateGroupCommand(GroupCommand command) {
        if (!StringUtils.hasText(command.getGroup())) {
            logger.error("Invalid group command got ["+command+"]");
            throw new WebApplicationException(new IllegalArgumentException("Group command cannot be empty"));
        }
        validateSearchCommand(command);
    }

    private void validateSearchCommand(SearchCommand command) {
        if (!StringUtils.hasText(command.getStartActorId())
                && !StringUtils.hasText(command.getBrokenActorId())
                && !StringUtils.hasText(command.getErrorClassName())
                && command.getStartPeriod() < 0
                && command.getEndPeriod() < 0) {
            logger.error("Invalid search command got ["+command+"]");
            throw new WebApplicationException(new IllegalArgumentException("Search command cannot be empty"));
        }
    }

    public static GroupCommand convertToCommand(Optional<String> starterIdOpt, Optional<String> actorIdOpt, Optional<String> exceptionOpt,
                                         Optional<String> dateFromOpt, Optional<String> dateToOpt, Optional<String> groupOpt) {
        String dateFrom = dateFromOpt.or("");
        String dateTo = dateToOpt.or("");
        GroupCommand result = new GroupCommand();

        if (StringUtils.hasText(dateFrom) || StringUtils.hasText(dateTo)) {
            SimpleDateFormat sdf = null;
            boolean withTime = false;
            if (dateFrom.length()>10 || dateTo.length()>10) {
                sdf = new SimpleDateFormat(DATETIME_TEMPLATE);
                withTime = true;
            } else {
                sdf = new SimpleDateFormat(DATE_TEMPLATE);
            }
            sdf.setLenient(false);

            try {
                if (StringUtils.hasText(dateFrom)) {
                    Date dateFromDate = sdf.parse(dateFrom);
                    result.setStartPeriod(dateFromDate.getTime());
                }
                if (StringUtils.hasText(dateTo)) {
                    Date dateToDate = sdf.parse(dateTo);
                    if (!withTime) {
                        dateToDate.setTime(dateToDate.getTime() + 24*60*60*1000);//+one day, cause default time 00:00 cuts off current day
                    }
                    result.setEndPeriod(dateToDate.getTime());
                }
            } catch (Exception e) {
                logger.error("Cannot parse date: from["+dateFrom+"], to["+dateTo+"]", e);
                throw new WebApplicationException(e);
            }
        }

        result.setStartActorId(starterIdOpt.or(""));
        result.setBrokenActorId(actorIdOpt.or(""));
        result.setErrorClassName(exceptionOpt.or(""));
        result.setGroup(groupOpt.or(GroupCommand.GROUP_STARTER));//default group processes by starter task

        logger.debug("Converted GroupCommand is [{}]", result);

        return result;
    }

    public List<ProcessGroupVO> getGroupList(GroupCommand command) {
        List<ProcessGroupVO> result = null;
        Collection<BrokenProcessVO> processes = brokenProcessBackend.find(command);

        if (processes != null && !processes.isEmpty()) {
            Map<String, Collection<BrokenProcessVO>> groupedProcesses = groupProcessList(processes, command.getGroup());
            result = convertToGroupsList(groupedProcesses, command);
        }

        return result;
    }


    private Map<String, Collection<BrokenProcessVO>> groupProcessList(Collection<BrokenProcessVO> processes, String groupType) {
        Map<String, Collection<BrokenProcessVO>> result = new HashMap<>();

        if (processes != null && !processes.isEmpty()) {

            if (GroupCommand.GROUP_STARTER.equals(groupType)) {
                for (BrokenProcessVO bp: processes) {
                    Collection<BrokenProcessVO> coll = result.get(bp.getStartActorId());
                    if (coll == null) {
                        coll = new ArrayList<>();
                    }
                    coll.add(bp);
                    result.put(bp.getStartActorId(), coll);
                }
            } else if (GroupCommand.GROUP_ACTOR.equals(groupType)) {
                for (BrokenProcessVO bp: processes) {
                    Collection<BrokenProcessVO> coll = result.get(bp.getBrokenActorId());
                    if (coll == null) {
                        coll = new ArrayList<>();
                    }
                    coll.add(bp);
                    result.put(bp.getBrokenActorId(), coll);
                }
            } else if (GroupCommand.GROUP_EXCEPTION.equals(groupType)) {
                for (BrokenProcessVO bp: processes) {
                    Collection<BrokenProcessVO> coll = result.get(bp.getErrorClassName());
                    if (coll == null) {
                        coll = new ArrayList<>();
                    }
                    coll.add(bp);
                    result.put(bp.getErrorClassName(), coll);
                }
            } else {
                logger.error("Unsupported groupType["+groupType+"]");
            }

        }

        return result;
    }

    private List<ProcessGroupVO> convertToGroupsList(Map<String, Collection<BrokenProcessVO>> groupedProcesses, GroupCommand command) {
        List<ProcessGroupVO> result = null;
        if (groupedProcesses!=null && !groupedProcesses.isEmpty()) {
            result = new ArrayList<>();
            for (Map.Entry<String, Collection<BrokenProcessVO>> entry: groupedProcesses.entrySet()) {
                Collection<BrokenProcessVO> groupItems = entry.getValue();
                ProcessGroupVO group = convertToGroup(groupItems, entry.getKey());
                result.add(group);
            }
        }
        return result;
    }

    private static ProcessGroupVO convertToGroup(Collection<BrokenProcessVO> members, String name) {
        ProcessGroupVO group= new ProcessGroupVO();
        Set<String> actorsDiffs = new HashSet<>();
        Set<String> startersDiffs = new HashSet<>();
        Set<String> exceptionsDiffs = new HashSet<>();
        Set<String> processIds = new HashSet<>();
        if (members!=null && !members.isEmpty()) {
            for(BrokenProcessVO bp: members) {
                actorsDiffs.add(bp.getBrokenActorId());
                startersDiffs.add(bp.getStartActorId());
                exceptionsDiffs.add(bp.getErrorClassName());
                processIds.add(bp.getProcessId().toString());
            }
        }
        group.setName(name);
        group.setStartersCount(startersDiffs.size());
        group.setActorsCount(actorsDiffs.size());
        group.setExceptionsCount(exceptionsDiffs.size());
        group.setTotal(processIds.size());
        group.setProcessIds(processIds);

        return group;
    }

    @Required
    public void setBrokenProcessBackend(BrokenProcessBackend brokenProcessBackend) {
        this.brokenProcessBackend = brokenProcessBackend;
    }

    @Required
    public void setRecoveryProcessBackend(RecoveryProcessBackend recoveryProcessBackend) {
        this.recoveryProcessBackend = recoveryProcessBackend;
    }
}
