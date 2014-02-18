package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.Container;

/**
 * This command stops all containers create from specified image ID.
 * 
 * @author vjuranek
 *
 */
public class StopByImageIdCommand extends DockerCommand {

    private String imageId;

    @DataBoundConstructor
    public StopByImageIdCommand(String imageId) {
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        if (imageId == null || imageId.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        DockerClient client = getClient();
        List<Container> containers = getClient().listContainers(false);
        for (Container c : containers) {
            if (imageId.equalsIgnoreCase(c.getImage())) {
                client.stopContainer(c.getId());
                console.logInfo("stop container id " + c.getId());
            }
        }
    }

    @Extension
    public static class StartCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Stop constainer(s) by image ID";
        }
    }

}
