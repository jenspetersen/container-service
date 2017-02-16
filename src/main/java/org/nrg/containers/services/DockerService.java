package org.nrg.containers.services;

import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.exceptions.NotUniqueException;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.auto.DockerImage;
import org.nrg.containers.model.DockerHubEntity;
import org.nrg.containers.model.auto.DockerImageAndCommandSummary;
import org.nrg.containers.model.DockerServer;
import org.nrg.prefs.exceptions.InvalidPreferenceName;

import java.util.List;

public interface DockerService {
    List<DockerHubEntity> getHubs();
    DockerHubEntity setHub(DockerHubEntity hub);
    String pingHub(Long hubId) throws DockerServerException, NoServerPrefException, NotFoundException;
    String pingHub(String hubName) throws DockerServerException, NoServerPrefException, NotUniqueException;
    DockerImage pullFromHub(long hubId, String imageName, boolean saveCommands)
            throws DockerServerException, NoServerPrefException, NotFoundException;
    DockerImage pullFromHub(String hubName, String imageName, boolean saveCommands)
            throws DockerServerException, NoServerPrefException, NotFoundException;
    DockerImage pullFromHub(String imageName, boolean saveCommands)
            throws DockerServerException, NoServerPrefException, NotFoundException;

    DockerServer getServer() throws NotFoundException;
    DockerServer setServer(DockerServer server) throws InvalidPreferenceName;
    String pingServer() throws NoServerPrefException, DockerServerException;

    List<DockerImage> getImages() throws NoServerPrefException, DockerServerException;
    List<DockerImageAndCommandSummary> getImageSummaries() throws NoServerPrefException, DockerServerException;
    DockerImage getImage(String imageId) throws NoServerPrefException, NotFoundException;
    void removeImage(String imageId, Boolean force) throws NotFoundException, NoServerPrefException, DockerServerException;
    List<Command> saveFromImageLabels(String imageName) throws DockerServerException, NotFoundException, NoServerPrefException;
}
