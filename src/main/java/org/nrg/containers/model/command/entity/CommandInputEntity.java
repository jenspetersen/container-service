package org.nrg.containers.model.command.entity;

import com.google.common.base.MoreObjects;
import org.hibernate.envers.Audited;
import org.nrg.containers.model.command.auto.Command;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Audited
public class CommandInputEntity implements Serializable {
    public static Type DEFAULT_TYPE = Type.STRING;

    private long id;
    private CommandEntity commandEntity;
    private String name;
    private String description;
    private Type type = DEFAULT_TYPE;
    private Boolean required;
    private String matcher;
    private String defaultValue;
    private String rawReplacementKey;
    private String commandLineFlag = "";
    private String commandLineSeparator = " ";
    private String trueValue;
    private String falseValue;

    public static CommandInputEntity fromPojo(final Command.CommandInput commandInput) {
        return new CommandInputEntity().update(commandInput);
    }

    @Nonnull
    public CommandInputEntity update(final Command.CommandInput commandInput) {
        if (this.id == 0L || commandInput.id() != 0L) {
            this.setId(commandInput.id());
        }
        this.setName(commandInput.name());
        this.setDescription(commandInput.description());
        this.setRequired(commandInput.required());
        this.setMatcher(commandInput.matcher());
        this.setDefaultValue(commandInput.defaultValue());
        this.setRawReplacementKey(commandInput.rawReplacementKey());
        this.setCommandLineFlag(commandInput.commandLineFlag());
        this.setCommandLineSeparator(commandInput.commandLineSeparator());
        this.setTrueValue(commandInput.trueValue());
        this.setFalseValue(commandInput.falseValue());

        switch (commandInput.type()) {
            case "string":
                this.setType(Type.STRING);
                break;
            case "boolean":
                this.setType(Type.BOOLEAN);
                break;
            case "number":
                this.setType(Type.NUMBER);
                break;
            default:
                this.setType(DEFAULT_TYPE);
        }

        return this;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @ManyToOne
    public CommandEntity getCommandEntity() {
        return commandEntity;
    }

    public void setCommandEntity(final CommandEntity commandEntity) {
        this.commandEntity = commandEntity;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Enumerated(EnumType.STRING)
    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public Boolean getRequired() {
        return required;
    }

    @Transient
    public boolean isRequired() {
        return required != null && required;
    }

    public void setRequired(final Boolean required) {
        this.required = required;
    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(final String matcher) {
        this.matcher = matcher;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final String value) {
        this.defaultValue = value;
    }

    public String getRawReplacementKey() {
        return rawReplacementKey;
    }

    public void setRawReplacementKey(final String rawReplacementKey) {
        this.rawReplacementKey = rawReplacementKey;
    }

    public String getCommandLineFlag() {
        return commandLineFlag;
    }

    public void setCommandLineFlag(final String commandLineFlag) {
        this.commandLineFlag = commandLineFlag;
    }

    public String getCommandLineSeparator() {
        return commandLineSeparator;
    }

    public void setCommandLineSeparator(final String commandLineSeparator) {
        this.commandLineSeparator = commandLineSeparator;
    }

    public String getTrueValue() {
        return trueValue;
    }

    public void setTrueValue(final String trueValue) {
        this.trueValue = trueValue;
    }

    public String getFalseValue() {
        return falseValue;
    }

    public void setFalseValue(final String falseValue) {
        this.falseValue = falseValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandInputEntity that = (CommandInputEntity) o;
        return Objects.equals(this.commandEntity, that.commandEntity) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandEntity, name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .add("type", type)
                .add("required", required)
                .add("matcher", matcher)
                .add("defaultValue", defaultValue)
                .add("rawReplacementKey", rawReplacementKey)
                .add("commandLineFlag", commandLineFlag)
                .add("commandLineSeparator", commandLineSeparator)
                .add("trueValue", trueValue)
                .add("falseValue", falseValue)
                .toString();
    }

    public enum Type {
        STRING("string"),
        BOOLEAN("boolean"),
        NUMBER("number");

        public final String name;

        Type(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}