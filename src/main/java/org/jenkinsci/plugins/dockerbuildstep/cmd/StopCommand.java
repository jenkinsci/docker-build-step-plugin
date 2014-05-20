package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

/**
 * This command stops one or more Docker containers.
 * 
 * @see http://docs.docker.io/en/master/api/docker_remote_api_v1.8/#stop-a-container
 * 
 * @author vjuranek
 * 
 */
public class StopCommand extends DockerCommand {

    private final String containerIds;

    @DataBoundConstructor
    public StopCommand(String containerIds) {
        this.containerIds = containerIds;
    }

    public String getContainerIds() {
        return containerIds;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        if (containerIds == null || containerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        String containerIdsRes = Resolver.buildVar(build, containerIds);
        
        List<String> ids = Arrays.asList(containerIdsRes.split(","));
        DockerClient client = getClient();
        //TODO check, if container is actually running
        for (String id : ids) {
            id = id.trim();
            client.stopContainer(id);
            console.logInfo("stopped container id " + id);
        }
    }

    @Extension
    public static class StopCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Stop container(s)";
        }
    }

}
