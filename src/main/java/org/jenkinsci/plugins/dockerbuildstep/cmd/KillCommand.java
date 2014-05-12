package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

/**
 * This command kills specified container(s).
 * 
 * @see http://docs.docker.io/en/master/api/docker_remote_api_v1.8/#kill-a-container
 * 
 * @author vjuranek
 * 
 */
public class KillCommand extends DockerCommand {

    private String containerIds;

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

        List<String> ids = Arrays.asList(containerIds.split(","));
        DockerClient client = getClient();
        for (String id : ids) {
            id = id.trim();
            client.kill(id);
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
