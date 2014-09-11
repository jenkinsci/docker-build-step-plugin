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

/**
 * This command restarts specified Docker container(s).
 * 
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#restart-a-container
 * 
 * @author vjuranek
 *
 */
public class RestartCommand extends DockerCommand {

    private final String containerIds;
    private final int timeout;

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
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console) throws DockerException {
        if (containerIds == null || containerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
        
        String containerIdsRes = Resolver.buildVar(build, containerIds);
        
        List<String> ids = Arrays.asList(containerIdsRes.split(","));
        DockerClient client = getClient();
        for(String id : ids) {
            id = id.trim();
            client.execute(client.restartContainerCmd(id).withtTimeout(timeout));
            console.logInfo("restrted conatiner id " + id);
        }
    }

    @Extension
    public static class RestartCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Restart container(s)";
        }
    }

}
