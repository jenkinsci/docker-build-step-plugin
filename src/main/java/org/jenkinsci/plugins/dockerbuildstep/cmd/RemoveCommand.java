package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.NotFoundException;

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
    private final boolean ignoreIfNotFound;

    @DataBoundConstructor
    public RemoveCommand(String containerIds, boolean ignoreIfNotFound) {
        this.containerIds = containerIds;
        this.ignoreIfNotFound = ignoreIfNotFound;
    }

    public String getContainerIds() {
        return containerIds;
    }

    public boolean getIgnoreIfNotFound() {
        return ignoreIfNotFound;
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
            try {
                client.killContainerCmd(id).exec();
            } catch (NotFoundException e) {
                if (!ignoreIfNotFound) {
                    throw e;
                }
            }
        }
        for (String id : ids) {
            try {
                client.removeContainerCmd(id).exec();
                console.logInfo("removed container id " + id);
            } catch (NotFoundException e) {
                console.logInfo("container not found " + id);
                if (!ignoreIfNotFound) {
                    throw e;
                }
            }
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
