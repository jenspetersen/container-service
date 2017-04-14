package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

/**
 * This is a value class that will be returned when the UI requests
 * a list of commands that are available to be launched for a given context.
 */
@AutoValue
public abstract class CommandSummaryForContext {
    @JsonProperty("command-id") public abstract long commandId();
    @JsonProperty("command-name") public abstract String commandName();
    @JsonProperty("wrapper-id") public abstract long wrapperId();
    @JsonProperty("wrapper-name") public abstract String wrapperName();
    @JsonProperty("image-name") public abstract String imageName();
    @JsonProperty("image-type") public abstract String imageType();
    @JsonProperty("enabled") public abstract boolean enabled();
    @JsonProperty("external-input-name") public abstract String externalInputName();

    public static CommandSummaryForContext create(final Command command,
                                                  final Command.CommandWrapper wrapper,
                                                  final boolean enabled,
                                                  final String externalInputName) {
        return new AutoValue_CommandSummaryForContext(
                command.id(),
                command.name(),
                wrapper.id(),
                wrapper.name(),
                command.image(),
                command.type(),
                enabled,
                externalInputName
        );
    }
}