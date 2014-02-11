package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;

import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.Container;

public class StopAllCommand extends DockerCommand {

    @DataBoundConstructor
    public StopAllCommand() {
    }
    
    @Override
    public void execute() throws DockerException {
        List<Container> containers = getClient().listContainers(true);
        for(Container c : containers) {
            getClient().stopContainer(c.getId());
        }
    }

    @Extension
    public static class StopAllCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Stop all constainers";
        }
    }

}