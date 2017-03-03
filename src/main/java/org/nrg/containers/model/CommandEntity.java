package org.nrg.containers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModelProperty;
import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.model.auto.Command;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DockerCommandEntity.class, name = "docker")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Audited
public abstract class CommandEntity extends AbstractHibernateEntity {
    public static CommandType DEFAULT_TYPE = CommandType.DOCKER;
    private String name;
    private String label;
    private String description;
    private String version;
    @JsonProperty("schema-version") private String schemaVersion;
    @JsonProperty("info-url") private String infoUrl;
    private String image;
    @JsonProperty("working-directory") private String workingDirectory;
    @JsonProperty("command-line") private String commandLine;
    @JsonProperty("mounts") private List<CommandMountEntity> mounts;
    @JsonProperty("environment-variables") private Map<String, String> environmentVariables;
    private List<CommandInputEntity> inputs;
    private List<CommandOutputEntity> outputs;
    @JsonProperty("xnat") private List<CommandWrapperEntity> commandWrapperEntities;

    @Nonnull
    public static CommandEntity fromPojo(@Nonnull final Command command, @Nullable final CommandEntity template)
            throws CommandValidationException {
        final List<String> errors = command.validate();
        if (!errors.isEmpty()) {
            throw new CommandValidationException(errors);
        }

        final CommandEntity commandEntity;
        final String type = command.type();
        switch (type) {
            case "docker":
                commandEntity = DockerCommandEntity.fromPojo(command);
                break;
            default:
                // This should have been caught already, but still...
                throw new CommandValidationException("Cannot instantiate command with type " + type);
        }

        commandEntity.setName(command.name());
        commandEntity.setLabel(command.label());
        commandEntity.setDescription(command.description());
        commandEntity.setVersion(command.version());
        commandEntity.setSchemaVersion(command.schemaVersion());
        commandEntity.setInfoUrl(command.infoUrl());
        commandEntity.setImage(command.image());
        commandEntity.setWorkingDirectory(command.workingDirectory());
        commandEntity.setCommandLine(command.commandLine());
        commandEntity.setEnvironmentVariables(command.environmentVariables());

        for (final Command.CommandMount commandMount : command.mounts()) {
            commandEntity.addMount(CommandMountEntity.fromPojo(commandMount));
        }

        for (final Command.CommandInput commandInput : command.inputs()) {
            commandEntity.addInput(CommandInputEntity.fromPojo(commandInput));
        }

        for (final Command.CommandOutput commandOutput : command.outputs()) {
            commandEntity.addOutput(CommandOutputEntity.fromPojo(commandOutput));
        }
        for (final Command.CommandWrapper commandWrapper : command.xnatCommandWrappers()) {
            commandEntity.addXnatCommandWrapper(CommandWrapperEntity.fromPojo(commandWrapper));
        }

        if (template != null) {
            commandEntity.setEnabled(template.isEnabled());
            commandEntity.setDisabled(template.getDisabled());
            commandEntity.setCreated(template.getCreated());
        }

        return commandEntity;
    }

    @Transient
    public abstract CommandType getType();

    // @javax.persistence.Id
    // @GeneratedValue(strategy = GenerationType.TABLE)
    // // @Override
    // public long getId() {
    //     return super.getId();
    // }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(final String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @ApiModelProperty("A URL where users can get more information about the Command")
    public String getInfoUrl() {
        return infoUrl;
    }

    public void setInfoUrl(final String infoUrl) {
        this.infoUrl = infoUrl;
    }

    public String getImage() {
        return image;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(final String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @ApiModelProperty("The command that will be executed in the container when the Command is launched.")
    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(final String commandLine) {
        this.commandLine = commandLine;
    }

    @OneToMany(mappedBy = "commandEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<CommandMountEntity> getMounts() {
        return mounts;
    }

    public void setMounts(final List<CommandMountEntity> mounts) {
        this.mounts = mounts == null ?
                Lists.<CommandMountEntity>newArrayList() :
                mounts;
        for (final CommandMountEntity mount : this.mounts) {
            mount.setCommandEntity(this);
        }
    }

    public void addMount(final CommandMountEntity mount) {
        if (mount == null) {
            return;
        }
        mount.setCommandEntity(this);

        if (this.mounts == null) {
            this.mounts = Lists.newArrayList();
        }
        this.mounts.add(mount);
    }

    @ElementCollection
    @ApiModelProperty("A Map of environment variables. Each kay is the environment variable's name, and each value is the environment variable's value." +
            "Both the names and values can use template strings, e.g. #variable-name#, which will be resolved into a value when the Command is launched.")
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(final Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables == null ?
                Maps.<String, String>newHashMap() :
                environmentVariables;
    }

    @OneToMany(mappedBy = "commandEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @ApiModelProperty("A list of inputs. " +
            "When the Command is launched, these inputs receive values; " +
            "those values will be used to fill in any template strings in the Command's run-template, mounts, or environment variables.")
    public List<CommandInputEntity> getInputs() {
        return inputs;
    }

    public void setInputs(final List<CommandInputEntity> inputs) {
        this.inputs = inputs == null ?
                Lists.<CommandInputEntity>newArrayList() :
                inputs;
        for (final CommandInputEntity input : this.inputs) {
            input.setCommandEntity(this);
        }
    }

    public void addInput(final CommandInputEntity input) {
        if (input == null) {
            return;
        }
        input.setCommandEntity(this);

        if (this.inputs == null) {
            this.inputs = Lists.newArrayList();
        }
        this.inputs.add(input);
    }

    @OneToMany(mappedBy = "commandEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @ApiModelProperty("A list of outputs.")
    public List<CommandOutputEntity> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<CommandOutputEntity> outputs) {
        this.outputs = outputs == null ?
                Lists.<CommandOutputEntity>newArrayList() :
                outputs;
        for (final CommandOutputEntity output : this.outputs) {
            output.setCommandEntity(this);
        }
    }

    public void addOutput(final CommandOutputEntity output) {
        if (output == null) {
            return;
        }
        output.setCommandEntity(this);

        if (this.outputs == null) {
            this.outputs = Lists.newArrayList();
        }
        this.outputs.add(output);
    }

    @OneToMany(mappedBy = "commandEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<CommandWrapperEntity> getCommandWrapperEntities() {
        return commandWrapperEntities;
    }

    public void setCommandWrapperEntities(final List<CommandWrapperEntity> commandWrapperEntities) {
        this.commandWrapperEntities = commandWrapperEntities == null ?
                Lists.<CommandWrapperEntity>newArrayList() :
                commandWrapperEntities;
    }

    @Transient
    public void addXnatCommandWrapper(final CommandWrapperEntity commandWrapperEntity) {
        if (commandWrapperEntity == null) {
            return;
        }
        commandWrapperEntity.setCommandEntity(this);

        if (this.commandWrapperEntities == null) {
            this.commandWrapperEntities = Lists.newArrayList();
        }
        this.commandWrapperEntities.add(commandWrapperEntity);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandEntity that = (CommandEntity) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    @Override
    public ToStringHelper addParentPropertiesToString(final ToStringHelper helper) {
        return super.addParentPropertiesToString(helper)
                .add("name", name)
                .add("label", label)
                .add("description", description)
                .add("version", version)
                .add("schemaVersion", schemaVersion)
                .add("infoUrl", infoUrl)
                .add("image", image)
                .add("workingDirectory", workingDirectory)
                .add("commandLine", commandLine)
                .add("mounts", mounts)
                .add("environmentVariables", environmentVariables)
                .add("inputs", inputs)
                .add("outputs", outputs)
                .add("xnatCommandWrappers", commandWrapperEntities);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .toString();
    }
}
