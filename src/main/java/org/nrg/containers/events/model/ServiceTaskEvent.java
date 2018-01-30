package org.nrg.containers.events.model;

import com.google.auto.value.AutoValue;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.auto.ServiceTask;
import org.nrg.framework.event.EventI;

import javax.annotation.Nonnull;

@AutoValue
public abstract class ServiceTaskEvent implements EventI {
    public abstract ServiceTask task();
    public abstract Container service();

    public static ServiceTaskEvent create(final @Nonnull ServiceTask task, final @Nonnull Container service) {
        return new AutoValue_ServiceTaskEvent(task, service);
    }

}
