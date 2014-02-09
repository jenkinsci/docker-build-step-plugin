package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerException;

public class RestartCommand extends DockerCommand {

    private String containerId;
    private int timeout;

    @DataBoundConstructor
    public RestartCommand(String containerId, int timeout) {
        this.containerId = containerId;
        this.timeout = timeout;
    }

    public String getContainerId() {
        return containerId;
    }

    public int getTimeout() {
        return timeout;
    }

    @Override
    public void execute() throws DockerException {
        if (containerId == null || containerId.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
        getClient().restart(containerId, timeout);
    }

    @Extension
    public static class RestartCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Restart constainer";
        }
    }

}