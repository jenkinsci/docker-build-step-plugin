package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.DockerException;

/**
 * This command removes specified Docker container(s).
 * 
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#remove-a-container
 * 
 * @author vjuranek
 * 
 */
public class RemoveCommand extends DockerCommand {

    private final String containerIds;

    @DataBoundConstructor
    public RemoveCommand(String containerIds) {
        this.containerIds = containerIds;
    }

    public String getContainerIds() {
        return containerIds;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        // TODO check it when submitting the form
        if (containerIds == null || containerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
        
        String containerIdsRes = Resolver.buildVar(build, containerIds);

        List<String> ids = Arrays.asList(containerIdsRes.split(","));
        DockerClient client = getClient();
        for (String id : ids) {
            id = id.trim();
            client.execute(client.killContainerCmd(id));
        }
        for (String id : ids) {
            client.execute(client.removeContainerCmd(id));
            console.logInfo("removed container id " + id);
        }
    }

    @Extension
    public static class RemoveCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Remove container(s)";
        }
    }

}
