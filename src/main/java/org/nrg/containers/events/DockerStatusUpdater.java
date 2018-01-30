package org.nrg.containers.events;

import com.google.common.collect.Lists;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoDockerServerException;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.auto.ServiceTask;
import org.nrg.containers.model.server.docker.DockerServerBase.DockerServer;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.DockerServerService;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xft.schema.XFTManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class DockerStatusUpdater implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DockerStatusUpdater.class);

    private ContainerControlApi controlApi;
    private DockerServerService dockerServerService;
    private ContainerService containerService;

    private boolean haveLoggedDockerConnectFailure = false;
    private boolean haveLoggedNoServerInDb = false;
    private boolean haveLoggedXftInitFailure = false;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public DockerStatusUpdater(final ContainerControlApi controlApi,
                               final DockerServerService dockerServerService,
                               final ContainerService containerService) {
        this.controlApi = controlApi;
        this.dockerServerService = dockerServerService;
        this.containerService = containerService;
    }

    @Override
    public void run() {
        log.trace("Attempting to update status with docker.");

        final String skipMessage = "Skipping attempt to update status.";

        if (!XFTManager.isInitialized()) {
            if (!haveLoggedXftInitFailure) {
                log.info("XFT is not initialized. " + skipMessage);
                haveLoggedXftInitFailure = true;
            }
            return;
        }

        // Since XFT is up, we should be able to connect to the database and read the docker server
        DockerServer dockerServer = null;
        try {
            dockerServer = dockerServerService.getServer();
        } catch (NotFoundException e) {
            // ignored
        }
        if (dockerServer == null) {
            if (!haveLoggedNoServerInDb) {
                log.info("No docker server has been defined (or enabled) in the database. " + skipMessage);
                haveLoggedNoServerInDb = true;
                haveLoggedXftInitFailure = false;
            }
            return;
        }

        if (!controlApi.canConnect()) {
            if (!haveLoggedDockerConnectFailure) {
                log.info("Cannot ping docker server " + dockerServer.name() + ". " + skipMessage);
                haveLoggedDockerConnectFailure = true;
                haveLoggedXftInitFailure = false;
                haveLoggedNoServerInDb = false;
            }
            return;
        }

        // Now we should be able to check the status
        final UpdateReport updateReport = dockerServer.swarmMode() ? updateServices(dockerServer) : updateContainers(dockerServer);
        if (updateReport.successful == null) {
            // This means some, but not all, of the services didn't update properly. Which ones?
            for (final UpdateReportEntry entry : updateReport.updateReports) {
                if (!entry.successful) {
                    log.error("Could not update status for {}. Message: {}", entry.id, entry.message);
                } else {
                    log.debug("Updated successfully for {}.", entry.id);
                }
            }

            // Reset failure flags
            haveLoggedDockerConnectFailure = false;
            haveLoggedXftInitFailure = false;
            haveLoggedNoServerInDb = false;
        } else if (updateReport.successful) {
            if (updateReport.updateReports.size() > 0) {
                log.debug("Updated status successfully.");
            }
            // Reset failure flags
            haveLoggedDockerConnectFailure = false;
            haveLoggedXftInitFailure = false;
            haveLoggedNoServerInDb = false;
        } else {
            log.info("Did not update status successfully.");
        }
    }

    @Nonnull
    private UpdateReport updateContainers(final DockerServer dockerServer) {
        final Date lastEventCheckTime = dockerServer.lastEventCheckTime();
        final Date since = lastEventCheckTime == null ? new Date(0L) : lastEventCheckTime;

        final Date now = new Date();

        try {
            controlApi.throwContainerEvents(since, now);
            dockerServerService.update(dockerServer.updateEventCheckTime(now));

            return UpdateReport.singleton(UpdateReportEntry.success());
        } catch (NoDockerServerException e) {
            log.info("Cannot search for Docker container events. No Docker server defined.");
        } catch (DockerServerException e) {
            log.error("Cannot find Docker container events.", e);
        }
        return UpdateReport.singleton(UpdateReportEntry.failure());
    }

    @Nonnull
    private UpdateReport updateServices(final DockerServer dockerServer) {
        final UpdateReport report = UpdateReport.create();
        for (final Container service : containerService.retrieveNonfinalizedServices()) {
            // log.debug("Getting Task info for Service {}.", service.serviceId());
            try {
                controlApi.throwTaskEventForService(dockerServer, service);
                report.add(UpdateReportEntry.success(service.serviceId()));
            } catch (DockerServerException e) {
                log.error(String.format("Cannot get Tasks for Service %s.", service.serviceId()), e);
                report.add(UpdateReportEntry.failure(service.serviceId(), e.getMessage()));
            }
        }

        boolean allTrue = true;
        boolean allFalse = true;
        for (final UpdateReportEntry entry : report.updateReports) {
            allTrue = allTrue && entry.successful;
            allFalse = allFalse && !entry.successful;
        }

        // If either allTrue or allFalse is true, then allTrue will tell us if everything was successful or not.
        // If both allTrue and allFalse are false,, that means some were successful and some not. So we set overall report success to null.
        report.successful = allTrue || allFalse ? allTrue : null;

        return report;
    }

    private static class UpdateReport {
        private Boolean successful;
        private List<UpdateReportEntry> updateReports;

        private UpdateReport() {}

        public static UpdateReport create() {
            final UpdateReport report = new UpdateReport();
            report.updateReports = Lists.newArrayList();
            return report;
        }

        public static UpdateReport singleton(final UpdateReportEntry entry) {
            final UpdateReport report = new UpdateReport();
            report.updateReports = Collections.singletonList(entry);
            report.successful = entry.successful;
            return report;
        }

        public void add(final UpdateReportEntry entry) {
            updateReports.add(entry);
        }
    }

    private static class UpdateReportEntry {
        private Boolean successful;
        private String id;
        private String message;

        public static UpdateReportEntry success() {
            final UpdateReportEntry updateReportEntry = new UpdateReportEntry();
            updateReportEntry.successful = true;
            return updateReportEntry;
        }

        public static UpdateReportEntry success(final String id) {
            final UpdateReportEntry updateReportEntry = success();
            updateReportEntry.id = id;
            return updateReportEntry;
        }

        public static UpdateReportEntry failure() {
            final UpdateReportEntry updateReportEntry = new UpdateReportEntry();
            updateReportEntry.successful = false;
            return updateReportEntry;
        }

        public static UpdateReportEntry failure(final String id,
                                                final String message) {
            final UpdateReportEntry updateReportEntry = failure();
            updateReportEntry.id = id;
            updateReportEntry.message = message;
            return updateReportEntry;
        }
    }
}
