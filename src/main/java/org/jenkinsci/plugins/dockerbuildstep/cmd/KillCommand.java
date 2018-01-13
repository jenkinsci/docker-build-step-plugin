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
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.KillContainerRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;

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
                launcher.getChannel().call(new KillContainerRemoteCallable(cfgData, descriptor, id));
            	console.logInfo("killed container id " + id);
            } catch (Exception e) {
                console.logError("failed to kill containers " + ids);
                e.printStackTrace();
                throw new IllegalArgumentException(e);
            }
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
