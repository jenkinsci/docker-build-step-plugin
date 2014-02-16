package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.Container;

/**
 * This command removes all Docker containers. Before removing them, it kills all them in case some of them are running.
 * 
 * @author vjuranek
 * 
 */
public class RemoveAllCommand extends DockerCommand {

    @DataBoundConstructor
    public RemoveAllCommand() {
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        DockerClient client = getClient();
        List<Container> conatiners = client.listContainers(true);
        for (Container container : conatiners) {
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