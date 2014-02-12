package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

public class RemoveCommand extends DockerCommand {

    private final String containerId;

    @DataBoundConstructor
    public RemoveCommand(String containerId) {
        this.containerId = containerId;
    }

    public String getContainerId() {
        return containerId;
    }

    @Override
    public void execute() throws DockerException {
        // TODO check it when submitting the form
        if (containerId == null || containerId.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
        DockerClient client = getClient();
        client.kill(containerId);
        client.removeContainer(containerId);
    }

    @Extension
    public static class RemoveCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Remove constainer";
        }
    }

}
