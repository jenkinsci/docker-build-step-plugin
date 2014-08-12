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
 * This command kills specified container(s).
 * 
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#kill-a-container
 * 
 * @author vjuranek
 * 
 */
public class KillCommand extends DockerCommand {

    private final String containerIds;

    @DataBoundConstructor
    public KillCommand(String containerIds) {
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
        for (String id : ids) {
            id = id.trim();
            client.execute(client.killContainerCmd(id));
            console.logInfo("killed container id " + id);
        }
    }

    @Extension
    public static class KillCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Kill container(s)";
        }
    }

}
