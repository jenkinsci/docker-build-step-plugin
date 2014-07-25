package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.DockerException;
import com.github.dockerjava.client.model.Container;

/**
 * This command stops all containers create from specified image ID.
 * 
 * @author vjuranek
 *
 */
public class StopByImageIdCommand extends DockerCommand {

    private final String imageId;

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

        String imageIdRes = Resolver.buildVar(build, imageId);
        
        DockerClient client = getClient();
        List<Container> containers = client.execute(client.listContainersCmd());
        for (Container c : containers) {
            if (imageIdRes.equalsIgnoreCase(c.getImage())) {
                client.execute(client.stopContainerCmd(c.getId()));
                console.logInfo("stop container id " + c.getId());
            }
        }
    }

    @Extension
    public static class StopByImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Stop container(s) by image ID";
        }
    }

}
