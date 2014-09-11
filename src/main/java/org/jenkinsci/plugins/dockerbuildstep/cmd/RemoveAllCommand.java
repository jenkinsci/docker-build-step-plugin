package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.model.Container;

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
        List<Container> containers = client.execute(client.listContainersCmd().withShowAll(true));
        for (Container container : containers) {
            client.execute(client.killContainerCmd(container.getId()));
            client.execute(client.removeContainerCmd((container.getId())));
            console.logInfo("removed container id " + container.getId());
        }
    }

    @Extension
    public static class RemoveAllCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Remove all containers";
        }
    }

}
