package org.nrg.containers.model.container.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.nrg.containers.events.model.ContainerEvent;
import org.nrg.containers.events.model.DockerContainerEvent;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.command.auto.ResolvedCommandMount;
import org.nrg.containers.model.command.entity.CommandType;
import org.nrg.containers.model.container.ContainerInputType;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.containers.model.container.entity.ContainerEntityInput;
import org.nrg.containers.model.container.entity.ContainerEntityMount;
import org.nrg.containers.model.container.entity.ContainerEntityOutput;
import org.nrg.containers.model.container.entity.ContainerMountFilesEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@AutoValue
@JsonInclude(JsonInclude.Include.ALWAYS)
public abstract class Container {
    @JsonIgnore private String exitCode;

    @JsonProperty("id") public abstract long databaseId();
    @JsonProperty("command-id") public abstract long commandId();
    @Nullable @JsonProperty("status") public abstract String status();
    @Nullable @JsonProperty("status-time") public abstract Date statusTime();
    @JsonProperty("wrapper-id") public abstract long wrapperId();
    @Nullable @JsonProperty("container-id") public abstract String containerId();
    @Nullable @JsonProperty("workflow-id") public abstract String workflowId();
    @JsonProperty("user-id") public abstract String userId();
    @Nullable @JsonProperty("swarm") public abstract Boolean swarm();
    @Nullable @JsonProperty("service-id") public abstract String serviceId();
    @Nullable @JsonProperty("task-id") public abstract String taskId();
    @Nullable @JsonProperty("node-id") public abstract String nodeId();
    @JsonProperty("docker-image") public abstract String dockerImage();
    @JsonProperty("command-line") public abstract String commandLine();
    @Nullable @JsonProperty("override-entrypoint") public abstract Boolean overrideEntrypoint();
    @Nullable @JsonProperty("working-directory") public abstract String workingDirectory();
    @Nullable @JsonProperty("subtype") public abstract String subtype();
    @Nullable @JsonProperty("parent-database-id") public abstract Long parentDatabaseId();
    @Nullable @JsonProperty("parent-container-id") public abstract String parentContainerId();
    @JsonIgnore @Nullable public abstract Container parentContainer();
    @JsonProperty("env") public abstract ImmutableMap<String, String> environmentVariables();
    @JsonProperty("mounts") public abstract ImmutableList<ContainerMount> mounts();
    @JsonProperty("inputs") public abstract ImmutableList<ContainerInput> inputs();
    @JsonProperty("outputs") public abstract ImmutableList<ContainerOutput> outputs();
    @JsonProperty("history") public abstract ImmutableList<ContainerHistory> history();
    @JsonProperty("log-paths") public abstract ImmutableList<String> logPaths();
    @Nullable @JsonProperty("reserve-memory") public abstract Long reserveMemory();
    @Nullable @JsonProperty("limit-memory") public abstract Long limitMemory();
    @Nullable @JsonProperty("limit-cpu") public abstract Double limitCpu();

    @JsonIgnore
    public boolean isSwarmService() {
        final Boolean swarm = swarm();
        return swarm != null && swarm;
    }

    @JsonIgnore
    @Nullable
    public String exitCode() {
        if (exitCode == null) {
            // Assumption: At most one container history item will have a non-null exit code.
            // "": This event is an exit event (status == kill, die, or oom) but the attributes map
            //      did not contain an "exitCode" key
            // "0": success
            // "1" to "255": failure
            for (final ContainerHistory history : this.history()) {
                if (history.exitCode() != null) {
                    exitCode = history.exitCode();
                    break;
                }
            }
        }
        return exitCode;
    }

    @JsonCreator
    public static Container create(@JsonProperty("id") final long databaseId,
                                   @JsonProperty("command-id") final long commandId,
                                   @JsonProperty("status") final String status,
                                   @JsonProperty("status-time") final Date statusTime,
                                   @JsonProperty("wrapper-id") final long wrapperId,
                                   @JsonProperty("container-id") final String containerId,
                                   @JsonProperty("workflow-id") final String workflowId,
                                   @JsonProperty("user-id") final String userId,
                                   @JsonProperty("swarm") final Boolean swarm,
                                   @JsonProperty("service-id") final String serviceId,
                                   @JsonProperty("task-id") final String taskId,
                                   @JsonProperty("node-id") final String nodeId,
                                   @JsonProperty("docker-image") final String dockerImage,
                                   @JsonProperty("command-line") final String commandLine,
                                   @JsonProperty("override-entrypoint") final Boolean overrideEntrypoint,
                                   @JsonProperty("working-directory") final String workingDirectory,
                                   @JsonProperty("subtype") final String subtype,
                                   @JsonProperty("parent-database-id") final long parentDatabaseId,
                                   @JsonProperty("parent-container-id") final String parentContainerId,
                                   @JsonProperty("env") final Map<String, String> environmentVariables,
                                   @JsonProperty("mounts") final List<ContainerMount> mounts,
                                   @JsonProperty("inputs") final List<ContainerInput> inputs,
                                   @JsonProperty("outputs") final List<ContainerOutput> outputs,
                                   @JsonProperty("history") final List<ContainerHistory> history,
                                   @JsonProperty("log-paths") final List<String> logPaths,
                                   @JsonProperty("reserve-memory") final Long reserveMemory,
                                   @JsonProperty("limit-memory") final Long limitMemory,
                                   @JsonProperty("limit-cpu") final Double limitCpu) {

        return builder()
                .databaseId(databaseId)
                .status(status)
                .statusTime(statusTime == null ? null : new Date(statusTime.getTime()))
                .commandId(commandId)
                .wrapperId(wrapperId)
                .containerId(containerId)
                .workflowId(workflowId)
                .userId(userId)
                .swarm(swarm)
                .serviceId(serviceId)
                .taskId(taskId)
                .nodeId(nodeId)
                .dockerImage(dockerImage)
                .commandLine(commandLine)
                .overrideEntrypoint(overrideEntrypoint)
                .workingDirectory(workingDirectory)
                .subtype(subtype)
                .parentDatabaseId(parentDatabaseId)
                .parentContainerId(parentContainerId)
                .environmentVariables(environmentVariables == null ? Collections.<String, String>emptyMap() : environmentVariables)
                .mounts(mounts == null ? Collections.<ContainerMount>emptyList() : mounts)
                .inputs(inputs == null ? Collections.<ContainerInput>emptyList() : inputs)
                .outputs(outputs == null ? Collections.<ContainerOutput>emptyList() : outputs)
                .history(history == null ? Collections.<ContainerHistory>emptyList() : history)
                .logPaths(logPaths == null ? Collections.<String>emptyList() : logPaths)
                .reserveMemory(reserveMemory)
                .limitMemory(limitMemory)
                .limitCpu(limitCpu)
                .build();
    }

    public static Container create(final ContainerEntity containerEntity) {
        if (containerEntity == null) {
            return null;
        }
        return builder()
                .databaseId(containerEntity.getId())
                .status(containerEntity.getStatus())
                .statusTime(containerEntity.getStatusTime() == null ? null : new Date(containerEntity.getStatusTime().getTime()))
                .commandId(containerEntity.getCommandId())
                .wrapperId(containerEntity.getWrapperId())
                .containerId(containerEntity.getContainerId())
                .workflowId(containerEntity.getWorkflowId())
                .userId(containerEntity.getUserId())
                .swarm(containerEntity.getSwarm())
                .serviceId(containerEntity.getServiceId())
                .taskId(containerEntity.getTaskId())
                .nodeId(containerEntity.getNodeId())
                .dockerImage(containerEntity.getDockerImage())
                .commandLine(containerEntity.getCommandLine())
                .overrideEntrypoint(containerEntity.getOverrideEntrypoint())
                .workingDirectory(containerEntity.getWorkingDirectory())
                .subtype(containerEntity.getSubtype())
                .parentContainer(create(containerEntity.getParentContainerEntity()))
                .parentDatabaseId(containerEntity.getParentContainerEntity() != null ? containerEntity.getParentContainerEntity().getId() : null)
                .parentContainerId(containerEntity.getParentContainerEntity() != null ? containerEntity.getParentContainerEntity().getContainerId() : null)
                .environmentVariables(containerEntity.getEnvironmentVariables() == null ? Collections.<String, String>emptyMap() : containerEntity.getEnvironmentVariables())
                .logPaths(containerEntity.getLogPaths() == null ? Collections.<String>emptyList() : containerEntity.getLogPaths())
                .mounts(containerEntity.getMounts() == null ?
                        Collections.<ContainerMount>emptyList() :
                        Lists.transform(containerEntity.getMounts(), new Function<ContainerEntityMount, ContainerMount>() {
                            @Override
                            public ContainerMount apply(final ContainerEntityMount input) {
                                return ContainerMount.create(input);
                            }
                        })
                )
                .inputs(containerEntity.getInputs() == null ?
                        Collections.<ContainerInput>emptyList() :
                        Lists.transform(containerEntity.getInputs(), new Function<ContainerEntityInput, ContainerInput>() {
                            @Override
                            public ContainerInput apply(final ContainerEntityInput input) {
                                return ContainerInput.create(input);
                            }
                        })
                )
                .outputs(containerEntity.getOutputs() == null ?
                        Collections.<ContainerOutput>emptyList() :
                        Lists.transform(containerEntity.getOutputs(), new Function<ContainerEntityOutput, ContainerOutput>() {
                            @Override
                            public ContainerOutput apply(final ContainerEntityOutput input) {
                                return ContainerOutput.create(input);
                            }
                        })
                )
                .history(containerEntity.getHistory() == null ?
                        Collections.<ContainerHistory>emptyList() :
                        Lists.transform(containerEntity.getHistory(), new Function<ContainerEntityHistory, ContainerHistory>() {
                            @Override
                            public ContainerHistory apply(final ContainerEntityHistory input) {
                                return ContainerHistory.create(input);
                            }
                        })
                )
                .reserveMemory(containerEntity.getReserveMemory())
                .limitMemory(containerEntity.getLimitMemory())
                .limitCpu(containerEntity.getLimitCpu())
                .build();
    }

    public static Container containerFromResolvedCommand(final ResolvedCommand resolvedCommand,
                                                         final String containerId,
                                                         final String userId) {
        return buildFromResolvedCommand(resolvedCommand)
                .userId(userId)
                .containerId(containerId)
                .build();
    }

    public static Container serviceFromResolvedCommand(final ResolvedCommand resolvedCommand,
                                                       final String serviceId,
                                                       final String userId) {
        return buildFromResolvedCommand(resolvedCommand)
                .userId(userId)
                .serviceId(serviceId)
                .swarm(true)
                .build();
    }

    private static Container.Builder buildFromResolvedCommand(final ResolvedCommand resolvedCommand) {
        String containerSubtype = null;
        if (resolvedCommand.type().equals(CommandType.DOCKER_SETUP.getName())) {
            containerSubtype = "setup";
        }
        return builder()
                .databaseId(0L)
                .commandId(resolvedCommand.commandId())
                .wrapperId(resolvedCommand.wrapperId())
                .dockerImage(resolvedCommand.image())
                .commandLine(resolvedCommand.commandLine())
                .overrideEntrypoint(resolvedCommand.overrideEntrypoint())
                .workingDirectory(resolvedCommand.workingDirectory())
                .environmentVariables(resolvedCommand.environmentVariables())
                .subtype(containerSubtype)
                .mountsFromResolvedCommand(resolvedCommand.mounts())
                .addRawInputs(resolvedCommand.rawInputValues())
                .addCommandInputs(resolvedCommand.commandInputValues())
                .addExternalWrapperInputs(resolvedCommand.externalWrapperInputValues())
                .addDerivedWrapperInputs(resolvedCommand.derivedWrapperInputValues())
                .addOutputsFromResolvedCommand(resolvedCommand.outputs())
                .reserveMemory(resolvedCommand.reserveMemory())
                .limitMemory(resolvedCommand.limitMemory())
                .limitCpu(resolvedCommand.limitCpu());
    }

    public static Builder builder() {
        return new AutoValue_Container.Builder();
    }

    public abstract Builder toBuilder();

    private Map<String, String> getInputs(final ContainerInputType type) {
        final Map<String, String> inputs = Maps.newHashMap();
        for (final ContainerInput input : inputs()) {
            if (input.type() == type) {
                inputs.put(input.name(), input.value());
            }
        }
        return inputs;
    }

    @JsonIgnore
    public Map<String, String> getCommandInputs() {
        return getInputs(ContainerInputType.COMMAND);
    }

    @JsonIgnore
    @SuppressWarnings("deprecation")
    public Map<String, String> getWrapperInputs() {
        final Map<String, String> wrapperInputs = Maps.newHashMap();
        wrapperInputs.putAll(getLegacyWrapperInputs());
        wrapperInputs.putAll(getExternalWrapperInputs());
        wrapperInputs.putAll(getDerivedWrapperInputs());
        return wrapperInputs;
    }

    @JsonIgnore
    public Map<String, String> getExternalWrapperInputs() {
        return getInputs(ContainerInputType.WRAPPER_EXTERNAL);
    }

    @JsonIgnore
    public Map<String, String> getDerivedWrapperInputs() {
        return getInputs(ContainerInputType.WRAPPER_DERIVED);
    }

    /**
     * Get inputs of type "wrapper".
     * We no longer save inputs of this type. Now the wrapper inputs are separately saved
     * as type "wrapper_external" or "wrapper_derived". But we keep this here for legacy containers.
     * @return A map of wrapper input names to values.
     * @since 1.2
     */
    @JsonIgnore
    @Deprecated
    public Map<String, String> getLegacyWrapperInputs() {
        return getInputs(ContainerInputType.WRAPPER_DEPRECATED);
    }

    @JsonIgnore
    public Map<String, String> getRawInputs() {
        return getInputs(ContainerInputType.RAW);
    }

    @JsonIgnore
    public String getLogPath(final String filename) {
        for (final String path : logPaths()) {
            if (path.endsWith(filename)) {
                return path;
            }
        }
        return null;
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder databaseId(long databaseId);
        public abstract Builder commandId(long commandId);
        public abstract Builder wrapperId(long wrapperId);
        public abstract Builder containerId(String containerId);
        public abstract Builder workflowId(String workflowId);
        public abstract Builder userId(String userId);
        public abstract Builder dockerImage(String dockerImage);
        public abstract Builder commandLine(String commandLine);
        public abstract Builder overrideEntrypoint(Boolean overrideEntrypoint);
        public abstract Builder workingDirectory(String workingDirectory);
        public abstract Builder swarm(Boolean swarm);
        public abstract Builder serviceId(String serviceId);
        public abstract Builder taskId(String taskId);
        public abstract Builder nodeId(String nodeId);
        public abstract Builder status(String status);
        public abstract Builder statusTime(Date statusTime);
        public abstract Builder subtype(String subtype);
        public abstract Builder parentDatabaseId(Long parentDatabaseId);
        public abstract Builder parentContainerId(String parentContainerId);
        public abstract Builder parentContainer(Container parentContainer);
        public abstract Builder reserveMemory(Long reserveMemory);
        public abstract Builder limitMemory(Long limitMemory);
        public abstract Builder limitCpu(Double limitCpu);

        public Builder setParentProperties(final Container parentContainer) {
            return this
                    .parentContainer(parentContainer)
                    .parentContainerId(parentContainer == null ? null : parentContainer.containerId())
                    .parentDatabaseId(parentContainer == null ? null : parentContainer.databaseId());
        }

        public abstract Builder environmentVariables(Map<String, String> environmentVariables);
        abstract ImmutableMap.Builder<String, String> environmentVariablesBuilder();
        public Builder addEnvironmentVariable(final String envKey, final String envValue) {
            environmentVariablesBuilder().put(envKey, envValue);
            return this;
        }

        public abstract Builder mounts(List<ContainerMount> mounts);
        abstract ImmutableList.Builder<ContainerMount> mountsBuilder();
        public Builder addMount(final ContainerMount mounts) {
            mountsBuilder().add(mounts);
            return this;
        }
        public Builder mountsFromResolvedCommand(final List<ResolvedCommandMount> resolvedCommandMounts) {
            if (resolvedCommandMounts != null) {
                for (final ResolvedCommandMount resolvedCommandMount : resolvedCommandMounts) {
                    addMount(ContainerMount.create(resolvedCommandMount));
                }
            }
            return this;
        }

        public abstract Builder inputs(List<ContainerInput> inputs);
        abstract ImmutableList.Builder<ContainerInput> inputsBuilder();
        public Builder addInput(final ContainerInput inputs) {
            inputsBuilder().add(inputs);
            return this;
        }
        public Builder addInputsOfType(final ContainerInputType type, final Map<String, String> inputMap) {
            if (inputMap != null) {
                for (final Map.Entry<String, String> input : inputMap.entrySet()) {
                    addInput(ContainerInput.create(0L, type, input.getKey(), input.getValue()));
                }
            }
            return this;
        }
        public Builder addRawInputs(Map<String, String> inputMap) {
            return addInputsOfType(ContainerInputType.RAW, inputMap);
        }
        public Builder addExternalWrapperInputs(Map<String, String> inputMap) {
            return addInputsOfType(ContainerInputType.WRAPPER_EXTERNAL, inputMap);
        }
        public Builder addDerivedWrapperInputs(Map<String, String> inputMap) {
            return addInputsOfType(ContainerInputType.WRAPPER_DERIVED, inputMap);
        }
        public Builder addCommandInputs(Map<String, String> inputMap) {
            return addInputsOfType(ContainerInputType.COMMAND, inputMap);
        }

        public abstract Builder outputs(List<ContainerOutput> outputs);
        abstract ImmutableList.Builder<ContainerOutput> outputsBuilder();
        public Builder addOutput(final ContainerOutput outputs) {
            outputsBuilder().add(outputs);
            return this;
        }
        public Builder addOutputsFromResolvedCommand(final List<ResolvedCommand.ResolvedCommandOutput> resolvedCommandOutputs) {
            if (resolvedCommandOutputs != null) {
                for (final ResolvedCommand.ResolvedCommandOutput resolvedCommandOutput : resolvedCommandOutputs) {
                    addOutput(ContainerOutput.create(resolvedCommandOutput));
                }
            }
            return this;
        }

        public abstract Builder history(List<ContainerHistory> history);
        abstract ImmutableList.Builder<ContainerHistory> historyBuilder();
        public Builder addHistoryItem(final ContainerHistory history) {
            historyBuilder().add(history);
            return this;
        }

        public abstract Builder logPaths(List<String> logPaths);
        abstract ImmutableList.Builder<String> logPathsBuilder();
        public Builder addLogPath(final String logPaths) {
            logPathsBuilder().add(logPaths);
            return this;
        }

        public abstract Container build();
    }

    @AutoValue
    public static abstract class ContainerMount {
        @JsonProperty("id") public abstract long databaseId();
        @JsonProperty("name") public abstract String name();
        @JsonProperty("writable") public abstract boolean writable();
        @JsonProperty("xnat-host-path") public abstract String xnatHostPath();
        @JsonProperty("container-host-path") public abstract String containerHostPath();
        @JsonProperty("container-path") public abstract String containerPath();
        @JsonProperty("input-files") public abstract ImmutableList<ContainerMountFiles> inputFiles();

        @JsonCreator
        public static ContainerMount create(@JsonProperty("id") final long databaseId,
                                            @JsonProperty("name") final String name,
                                            @JsonProperty("writable") final boolean writable,
                                            @JsonProperty("xnat-host-path") final String xnatHostPath,
                                            @JsonProperty("container-host-path") final String containerHostPath,
                                            @JsonProperty("container-path") final String containerPath,
                                            @JsonProperty("input-files") final List<ContainerMountFiles> inputFiles) {
            return builder()
                    .databaseId(databaseId)
                    .name(name)
                    .writable(writable)
                    .xnatHostPath(xnatHostPath)
                    .containerHostPath(containerHostPath)
                    .containerPath(containerPath)
                    .inputFiles(inputFiles == null ? Collections.<ContainerMountFiles>emptyList() : inputFiles)
                    .build();
        }

        public static ContainerMount create(final ContainerEntityMount containerEntityMount) {
            final List<ContainerMountFiles> containerMountFiles = containerEntityMount.getInputFiles() == null ? null :
                    Lists.transform(containerEntityMount.getInputFiles(), new Function<ContainerMountFilesEntity, ContainerMountFiles>() {
                        @Override
                        public ContainerMountFiles apply(final ContainerMountFilesEntity input) {
                            return ContainerMountFiles.create(input);
                        }
                    });
            return create(containerEntityMount.getId(), containerEntityMount.getName(), containerEntityMount.isWritable(),
                    containerEntityMount.getXnatHostPath(), containerEntityMount.getContainerHostPath(),
                    containerEntityMount.getContainerPath(), containerMountFiles);
        }

        public static ContainerMount create(final ResolvedCommandMount resolvedCommandMount) {
            return create(0L,
                    resolvedCommandMount.name(),
                    resolvedCommandMount.writable(),
                    resolvedCommandMount.xnatHostPath(),
                    resolvedCommandMount.containerHostPath(),
                    resolvedCommandMount.containerPath(),
                    Collections.singletonList(ContainerMountFiles.create(0L,
                            resolvedCommandMount.fromWrapperInput(),
                            resolvedCommandMount.fromUri(),
                            resolvedCommandMount.fromRootDirectory(),
                            null)));
        }

        public static Builder builder() {
            return new AutoValue_Container_ContainerMount.Builder();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder databaseId(long databaseId);
            public abstract Builder name(String name);
            public abstract Builder writable(boolean writable);
            public abstract Builder xnatHostPath(String xnatHostPath);
            public abstract Builder containerHostPath(String containerHostPath);
            public abstract Builder containerPath(String containerPath);

            public abstract Builder inputFiles(List<ContainerMountFiles> inputFiles);
            abstract ImmutableList.Builder<ContainerMountFiles> inputFilesBuilder();
            public Builder addInputFiles(final ContainerMountFiles inputFiles) {
                inputFilesBuilder().add(inputFiles);
                return this;
            }

            public abstract ContainerMount build();
        }
    }

    @AutoValue
    public static abstract class ContainerMountFiles {
        @JsonProperty("id") public abstract long databaseId();
        @Nullable @JsonProperty("from-xnat-input") public abstract String fromXnatInput();
        @Nullable @JsonProperty("from-uri") public abstract String fromUri();
        @Nullable @JsonProperty("root-directory") public abstract String rootDirectory();
        @Nullable @JsonProperty("path") public abstract String path();

        @JsonCreator
        public static ContainerMountFiles create(@JsonProperty("id") final long databaseId,
                                                 @JsonProperty("from-xnat-input") final String fromXnatInput,
                                                 @JsonProperty("from-uri") final String fromUri,
                                                 @JsonProperty("root-directory") final String rootDirectory,
                                                 @JsonProperty("path") final String path) {
            return new AutoValue_Container_ContainerMountFiles(databaseId, fromXnatInput, fromUri, rootDirectory, path);
        }

        public static ContainerMountFiles create(final ContainerMountFilesEntity containerMountFilesEntity) {
            return create(containerMountFilesEntity.getId(), containerMountFilesEntity.getFromXnatInput(), containerMountFilesEntity.getFromUri(),
                    containerMountFilesEntity.getRootDirectory(), containerMountFilesEntity.getPath());
        }
    }

    @AutoValue
    public static abstract class ContainerInput {
        @JsonProperty("id") public abstract long databaseId();
        @JsonProperty("type") public abstract ContainerInputType type();
        @JsonProperty("name") public abstract String name();
        @JsonProperty("value") public abstract String value();

        @JsonCreator
        public static ContainerInput create(@JsonProperty("id") final long databaseId,
                                            @JsonProperty("type") final ContainerInputType type,
                                            @JsonProperty("name") final String name,
                                            @JsonProperty("value") final String value) {
            return new AutoValue_Container_ContainerInput(databaseId, type, name, value);
        }

        public static ContainerInput create(final ContainerEntityInput containerEntityInput) {
            return create(containerEntityInput.getId(), containerEntityInput.getType(), containerEntityInput.getName(), containerEntityInput.getValue());
        }
    }

    @AutoValue
    public static abstract class ContainerOutput {
        @JsonProperty("id") public abstract long databaseId();
        @JsonProperty("name") public abstract String name();
        @JsonProperty("type") public abstract String type();
        @JsonProperty("required") public abstract Boolean required();
        @JsonProperty("mount") public abstract String mount();
        @Nullable @JsonProperty("path") public abstract String path();
        @Nullable @JsonProperty("glob") public abstract String glob();
        @JsonProperty("label") public abstract String label();
        @Nullable @JsonProperty("created") public abstract String created();
        @JsonProperty("handled-by-wrapper-input") public abstract String handledByWrapperInput();

        @JsonCreator
        public static ContainerOutput create(@JsonProperty("id") final long databaseId,
                                             @JsonProperty("name") final String name,
                                             @JsonProperty("type") final String type,
                                             @JsonProperty("required") final Boolean required,
                                             @JsonProperty("mount") final String mount,
                                             @JsonProperty("path") final String path,
                                             @JsonProperty("glob") final String glob,
                                             @JsonProperty("label") final String label,
                                             @JsonProperty("created") final String created,
                                             @JsonProperty("handled-by-wrapper-input") final String handledByWrapperInput) {
            return builder()
                    .databaseId(databaseId)
                    .name(name)
                    .type(type)
                    .required(required)
                    .mount(mount)
                    .path(path)
                    .glob(glob)
                    .label(label)
                    .created(created)
                    .handledByWrapperInput(handledByWrapperInput)
                    .build();
        }

        public static ContainerOutput create(final ContainerEntityOutput containerEntityOutput) {
            return create(containerEntityOutput.getId(), containerEntityOutput.getName(), containerEntityOutput.getType(), containerEntityOutput.isRequired(),
                    containerEntityOutput.getMount(), containerEntityOutput.getPath(), containerEntityOutput.getGlob(),
                    containerEntityOutput.getLabel(), containerEntityOutput.getCreated(), containerEntityOutput.getHandledByXnatCommandInput());
        }

        public static ContainerOutput create(final ResolvedCommand.ResolvedCommandOutput resolvedCommandOutput) {
            return create(0L,
                    resolvedCommandOutput.name(),
                    resolvedCommandOutput.type(),
                    resolvedCommandOutput.required(),
                    resolvedCommandOutput.mount(),
                    resolvedCommandOutput.path(),
                    resolvedCommandOutput.glob(),
                    resolvedCommandOutput.label(),
                    null,
                    resolvedCommandOutput.handledByWrapperInput());
        }

        public static Builder builder() {
            return new AutoValue_Container_ContainerOutput.Builder();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder databaseId(long databaseId);
            public abstract Builder name(String name);
            public abstract Builder type(String type);
            public abstract Builder required(Boolean required);
            public abstract Builder mount(String mount);
            public abstract Builder path(String path);
            public abstract Builder glob(String glob);
            public abstract Builder label(String label);
            public abstract Builder created(String created);
            public abstract Builder handledByWrapperInput(String handledByWrapperInput);

            public abstract ContainerOutput build();
        }
    }

    @AutoValue
    public static abstract class ContainerHistory {
        @Nullable @JsonProperty("id") public abstract Long databaseId();
        @JsonProperty("status") public abstract String status();
        @JsonProperty("entity-type") public abstract String entityType();
        @Nullable @JsonProperty("entity-id") public abstract String entityId();
        @JsonProperty("time-recorded") public abstract Date timeRecorded();
        @Nullable @JsonProperty("external-timestamp") public abstract String externalTimestamp();
        @Nullable @JsonProperty("message") public abstract String message();
        @Nullable @JsonProperty("exitCode") public abstract String exitCode();

        @JsonCreator
        public static ContainerHistory create(@JsonProperty("id") final long databaseId,
                                              @JsonProperty("status") final String status,
                                              @JsonProperty("entity-type") final String entityType,
                                              @JsonProperty("entity-id") final String entityId,
                                              @JsonProperty("time-recorded") final Date timeRecorded,
                                              @JsonProperty("external-timestamp") final String externalTimestamp,
                                              @JsonProperty("message") final String message,
                                              @JsonProperty("exitCode") final String exitCode) {
            return builder()
                    .databaseId(databaseId)
                    .status(status)
                    .entityType(entityType)
                    .entityId(entityId)
                    .timeRecorded(timeRecorded)
                    .externalTimestamp(externalTimestamp)
                    .message(message)
                    .exitCode(exitCode)
                    .build();
        }

        public static ContainerHistory create(final ContainerEntityHistory containerEntityHistory) {
            return builder()
                    .databaseId(containerEntityHistory.getId())
                    .status(containerEntityHistory.getStatus())
                    .entityType(containerEntityHistory.getEntityType())
                    .entityId(containerEntityHistory.getEntityId())
                    .timeRecorded(containerEntityHistory.getTimeRecorded())
                    .externalTimestamp(containerEntityHistory.getExternalTimestamp())
                    .exitCode(containerEntityHistory.getExitCode())
                    .build();
        }

        public static ContainerHistory fromContainerEvent(final ContainerEvent containerEvent) {
            return builder()
                    .status(containerEvent.status())
                    .entityType("event")
                    .entityId(null)
                    .timeRecorded(new Date())
                    .externalTimestamp(containerEvent instanceof DockerContainerEvent ? String.valueOf(((DockerContainerEvent)containerEvent).timeNano()) : null)
                    .message(null)
                    .exitCode(containerEvent.exitCode())
                    .build();
        }

        public static ContainerHistory fromSystem(final String status,
                                                  final String message) {
            return builder()
                    .status(status)
                    .entityType("system")
                    .entityId(null)
                    .timeRecorded(new Date())
                    .externalTimestamp(null)
                    .message(message)
                    .build();
        }

        public static ContainerHistory fromUserAction(final String status, final String username) {
            return builder()
                    .status(status)
                    .entityType("user")
                    .entityId(username)
                    .timeRecorded(new Date())
                    .externalTimestamp(null)
                    .message(null)
                    .build();
        }

        public static ContainerHistory fromServiceTask(final ServiceTask task) {
            return builder()
                    .entityType("service")
                    .entityId(null)
                    .status(task.status())
                    .exitCode(task.exitCode() == null ? null : String.valueOf(task.exitCode()))
                    .timeRecorded(new Date())
                    .externalTimestamp(task.statusTime() == null ? null : String.valueOf(task.statusTime().getTime()))
                    .message(task.message())
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_Container_ContainerHistory.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder databaseId(Long databaseId);
            public abstract Builder status(String status);
            public abstract Builder entityType(String entityType);
            public abstract Builder entityId(String entityId);
            public abstract Builder timeRecorded(Date timeRecorded);
            public abstract Builder externalTimestamp(String externalTimestamp);
            public abstract Builder message(String message);
            public abstract Builder exitCode(String exitCode);
            public abstract ContainerHistory build();
        }
    }
}
