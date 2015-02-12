package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * This command starts all containers create from specified image ID. It also exports some build environment variable
 * like IP or started containers.
 * 
 * @author vjuranek
 * 
 */
public class StartByImageIdCommand extends DockerCommand {

    private final String imageId;

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

        String imageIdRes = Resolver.buildVar(build, imageId);
        
        DockerClient client = getClient(null);
        List<Container> containers = client.listContainersCmd().withShowAll(true).exec();
        for (Container c : containers) {
            if (imageIdRes.equalsIgnoreCase(c.getImage())) {
                client.startContainerCmd(c.getId());
                console.logInfo("started container id " + c.getId());

                InspectContainerResponse inspectResp = client.inspectContainerCmd(c.getId()).exec();
                EnvInvisibleAction envAction = new EnvInvisibleAction(inspectResp);
                build.addAction(envAction);
            }
        }
    }

    @Extension
    public static class StartByImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Start container(s) by image ID";
        }
    }

}
