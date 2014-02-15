package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.util.Arrays;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

/**
 * This command removes specified Docker container(s).
 * 
 * @see http://docs.docker.io/en/master/api/docker_remote_api_v1.8/#remove-a-container
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
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, BuildListener listener)
            throws DockerException {
        // TODO check it when submitting the form
        if (containerIds == null || containerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        List<String> ids = Arrays.asList(containerIds.split(","));
        DockerClient client = getClient();
        for (String id : ids) {
            id = id.trim();
            client.kill(id);
        }
        client.removeContainers(ids, false);
    }

    @Extension
    public static class RemoveCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Remove constainer(s)";
        }
    }

}
