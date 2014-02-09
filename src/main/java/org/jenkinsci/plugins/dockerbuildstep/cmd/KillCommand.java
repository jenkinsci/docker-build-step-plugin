package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerException;

public class KillCommand extends DockerCommand {

    private String containerId;

    @DataBoundConstructor
    public KillCommand(String containerId) {
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
        getClient().kill(containerId);
    }

    @Extension
    public static class KillCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Kill container";
        }
    }

}