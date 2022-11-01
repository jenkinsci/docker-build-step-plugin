package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.RestartContainerRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;

/**
 * This command restarts specified Docker container(s).
 * 
 * @see <a href="https://docs.docker.com/engine/api/v1.41/#tag/Container/operation/ContainerRestart">Restart a container</a>
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
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console) throws DockerException {
        if (containerIds == null || containerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
        
        String containerIdsRes = Resolver.buildVar(build, containerIds);
        List<String> ids = Arrays.asList(containerIdsRes.split(","));
        
        try {
            Config cfgData = getConfig(build);
            Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
            
            for (String id : ids) {
                id = id.trim();
                launcher.getChannel().call(new RestartContainerRemoteCallable(cfgData, descriptor, id, timeout));
                console.logInfo("restarted container id " + id);
            }
        } catch (Exception e) {
            console.logError("failed to restart containers ids " + ids);
            e.printStackTrace();
            throw new IllegalArgumentException(e);
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
