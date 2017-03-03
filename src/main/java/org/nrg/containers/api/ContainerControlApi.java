package org.nrg.containers.api;

import org.nrg.containers.events.DockerContainerEvent;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.auto.DockerImage;
import org.nrg.containers.model.DockerServer;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ContainerControlApi {
    DockerServer getServer() throws NoServerPrefException;
    DockerServer setServer(String host, String certPath) throws InvalidPreferenceName;
    DockerServer setServer(DockerServer server) throws InvalidPreferenceName;
    void setServer(String host) throws InvalidPreferenceName;
    String pingServer() throws NoServerPrefException, DockerServerException;
    boolean canConnect();

    String pingHub(DockerHub hub) throws DockerServerException, NoServerPrefException;

    List<DockerImage> getAllImages() throws NoServerPrefException, DockerServerException;
    DockerImage getImageById(final String imageId) throws NotFoundException, DockerServerException, NoServerPrefException;
    void deleteImageById(String id, Boolean force) throws NoServerPrefException, DockerServerException;

    DockerImage pullImage(String name) throws NoServerPrefException, DockerServerException;
    DockerImage pullImage(String name, DockerHub hub) throws NoServerPrefException, DockerServerException;

    String launchImage(final ResolvedDockerCommand dockerCommand) throws NoServerPrefException, DockerServerException;
//    String launchImage(final String imageName, final List<String> runCommand, final List <String> volumes) throws NoServerPrefException, DockerServerException;
//    String launchImage(final DockerServer server, final String imageName,
//                       final List<String> runCommand, final List <String> volumes) throws DockerServerException;
//    String launchImage(final DockerServer server, final String imageName,
//                       final List<String> runCommand, final List <String> volumes,
//                       final List<String> environmentVariables) throws DockerServerException;

    List<Command> parseLabels(final String imageName)
            throws DockerServerException, NoServerPrefException, NotFoundException;

    List<Container> getAllContainers() throws NoServerPrefException, DockerServerException;
    List<Container> getContainers(final Map<String, String> params) throws NoServerPrefException, DockerServerException;
    Container getContainer(final String id) throws NotFoundException, NoServerPrefException, DockerServerException;
    String getContainerStatus(final String id) throws NotFoundException, NoServerPrefException, DockerServerException;
    String getContainerStdoutLog(String id) throws NoServerPrefException, DockerServerException;
    String getContainerStderrLog(String id) throws NoServerPrefException, DockerServerException;

    List<DockerContainerEvent> getContainerEvents(final Date since, final Date until) throws NoServerPrefException, DockerServerException;
    List<DockerContainerEvent> getContainerEventsAndThrow(final Date since, final Date until) throws NoServerPrefException, DockerServerException;

    void killContainer(final String id) throws NoServerPrefException, DockerServerException, NotFoundException;
}
