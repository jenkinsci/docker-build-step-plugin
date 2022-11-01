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
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.StopContainerRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;

/**
 * This command stops one or more Docker containers.
 * 
 * @see <a href="https://docs.docker.com/engine/api/v1.41/#tag/Container/operation/ContainerStop">Stop a container</a>
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
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        if (containerIds == null || containerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        String containerIdsRes = Resolver.buildVar(build, containerIds);
        List<String> ids = Arrays.asList(containerIdsRes.split(","));
        
        Config cfgData = getConfig(build);
        Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
        for (String id : ids) {
            id = id.trim();
            
            try {
                launcher.getChannel().call(new StopContainerRemoteCallable(cfgData, descriptor, id));
            } catch (Exception e) {
                console.logError("failed to stop container id " + id);
                e.printStackTrace();
                throw new IllegalArgumentException(e);
            }
            
            
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
