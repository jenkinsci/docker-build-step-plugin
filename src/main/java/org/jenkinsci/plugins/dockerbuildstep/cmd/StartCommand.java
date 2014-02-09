package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerException;

public class StartCommand extends DockerCommand {

    private String containerId;

    @DataBoundConstructor
    public StartCommand(String containerId) {
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
        getClient().startContainer(containerId);
    }

    @Extension
    public static class StartCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Start constainer";
        }
    }

}
