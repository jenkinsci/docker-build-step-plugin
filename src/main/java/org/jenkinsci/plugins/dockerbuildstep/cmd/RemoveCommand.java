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
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.RemoveContainerRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;

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
    private final boolean removeVolumes;
    private final boolean force;

    @DataBoundConstructor
    public RemoveCommand(String containerIds, boolean ignoreIfNotFound, boolean removeVolumes, boolean force) {
        this.containerIds = containerIds;
        this.ignoreIfNotFound = ignoreIfNotFound;
        this.removeVolumes = removeVolumes;
        this.force = force;
    }

    public String getContainerIds() {
        return containerIds;
    }

    public boolean getIgnoreIfNotFound() {
        return ignoreIfNotFound;
    }

    public boolean isRemoveVolumes() {
        return removeVolumes;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        // TODO check it when submitting the form
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
                try {
                    launcher.getChannel().call(new RemoveContainerRemoteCallable(cfgData, descriptor, id, force, removeVolumes));
                    console.logInfo("removed container id " + id);
                } catch (NotFoundException e) {
                    if (!ignoreIfNotFound) {
                        console.logError(String.format("container '%s' not found ", id));
                        throw e;
                    } else {
                        console.logInfo(String.format(
                                "container '%s' not found, but skipping this error is turned on, let's continue ... ", id));
                    }
                }
            }
        } catch (Exception e) {
            console.logError("failed to remove containers " + ids);
            e.printStackTrace();
            throw new IllegalArgumentException(e);
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
