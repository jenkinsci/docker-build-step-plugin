package org.jenkinsci.plugins.dockerbuildstep.cmd;

import java.util.Arrays;
import java.util.List;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

/**
 * This command restarts specified Docker container(s).
 * 
 * @see http://docs.docker.io/en/master/api/docker_remote_api_v1.8/#restart-a-container
 * 
 * @author vjuranek
 *
 */
public class RestartCommand extends DockerCommand {

    private String containerIds;
    private int timeout;

    @DataBoundConstructor
    public RestartCommand(String containerIds, int timeout) {
        this.containerIds = containerIds;
        this.timeout = timeout;
    }

    public String getContainerIds() {
        return containerIds;
    }

    public int getTimeout() {
        return timeout;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, BuildListener listener) throws DockerException {
        if (containerIds == null || containerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
        
        List<String> ids = Arrays.asList(containerIds.split(","));
        DockerClient client = getClient();
        for(String id : ids) {
            id = id.trim();
            client.restart(id, timeout);
        }
    }

    @Extension
    public static class RestartCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Restart constainer(s)";
        }
    }

}