package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.Container;
import com.kpelykh.docker.client.model.ContainerInspectResponse;

/**
 * This command starts all containers create from specified image ID. It also exports some build environment variable
 * like IP or started containers.
 * 
 * @author vjuranek
 * 
 */
public class StartByImageIdCommand extends DockerCommand {

    private String imageId;

    @DataBoundConstructor
    public StartByImageIdCommand(String imageId) {
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
        List<Container> containers = getClient().listContainers(true);
        for (Container c : containers) {
            if (imageId.equalsIgnoreCase(c.getImage())) {
                client.startContainer(c.getId());
                console.logInfo("started container id " + c.getId());

                ContainerInspectResponse inspectResp = client.inspectContainer(c.getId());
                EnvInvisibleAction envAction = new EnvInvisibleAction(inspectResp);
                build.addAction(envAction);
            }
        }
    }

    @Extension
    public static class StartByImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Start constainer(s) by image ID";
        }
    }

}
