package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerException;

public class StopCommand extends DockerCommand {

    private String containerId;

    @DataBoundConstructor
    public StopCommand(String containerId) {
        this.containerId = containerId;
    }

    public String getContainerId() {
        return containerId;
    }

    @Override
    public void execute() throws DockerException {
        if (containerId == null || containerId.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
        getClient().stopContainer(containerId);
    }

    @Extension
    public static class StopCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Stop constainer";
        }
    }

}