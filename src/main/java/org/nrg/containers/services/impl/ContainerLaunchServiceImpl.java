package org.nrg.containers.services.impl;

import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerMountResolutionException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.helpers.CommandResolutionHelper;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.ContainerExecutionMount;
import org.nrg.containers.model.ContainerMountFiles;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.Command.CommandWrapper;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerExecutionService;
import org.nrg.containers.services.ContainerLaunchService;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ContainerLaunchServiceImpl implements ContainerLaunchService {
    private static final Logger log = LoggerFactory.getLogger(ContainerLaunchServiceImpl.class);

    private CommandService commandService;
    private final ContainerControlApi controlApi;
    private final AliasTokenService aliasTokenService;
    private final SiteConfigPreferences siteConfigPreferences;
    private final TransportService transporter;
    private final ContainerExecutionService containerExecutionService;
    private final ConfigService configService;

    @Autowired
    public ContainerLaunchServiceImpl(final CommandService commandService,
                                      final ContainerControlApi controlApi,
                                      final AliasTokenService aliasTokenService,
                                      final SiteConfigPreferences siteConfigPreferences,
                                      final TransportService transporter,
                                      final ContainerExecutionService containerExecutionService,
                                      final ConfigService configService) {
        this.commandService = commandService;
        this.controlApi = controlApi;
        this.aliasTokenService = aliasTokenService;
        this.siteConfigPreferences = siteConfigPreferences;
        this.transporter = transporter;
        this.containerExecutionService = containerExecutionService;
        this.configService = configService;
    }


    @Override
    @Nonnull
    public ResolvedCommand resolveCommand(final long commandId,
                                          final Map<String, String> runtimeInputValues,
                                          final UserI userI)
            throws NotFoundException, CommandResolutionException {
        final Command command = commandService.get(commandId);
        return resolveCommand(command, runtimeInputValues, userI);

    }

    @Override
    @Nonnull
    public ResolvedCommand resolveCommand(final String xnatCommandWrapperName,
                                          final long commandId,
                                          final Map<String, String> runtimeInputValues,
                                          final UserI userI)
            throws NotFoundException, CommandResolutionException {
        if (StringUtils.isBlank(xnatCommandWrapperName)) {
            return resolveCommand(commandId, runtimeInputValues, userI);
        }

        final Command command = commandService.get(commandId);
        CommandWrapper wrapper = null;

        for (final CommandWrapper commandWrapper : command.xnatCommandWrappers()) {
            if (xnatCommandWrapperName.equals(commandWrapper.name())) {
                wrapper = commandWrapper;
                break;
            }
        }

        if (wrapper == null) {
            throw new NotFoundException(String.format("Command %d has no wrapper with name \"%s\".", commandId, xnatCommandWrapperName));
        }

        return resolveCommand(wrapper, command, runtimeInputValues, userI);
    }

    @Override
    @Nonnull
    public ResolvedCommand resolveCommand(final long xnatCommandWrapperId,
                                          final long commandId,
                                          final Map<String, String> runtimeInputValues,
                                          final UserI userI)
            throws NotFoundException, CommandResolutionException {
        final Command command = commandService.get(commandId);
        CommandWrapper wrapper = null;

        for (final CommandWrapper commandWrapper : command.xnatCommandWrappers()) {
            if (xnatCommandWrapperId == commandWrapper.id()) {
                wrapper = commandWrapper;
                break;
            }
        }

        if (wrapper == null) {
            throw new NotFoundException(String.format("Command %d has no wrapper with id %d.", commandId, xnatCommandWrapperId));
        }

        return resolveCommand(wrapper, command, runtimeInputValues, userI);
    }

    @Override
    @Nonnull
    public ResolvedCommand resolveCommand(final Command command,
                                          final Map<String, String> runtimeInputValues,
                                          final UserI userI)
            throws NotFoundException, CommandResolutionException {
        // I was not given a wrapper.
        // TODO what should I do here? Should I...
        //  1. Use the "passthrough" wrapper, no matter what
        //  2. Use the "passthrough" wrapper only if the command has no outputs
        //  3. check if the command has any wrappers, and use one if it exists
        //  4. Something else
        //
        // I guess for now I'll do 2.

        if (!command.outputs().isEmpty()) {
            throw new CommandResolutionException("Cannot resolve command without an XNAT wrapper. Command has outputs that will not be handled.");
        }

        final CommandWrapper commandWrapperToResolve = CommandWrapper.passthrough(command);

        return resolveCommand(commandWrapperToResolve, command, runtimeInputValues, userI);
    }

    @Override
    @Nonnull
    public ResolvedCommand resolveCommand(final CommandWrapper commandWrapper,
                                          final Command command,
                                          final Map<String, String> runtimeInputValues,
                                          final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return CommandResolutionHelper.resolve(commandWrapper, command, runtimeInputValues, userI, configService);
    }

    @Override
    public ContainerExecution resolveAndLaunchCommand(final long commandId,
                                                      final Map<String, String> runtimeValues,
                                                      final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException {
        final ResolvedCommand resolvedCommand = resolveCommand(commandId, runtimeValues, userI);
        switch (resolvedCommand.getType()) {
            case DOCKER:
                return launchResolvedDockerCommand((ResolvedDockerCommand) resolvedCommand, userI);
            default:
                return null; // TODO throw error
        }
    }

    @Override
    public ContainerExecution resolveAndLaunchCommand(final String xnatCommandWrapperName,
                                                      final long commandId,
                                                      final Map<String, String> runtimeValues,
                                                      final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException {
        final ResolvedCommand resolvedCommand = resolveCommand(xnatCommandWrapperName, commandId, runtimeValues, userI);
        switch (resolvedCommand.getType()) {
            case DOCKER:
                return launchResolvedDockerCommand((ResolvedDockerCommand) resolvedCommand, userI);
            default:
                return null; // TODO throw error
        }
    }

    @Override
    public ContainerExecution resolveAndLaunchCommand(final long xnatCommandWrapperId,
                                                      final long commandId,
                                                      final Map<String, String> runtimeValues,
                                                      final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException {
        final ResolvedCommand resolvedCommand = resolveCommand(xnatCommandWrapperId, commandId, runtimeValues, userI);
        switch (resolvedCommand.getType()) {
            case DOCKER:
                return launchResolvedDockerCommand((ResolvedDockerCommand) resolvedCommand, userI);
            default:
                return null; // TODO throw error
        }
    }

    @Override
    @Nonnull
    public ContainerExecution launchResolvedDockerCommand(final ResolvedDockerCommand resolvedDockerCommand,
                                                          final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerMountResolutionException {
        log.info("Preparing to launch resolved command.");
        final ResolvedDockerCommand preparedToLaunch = prepareToLaunch(resolvedDockerCommand, userI);

        log.info("Launching resolved command.");
        final String containerId = controlApi.launchImage(preparedToLaunch);

        log.info("Recording command launch.");
        return containerExecutionService.save(preparedToLaunch, containerId, userI);
    }

    @Nonnull
    private ResolvedDockerCommand prepareToLaunch(final ResolvedDockerCommand resolvedDockerCommand,
                                                  final UserI userI)
            throws NoServerPrefException, ContainerMountResolutionException {
        // Add default environment variables
        final Map<String, String> defaultEnv = Maps.newHashMap();

        defaultEnv.put("XNAT_HOST", (String)siteConfigPreferences.getProperty("processingUrl", siteConfigPreferences.getSiteUrl()));

        final AliasToken token = aliasTokenService.issueTokenForUser(userI);
        defaultEnv.put("XNAT_USER", token.getAlias());
        defaultEnv.put("XNAT_PASS", token.getSecret());

        if (log.isDebugEnabled()) {
            log.debug("Adding default environment variables");
            for (final String envKey : defaultEnv.keySet()) {
                log.debug(String.format("%s=%s", envKey, defaultEnv.get(envKey)));
            }
        }
        resolvedDockerCommand.addEnvironmentVariables(defaultEnv);

        // Transport mounts
        if (resolvedDockerCommand.getMounts() != null && !resolvedDockerCommand.getMounts().isEmpty()) {

            final String dockerHost = controlApi.getServer().getHost();
            for (final ContainerExecutionMount mount : resolvedDockerCommand.getMounts()) {
                // First, figure out what we have.
                // Do we have source files? A source directory?
                // Can we mount a directory directly, or should we copy the contents to a build directory?
                final List<ContainerMountFiles> filesList = mount.getInputFiles();
                final String buildDirectory;
                if (filesList != null && filesList.size() > 1) {
                    // We have multiple sources of files. We must copy them into one common location to mount.
                    buildDirectory = getBuildDirectory();
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Mount \"%s\" has multiple sources of files.", mount.getName()));
                    }

                    // TODO figure out what to do with multiple sources of files
                    if (log.isDebugEnabled()) {
                        log.debug("TODO");
                    }
                } else if (filesList != null && filesList.size() == 1) {
                    // We have one source of files. We may need to copy, or may be able to mount directly.
                    final ContainerMountFiles files = filesList.get(0);
                    final String path = files.getPath();
                    final boolean hasPath = StringUtils.isNotBlank(path);

                    if (StringUtils.isNotBlank(files.getRootDirectory())) {
                        // That source of files does have a directory set.

                        if (hasPath || mount.isWritable()) {
                            // In both of these conditions, we must copy some things to a build directory.
                            // Now we must find out what.
                            if (hasPath) {
                                // The source of files also has one or more paths set

                                buildDirectory = getBuildDirectory();
                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Mount \"%s\" has a root directory and a file. Copying the file from the root directory to build directory.", mount.getName()));
                                }

                                // TODO copy the file in "path", relative to the root directory, to the build directory
                                if (log.isDebugEnabled()) {
                                    log.debug("TODO");
                                }
                            } else {
                                // The mount is set to "writable".
                                buildDirectory = getBuildDirectory();
                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Mount \"%s\" has a root directory, and is set to \"writable\". Copying all files from the root directory to build directory.", mount.getName()));
                                }

                                // TODO We must copy all files out of the root directory to a build directory.
                                if (log.isDebugEnabled()) {
                                    log.debug("TODO");
                                }
                            }
                        } else {
                            // The source of files can be directly mounted
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Mount \"%s\" has a root directory, and is not set to \"writable\". The root directory can be mounted directly into the container.", mount.getName()));
                            }
                            buildDirectory = files.getRootDirectory();
                        }
                    } else if (hasPath) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Mount \"%s\" has a file. Copying it to build directory.", mount.getName()));
                        }
                        buildDirectory = getBuildDirectory();
                        // TODO copy the file to the build directory
                        if (log.isDebugEnabled()) {
                            log.debug("TODO");
                        }

                    } else {
                        final String message = String.format("Mount \"%s\" should have a file path or a directory or both but it does not.", mount.getName());
                        log.error(message);
                        throw new ContainerMountResolutionException(message, mount);
                    }

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Mount \"%s\" has no input files. Ensuring mount is set to \"writable\" and creating new build directory.", mount.getName()));
                    }
                    buildDirectory = getBuildDirectory();
                    if (!mount.isWritable()) {
                        mount.setWritable(true);
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Setting mount \"%s\" xnat host path to \"%s\".", mount.getName(), buildDirectory));
                }
                mount.setXnatHostPath(buildDirectory);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Transporting mount \"%s\".", mount.getName()));
                }
                final Path pathOnDockerHost = transporter.transport(dockerHost, Paths.get(buildDirectory));

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Setting mount \"%s\" container host path to \"%s\".", mount.getName(), buildDirectory));
                }
                mount.setContainerHostPath(pathOnDockerHost.toString());
            }
        }

        return resolvedDockerCommand;
    }

    private String getBuildDirectory() {
        String buildPath = siteConfigPreferences.getBuildPath();
        final String uuid = UUID.randomUUID().toString();
        return FilenameUtils.concat(buildPath, uuid);
    }

}
