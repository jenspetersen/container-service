package org.nrg.containers.services.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.events.model.ContainerEvent;
import org.nrg.containers.events.model.ServiceTaskEvent;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoDockerServerException;
import org.nrg.containers.exceptions.UnauthorizedException;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.command.auto.ResolvedInputTreeNode;
import org.nrg.containers.model.command.auto.ResolvedInputValue;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.auto.Container.ContainerHistory;
import org.nrg.containers.model.container.auto.ServiceTask;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.model.xnat.XnatModelObject;
import org.nrg.containers.services.CommandResolutionService;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.containers.services.ContainerFinalizeService;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.utils.WorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.nrg.containers.model.command.entity.CommandType.DOCKER;
import static org.nrg.containers.model.command.entity.CommandType.DOCKER_SETUP;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.ASSESSOR;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.PROJECT;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.RESOURCE;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.SCAN;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.SESSION;
import static org.nrg.containers.model.command.entity.CommandWrapperInputType.SUBJECT;

@Service
public class ContainerServiceImpl implements ContainerService {
    private static final Logger log = LoggerFactory.getLogger(ContainerServiceImpl.class);
    private static final Pattern exitCodePattern = Pattern.compile("kill|die|oom\\((\\d+|x)\\)");

    private final ContainerControlApi containerControlApi;
    private final ContainerEntityService containerEntityService;
    private final CommandResolutionService commandResolutionService;
    private final AliasTokenService aliasTokenService;
    private final SiteConfigPreferences siteConfigPreferences;
    private final ContainerFinalizeService containerFinalizeService;

    @Autowired
    public ContainerServiceImpl(final ContainerControlApi containerControlApi,
                                final ContainerEntityService containerEntityService,
                                final CommandResolutionService commandResolutionService,
                                final AliasTokenService aliasTokenService,
                                final SiteConfigPreferences siteConfigPreferences,
                                final ContainerFinalizeService containerFinalizeService) {
        this.containerControlApi = containerControlApi;
        this.containerEntityService = containerEntityService;
        this.commandResolutionService = commandResolutionService;
        this.aliasTokenService = aliasTokenService;
        this.siteConfigPreferences = siteConfigPreferences;
        this.containerFinalizeService = containerFinalizeService;
    }

    @Override
    public List<Container> getAll() {
        return toPojo(containerEntityService.getAll());
    }

    @Override
    @Nullable
    public Container retrieve(final String containerId) {
        final ContainerEntity containerEntity = containerEntityService.retrieve(containerId);
        return containerEntity == null ? null : toPojo(containerEntity);
    }

    @Override
    @Nullable
    public Container retrieve(final long id) {
        final ContainerEntity containerEntity = containerEntityService.retrieve(id);
        return containerEntity == null ? null : toPojo(containerEntity);
    }

    @Override
    @Nonnull
    public Container get(final long id) throws NotFoundException {
        return toPojo(containerEntityService.get(id));
    }

    @Override
    @Nonnull
    public Container get(final String containerId) throws NotFoundException {
        return toPojo(containerEntityService.get(containerId));
    }

    @Override
    public void delete(final long id) throws NotFoundException {
        containerEntityService.delete(id);
    }

    @Override
    public void delete(final String containerId) throws NotFoundException {
        containerEntityService.delete(containerId);
    }

    @Override
    @Nonnull
    public List<Container> retrieveServices() {
        return toPojo(containerEntityService.retrieveServices());
    }

    @Override
    @Nonnull
    public List<Container> retrieveNonfinalizedServices() {
        return toPojo(containerEntityService.retrieveNonfinalizedServices());
    }

    @Override
    public List<Container> retrieveSetupContainersForParent(final long parentId) {
        return toPojo(containerEntityService.retrieveSetupContainersForParent(parentId));
    }

    @Override
    @Nullable
    public Container addContainerEventToHistory(final ContainerEvent containerEvent, final UserI userI) {
        final ContainerEntity containerEntity = containerEntityService.addContainerEventToHistory(containerEvent, userI);
        return containerEntity == null ? null : toPojo(containerEntity);
    }

    @Override
    @Nullable
    public ContainerHistory addContainerHistoryItem(final Container container, final ContainerHistory history, final UserI userI) {
        final ContainerEntityHistory containerEntityHistoryItem = containerEntityService.addContainerHistoryItem(fromPojo(container), fromPojo(history), userI);
        return containerEntityHistoryItem == null ? null : toPojo(containerEntityHistoryItem);
    }

    @Override
    @Nonnull
    public Container resolveCommandAndLaunchContainer(final long wrapperId,
                                                      final Map<String, String> inputValues,
                                                      final UserI userI)
            throws NoDockerServerException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(wrapperId, inputValues, userI), userI);
    }

    @Override
    @Nonnull
    public Container resolveCommandAndLaunchContainer(final long commandId,
                                                      final String wrapperName,
                                                      final Map<String, String> inputValues,
                                                      final UserI userI)
            throws NoDockerServerException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(commandId, wrapperName, inputValues, userI), userI);

    }

    @Override
    @Nonnull
    public Container resolveCommandAndLaunchContainer(final String project,
                                                      final long wrapperId,
                                                      final Map<String, String> inputValues,
                                                      final UserI userI)
            throws NoDockerServerException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(project, wrapperId, inputValues, userI), userI);
    }

    @Override
    @Nonnull
    public Container resolveCommandAndLaunchContainer(final String project,
                                                      final long commandId,
                                                      final String wrapperName,
                                                      final Map<String, String> inputValues,
                                                      final UserI userI)
            throws NoDockerServerException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(project, commandId, wrapperName, inputValues, userI), userI);

    }

    @Override
    @Nonnull
    public Container launchResolvedCommand(final ResolvedCommand resolvedCommand,
                                           final UserI userI)
            throws NoDockerServerException, DockerServerException, ContainerException, UnsupportedOperationException {
        return launchResolvedCommand(resolvedCommand, userI, null);
    }

    private Container launchResolvedCommand(final ResolvedCommand resolvedCommand,
                                            final UserI userI,
                                            final Container parent)
            throws NoDockerServerException, DockerServerException, ContainerException, UnsupportedOperationException {
        if (resolvedCommand.type().equals(DOCKER.getName()) || resolvedCommand.type().equals(DOCKER_SETUP.getName())) {
            return launchResolvedDockerCommand(resolvedCommand, userI, parent);
        } else {
            throw new UnsupportedOperationException("Cannot launch a command of type " + resolvedCommand.type());
        }
    }

    @Nonnull
    private Container launchResolvedDockerCommand(final ResolvedCommand resolvedCommand,
                                                  final UserI userI,
                                                  final Container parent)
            throws NoDockerServerException, DockerServerException, ContainerException {
        log.info("Preparing to launch resolved command.");
        final ResolvedCommand preparedToLaunch = prepareToLaunch(resolvedCommand, userI);

        log.info("Creating container from resolved command.");
        final Container createdContainerOrService = containerControlApi.createContainerOrSwarmService(preparedToLaunch, userI);

        log.info("Recording container launch.");
        final String workflowId = makeWorkflowIfAppropriate(resolvedCommand, createdContainerOrService, userI);
        final Container savedContainerOrService = toPojo(containerEntityService.save(fromPojo(
                createdContainerOrService.toBuilder()
                        .workflowId(workflowId)
                        .setParentProperties(parent)
                        .build()
        ), userI));

        if (resolvedCommand.setupCommands().size() > 0) {
            log.info("Launching setup containers.");
            for (final ResolvedCommand resolvedSetupCommand : resolvedCommand.setupCommands()) {
                launchResolvedCommand(resolvedSetupCommand, userI, savedContainerOrService);
            }
        } else {
            startContainer(userI, savedContainerOrService);
        }

        return savedContainerOrService;
    }

    private void startContainer(final UserI userI, final Container savedContainerOrService) throws NoDockerServerException, ContainerException {
        log.info("Starting container.");
        try {
            containerControlApi.startContainer(savedContainerOrService);
        } catch (DockerServerException e) {
            addContainerHistoryItem(savedContainerOrService, ContainerHistory.fromSystem("Failed", "Did not start." + e.getMessage()), userI);
            handleFailure(savedContainerOrService);
            throw new ContainerException("Failed to start");
        }
    }

    @Nonnull
    private ResolvedCommand prepareToLaunch(final ResolvedCommand resolvedCommand,
                                            final UserI userI) {
        final ResolvedCommand.Builder resolvedCommandBuilder =
                resolvedCommand.toBuilder();

        // Add default environment variables & create new alias token
        final AliasToken token = aliasTokenService.issueTokenForUser(userI);
        final String processingUrl = (String)siteConfigPreferences.getProperty("processingUrl", siteConfigPreferences.getSiteUrl());
        resolvedCommandBuilder.addEnvironmentVariable("XNAT_USER", token.getAlias())
                .addEnvironmentVariable("XNAT_PASS", token.getSecret())
                .addEnvironmentVariable("XNAT_HOST", processingUrl);

        return resolvedCommandBuilder.build();
    }




    @Override
    public void processEvent(final ContainerEvent event) {
        log.debug("Processing container event");
        final Container container = retrieve(event.containerId());



        // container will be null if either we aren't tracking the container
        // that this event is about, or if we have already recorded the event
        if (container != null ) {
            final String userLogin = container.userId();
            try {
                final UserI userI = Users.getUser(userLogin);

                final Container containerWithAddedEvent = addContainerEventToHistory(event, userI);
                if (event.isExitStatus()) {
                    log.debug("Container is dead. Finalizing.");
                    finalize(containerWithAddedEvent, userI, event.exitCode());
                }
            } catch (UserInitException | UserNotFoundException e) {
                log.error("Could not update container status. Could not get user details for user " + userLogin, e);
            }
        } else {
            log.debug("Nothing to do. Container was null after retrieving by id {}.", event.containerId());
        }

        log.debug("Done processing docker container event: {}", event);
    }

    @Override
    public void processEvent(final ServiceTaskEvent event) {
        final ServiceTask task = event.task();
        final Container service;
        log.debug("Processing service task. Task id \"{}\" for service \"{}\".",
                task.taskId(), task.serviceId());

        // When we create the service, we don't know all the IDs. If this is the first time we
        // have seen a task for this service, we can set those IDs now.
        if (StringUtils.isBlank(event.service().taskId())) {
            log.debug("Service \"{}\" has no task information yet. Setting it now.", task.serviceId());
            final Container serviceToUpdate = event.service().toBuilder()
                    .taskId(task.taskId())
                    .containerId(task.containerId())
                    .nodeId(task.nodeId())
                    .build();
            containerEntityService.update(fromPojo(serviceToUpdate));
            service = retrieve(serviceToUpdate.databaseId());
        } else {
            service = event.service();
        }

        if (service != null) {
            final String userLogin = service.userId();
            try {
                final UserI userI = Users.getUser(userLogin);
                final ContainerHistory taskHistoryItem = ContainerHistory.fromServiceTask(task);
                final ContainerHistory createdTaskHistoryItem = addContainerHistoryItem(service, taskHistoryItem, userI);
                if (createdTaskHistoryItem == null) {
                    // We have already added this task and can safely skip it.
                    log.debug("Skipping task status we have already seen.");
                } else {
                    if (task.exitCode() != null || task.isExitStatus()) {
                        log.debug("Service has exited. Finalizing.");
                        final String exitCodeString = task.exitCode() == null ? null : String.valueOf(task.exitCode());
                        final Container serviceWithAddedEvent = retrieve(service.databaseId());
                        finalize(serviceWithAddedEvent, userI, exitCodeString);
                    }
                }
            } catch (UserInitException | UserNotFoundException e) {
                log.error("Could not update container status. Could not get user details for user " + userLogin, e);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Done processing service task event: " + event);
        }
    }

    @Override
    public void finalize(final String containerId, final UserI userI) throws NotFoundException {
        finalize(get(containerId), userI);
    }

    @Override
    public void finalize(final Container container, final UserI userI) {
        finalize(container, userI, container.exitCode());
    }

    @Override
    public void finalize(final Container container, final UserI userI, final String exitCode) {
        if (container.isSwarmService()) {
            log.info("Finalizing Container {}, service id {}.", container.databaseId(), container.serviceId());
        } else {
            log.info("Finalizing Container {}, container id {}.", container.databaseId(), container.containerId());
        }

        final Container finalized = containerFinalizeService.finalizeContainer(container, userI, exitCodeIsFailed(exitCode));

        log.info("Done uploading for Container {}. Now saving information about created outputs.", container.databaseId());

        containerEntityService.update(fromPojo(finalized));
        log.debug("Done saving outputs for Container {}.", container.databaseId());

        final Container parent = finalized.parentContainer();
        if (parent != null) {
            log.info("Container {} is a setup container for parent container {}. Checking whether parent needs a status change.", container.databaseId(), parent.databaseId());
            final List<Container> setupContainers = retrieveSetupContainersForParent(parent.databaseId());
            final List<Container> failedExitCode = new ArrayList<>();
            final List<Container> nullExitCode = new ArrayList<>();
            for (final Container setupContainer : setupContainers) {
                if (exitCodeIsFailed(setupContainer.exitCode())) {
                    failedExitCode.add(setupContainer);
                } else if (setupContainer.exitCode() == null) {
                    nullExitCode.add(setupContainer);
                }
            }

            final int numSetup = setupContainers.size();
            final int numFailed = failedExitCode.size();
            final int numNull = nullExitCode.size();

            if (numFailed > 0) {
                // If any of the setup containers failed, we must kill the rest and fail the main container.
                log.info("One or more setup containers have failed. Killing the rest and failing the parent.");
                for (final Container setupContainer : setupContainers) {
                    if (failedExitCode.contains(setupContainer)) {
                        log.debug("Setup container {} with container id {} failed.", setupContainer.databaseId(), setupContainer.containerId());
                    } else if (nullExitCode.contains(setupContainer)) {
                        log.info("Setup container {} with container id {} has no exit code. Attempting to kill it.", setupContainer.databaseId(), setupContainer.containerId());
                        try {
                            kill(setupContainer, userI);
                        } catch (NoDockerServerException | DockerServerException | NotFoundException e) {
                            log.error(String.format("Failed to kill setup container %d.", setupContainer.databaseId()), e);
                        }
                    } else {
                        log.debug("Setup container {} with container id {} succeeded.", setupContainer.databaseId(), setupContainer.containerId());
                    }
                }

                final String failedContainerMessageTemplate = "ID %d, container id %s";
                final String failedContainerMessage;
                if (failedExitCode.size() == 1) {
                    final Container failed = failedExitCode.get(0);
                    failedContainerMessage = "Failed setup container: " + String.format(failedContainerMessageTemplate, failed.databaseId(), failed.containerId());
                } else {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Failed setup containers: ");
                    sb.append(String.format(failedContainerMessageTemplate, failedExitCode.get(0).databaseId(), failedExitCode.get(0).containerId()));
                    for (int i = 1; i < failedExitCode.size(); i++) {
                        sb.append("; ");
                        sb.append(String.format(failedContainerMessageTemplate, failedExitCode.get(i).databaseId(), failedExitCode.get(i).containerId()));
                    }
                    sb.append(".");
                    failedContainerMessage = sb.toString();
                }

                log.info("Setting status to \"Failed Setup\" for parent container {} with container id {}.", parent.databaseId(), parent.containerId());
                addContainerHistoryItem(parent, ContainerHistory.fromSystem("Failed Setup", failedContainerMessage), userI);
            } else if (numNull == numSetup) {
                // This is an error. We know at least one setup container has finished because we have reached this "finalize" method.
                // At least one of the setup containers should have a non-null exit status.
                final String message = "All setup containers have null statuses, but one of them should be finished.";
                log.error(message);
                log.info("Setting status to \"Failed Setup\" for parent container {} with container id {}.", parent.databaseId(), parent.containerId());
                addContainerHistoryItem(parent, ContainerHistory.fromSystem("Failed Setup", message), userI);
            } else if (numNull > 0) {
                final int numLeft = numSetup - numNull;
                log.info("Not changing parent status. {} setup containers left to finish.", numLeft);
            } else {
                // If none of the setup containers have failed and none of the exit codes are null,
                // that means all the setup containers have succeeded.
                // We should start the parent container.
                log.info("All setup containers for parent Container {} are finished and not failed. Starting container id {}.", parent.databaseId(), parent.containerId());
                try {
                    startContainer(userI, parent);
                } catch (NoDockerServerException | ContainerException e) {
                    log.error("Failed to start parent Container {} with container id {}.", parent.databaseId(), parent.containerId());
                }
            }
        }
    }

    @Override
    public String kill(final String containerId, final UserI userI)
            throws NoDockerServerException, DockerServerException, NotFoundException {
        // TODO check user permissions. How?
        return kill(get(containerId), userI);
    }

    private String kill(final Container container, final UserI userI)
            throws NoDockerServerException, DockerServerException, NotFoundException {
        addContainerHistoryItem(container, ContainerHistory.fromUserAction("Killed", userI.getLogin()), userI);

        final String containerDockerId = container.containerId();
        containerControlApi.killContainer(containerDockerId);
        return containerDockerId;
    }

    @Override
    @Nonnull
    public Map<String, InputStream> getLogStreams(final long id)
            throws NotFoundException, NoDockerServerException, DockerServerException {
        return getLogStreams(get(id));
    }

    @Override
    @Nonnull
    public Map<String, InputStream> getLogStreams(final String containerId)
            throws NotFoundException, NoDockerServerException, DockerServerException {
        return getLogStreams(get(containerId));
    }

    @Nonnull
    private Map<String, InputStream> getLogStreams(final Container container)
            throws NotFoundException, NoDockerServerException, DockerServerException {
        final Map<String, InputStream> logStreams = Maps.newHashMap();
        for (final String logName : ContainerService.LOG_NAMES) {
            final InputStream logStream = getLogStream(container, logName);
            if (logStream != null) {
                logStreams.put(logName, logStream);
            }
        }
        return logStreams;
    }

    @Override
    @Nullable
    public InputStream getLogStream(final long id, final String logFileName)
            throws NotFoundException, NoDockerServerException, DockerServerException {
        return getLogStream(get(id), logFileName);
    }

    @Override
    @Nullable
    public InputStream getLogStream(final String containerId, final String logFileName)
            throws NotFoundException, NoDockerServerException, DockerServerException {
        return getLogStream(get(containerId), logFileName);
    }

    @Nullable
    private InputStream getLogStream(final Container container, final String logFileName)
            throws NoDockerServerException, DockerServerException {
        final String logPath = container.getLogPath(logFileName);
        if (StringUtils.isBlank(logPath)) {
            // If log path is blank, that means we have not yet saved the logs from docker. Go fetch them now.
            if (ContainerService.STDOUT_LOG_NAME.contains(logFileName)) {
                return new ByteArrayInputStream(containerControlApi.getContainerStdoutLog(container.containerId()).getBytes());
            } else if (ContainerService.STDERR_LOG_NAME.contains(logFileName)) {
                return new ByteArrayInputStream(containerControlApi.getContainerStderrLog(container.containerId()).getBytes());
            } else {
                return null;
            }
        } else {
            // If log path is not blank, that means we have saved the logs to a file. Read it now.
            try {
                return new FileInputStream(logPath);
            } catch (FileNotFoundException e) {
                log.error("Container %s log file %s not found. Path: %s", container.databaseId(), logFileName, logPath);
            }
        }

        return null;
    }

    private void handleFailure(final Container container) {
        // TODO handle failure
    }

    /**
     * Creates a workflow if possible and returns its ID.
     *
     * This is a way for us to show the
     * the container execution in the history table and as a workflow alert banner
     * (where enabled) without any custom UI work.
     *
     * It is possible to create a workflow for the execution if the resolved command
     * has one external input which is an XNAT object. If it has zero external inputs,
     * there is no object on which we can "hang" the workflow, so to speak. If it has more
     * than one external input, we don't know which is the one that should display the
     * workflow, so we don't make one.
     *
     * @param resolvedCommand A resolved command that will be used to launch a container
     * @param containerOrService The Container object which refers to either a container launched on
     *                           a single docker machine or a service created on a swarm
     * @param userI The user launching the container
     * @return ID of the created workflow, or null if no workflow was created
     */
    @Nullable
    private String makeWorkflowIfAppropriate(final ResolvedCommand resolvedCommand, final Container containerOrService, final UserI userI) {
        log.debug("Preparing to make workflow.");
        final XFTItem rootInputObject = findRootInputObject(resolvedCommand, userI);
        if (rootInputObject == null) {
            // We didn't find a root input XNAT object, so we can't make a workflow.
            log.debug("Cannot make workflow.");
            return null;
        }

        log.debug("Creating workflow for Wrapper {} - Command {} - Image {}.",
                resolvedCommand.wrapperName(), resolvedCommand.commandName(), resolvedCommand.image());
        try {
            final PersistentWorkflowI workflow = WorkflowUtils.buildOpenWorkflow(userI, rootInputObject,
                    EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS,
                            resolvedCommand.wrapperName(),
                            "Container launch",
                            StringUtils.isNotBlank(containerOrService.serviceId()) ?
                                    containerOrService.serviceId() :
                                    containerOrService.containerId()));
            WorkflowUtils.save(workflow, workflow.buildEvent());
            log.debug("Created workflow {}.", workflow.getWorkflowId());
            return String.valueOf(workflow.getWorkflowId());
        } catch (Exception e) {
            log.error("Could not create workflow.", e);
        }

        return null;
    }

    @Nullable
    private XFTItem findRootInputObject(final ResolvedCommand resolvedCommand, final UserI userI) {
        log.debug("Checking input values to find root XNAT input object.");
        final List<ResolvedInputTreeNode<? extends Command.Input>> flatInputTrees = resolvedCommand.flattenInputTrees();

        XFTItem rootInputValue = null;
        for (final ResolvedInputTreeNode<? extends Command.Input> node : flatInputTrees) {
            final Command.Input input = node.input();
            log.debug("Input \"{}\".", input.name());
            if (!(input instanceof Command.CommandWrapperExternalInput)) {
                log.debug("Skipping. Not an external wrapper input.");
                continue;
            }

            final String type = input.type();
            if (!(type.equals(PROJECT.getName()) || type.equals(SUBJECT.getName()) || type.equals(SESSION.getName()) || type.equals(SCAN.getName())
                    || type.equals(ASSESSOR.getName()) || type.equals(RESOURCE.getName()))) {
                log.debug("Skipping. Input type \"{}\" is not an XNAT type.", type);
                continue;
            }

            final List<ResolvedInputTreeNode.ResolvedInputTreeValueAndChildren> valuesAndChildren = node.valuesAndChildren();
            if (valuesAndChildren == null || valuesAndChildren.isEmpty() || valuesAndChildren.size() > 1) {
                log.debug("Skipping. {} values.", (valuesAndChildren == null || valuesAndChildren.isEmpty()) ? "No" : "Multiple");
                continue;
            }

            final ResolvedInputValue externalInputValue = valuesAndChildren.get(0).resolvedValue();
            final XnatModelObject inputValueXnatObject = externalInputValue.xnatModelObject();

            if (inputValueXnatObject == null) {
                log.debug("Skipping. XNAT model object is null.");
                continue;
            }

            if (rootInputValue != null) {
                // We have already seen one candidate for a root object.
                // Seeing this one means we have more than one, and won't be able to
                // uniquely resolve a root object.
                // We won't be able to make a workflow. We can bail out now.
                log.debug("Found another root XNAT input object. I was expecting one. Bailing out.", input.name());
                return null;
            }

            final XnatModelObject xnatObjectToUseAsRoot;
            if (type.equals(SCAN.getName())) {
                // If the external input is a scan, the workflow will not show up anywhere. So we
                // use its parent session as the root object instead.
                final XnatModelObject parentSession = ((Scan) inputValueXnatObject).getSession(userI);
                if (parentSession != null) {
                    xnatObjectToUseAsRoot = parentSession;
                } else {
                    // Ok, nevermind, use the scan anyway. It's not a huge thing.
                    xnatObjectToUseAsRoot = inputValueXnatObject;
                }
            } else {
                xnatObjectToUseAsRoot = inputValueXnatObject;
            }

            try {
                log.debug("Getting input value as XFTItem.");
                rootInputValue = xnatObjectToUseAsRoot.getXftItem(userI);
            } catch (Throwable t) {
                // If anything goes wrong, bail out. No workflow.
                log.error("That didn't work.", t);
                continue;
            }

            if (rootInputValue == null) {
                // I don't know if this is even possible
                log.debug("XFTItem is null.");
                continue;
            }

            // This is the first good input value.
            log.debug("Found a valid root XNAT input object.", input.name());
        }

        if (rootInputValue == null) {
            // Didn't find any candidates
            log.debug("Found no valid root XNAT input object candidates.");
            return null;
        }

        // At this point, we know we found a single valid external input value.
        // We can declare the object in that value to be the root object.

        return rootInputValue;
    }

    @Nonnull
    private Container toPojo(@Nonnull final ContainerEntity containerEntity) {
        return Container.create(containerEntity);
    }

    @Nonnull
    private List<Container> toPojo(@Nonnull final List<ContainerEntity> containerEntityList) {
        return Lists.newArrayList(Lists.transform(containerEntityList, new Function<ContainerEntity, Container>() {
            @Override
            public Container apply(final ContainerEntity input) {
                return toPojo(input);
            }
        }));
    }

    @Nonnull
    private ContainerHistory toPojo(@Nonnull final ContainerEntityHistory containerEntityHistory) {
        return ContainerHistory.create(containerEntityHistory);
    }

    @Nonnull
    private ContainerEntity fromPojo(@Nonnull final Container container) {
        final ContainerEntity template = containerEntityService.retrieve(container.databaseId());
        return template == null ? ContainerEntity.fromPojo(container) : template.update(container);
    }

    @Nonnull
    private ContainerEntityHistory fromPojo(@Nonnull final ContainerHistory containerHistory) {
        return ContainerEntityHistory.fromPojo(containerHistory);
    }

    private boolean exitCodeIsFailed(final String exitCode) {
        // Assume that everything is fine unless the exit code is explicitly > 0.
        // So exitCode="0", ="", =null all count as not failed.
        boolean isFailed = false;
        if (StringUtils.isNotBlank(exitCode)) {
            Long exitCodeNumber = null;
            try {
                exitCodeNumber = Long.parseLong(exitCode);
            } catch (NumberFormatException e) {
                // ignored
            }

            isFailed = exitCodeNumber != null && exitCodeNumber > 0;
        }
        return isFailed;
    }
}
