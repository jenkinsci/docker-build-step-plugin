package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.ListContainersRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.RemoveContainerRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;

/**
 * This command removes all Docker containers. Before removing them, it kills all them in case some of them are running.
 * 
 * @author vjuranek
 * 
 */
public class RemoveAllCommand extends DockerCommand {

    private final boolean removeVolumes;
    private final boolean force;

    @DataBoundConstructor
    public RemoveAllCommand(boolean removeVolumes, boolean force) {
        this.removeVolumes = removeVolumes;
        this.force = force;
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
        
        
        try {
            Config cfgData = getConfig(build);
            Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
            
            List<Container> containers = launcher.getChannel().call(new ListContainersRemoteCallable(cfgData, descriptor, false));
            
            for (Container container : containers) {
                launcher.getChannel().call(new RemoveContainerRemoteCallable(cfgData, descriptor, container.getId(), force, removeVolumes));
                console.logInfo("removed container id " + container.getId());
            }
        } catch (Exception e) {
            console.logError("failed to stop all containers");
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
        
    }

    @Extension
    public static class RemoveAllCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Remove all containers";
        }
    }

}
