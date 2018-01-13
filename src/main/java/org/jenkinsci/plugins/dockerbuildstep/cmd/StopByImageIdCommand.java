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
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.StopContainerRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;

/**
 * This command stops all containers create from specified image ID.
 * 
 * @author vjuranek
 *
 */
public class StopByImageIdCommand extends DockerCommand {

    private final String imageId;

    @DataBoundConstructor
    public StopByImageIdCommand(String imageId) {
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        if (imageId == null || imageId.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        String imageIdRes = Resolver.buildVar(build, imageId);
        
        try {
            Config cfgData = getConfig(build);
            Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
            
            List<Container> containers = launcher.getChannel().call(new ListContainersRemoteCallable(cfgData, descriptor, false));
            for (Container container : containers) {
                if (imageIdRes.equalsIgnoreCase(container.getImage())) {
                    launcher.getChannel().call(new StopContainerRemoteCallable(cfgData, descriptor, container.getId()));
                    console.logInfo("stoped container id " + container.getId());
                }
            }
        } catch (Exception e) {
            console.logError("failed to stop containers by imageIdRes " + imageIdRes);
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }

    @Extension
    public static class StopByImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Stop container(s) by image ID";
        }
    }
    
}
