package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;

import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.Container;

public class RemoveAllCommand extends DockerCommand {

    @DataBoundConstructor
    public RemoveAllCommand() {
    }

    @Override
    public void execute() throws DockerException {
        DockerClient client = getClient();
        List<Container> conatiners = client.listContainers(true);
        for(Container container : conatiners) {
            client.kill(container.getId());
            client.removeContainer(container.getId());
        }
    }

    @Extension
    public static class RemoveAllCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Remove all constainers";
        }
    }

}