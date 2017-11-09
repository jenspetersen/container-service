package org.nrg.containers.events.listeners;

import org.nrg.containers.events.model.ServiceTaskEvent;
import org.nrg.containers.services.ContainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import static reactor.bus.selector.Selectors.type;

@Component
public class DockerServiceEventListener implements Consumer<Event<ServiceTaskEvent>> {
    private static final Logger log = LoggerFactory.getLogger(DockerServiceEventListener.class);
    private ContainerService containerService;

    @Autowired
    public DockerServiceEventListener(final EventBus eventBus) {

        eventBus.on(type(ServiceTaskEvent.class), this);
    }

    @Override
    public void accept(final Event<ServiceTaskEvent> serviceTaskEventEvent) {
        final ServiceTaskEvent event = serviceTaskEventEvent.getData();
        try {
            containerService.processEvent(event);
        } catch (Throwable e) {
            log.error("There was a problem handling the docker event.", e);
        }
    }

    @Autowired
    public void setContainerService(final ContainerService containerService) {
        this.containerService = containerService;
    }
}
