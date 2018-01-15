package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.ListContainersRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.StartContainerRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * This command starts all containers create from specified image ID. It also exports some build environment variable
 * like IP or started containers.
 * 
 * @author vjuranek
 * 
 */
public class StartByImageIdCommand extends DockerCommand {

    private final String imageId;

    @DataBoundConstructor
    public StartByImageIdCommand(String imageId) {
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
            List<Container> containers = launcher.getChannel().call(new ListContainersRemoteCallable(cfgData, descriptor, true));
            
            for (Container container : containers) {
                if (imageIdRes.equalsIgnoreCase(container.getImage())) {
                    String inspectRespSerialized = launcher.getChannel().call(new StartContainerRemoteCallable(cfgData, descriptor, container.getId()));
                    
                    ObjectMapper mapper = new ObjectMapper();
                    InspectContainerResponse inspectResp = mapper.readValue(inspectRespSerialized, InspectContainerResponse.class);
                    
                    console.logInfo("started container id " + container.getId());
                    EnvInvisibleAction envAction = new EnvInvisibleAction(inspectResp);
                    build.addAction(envAction);
                }
            }
        } catch (Exception e) {
            console.logError("failed to start container by image id " + imageIdRes);
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }

    @Extension
    public static class StartByImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Start container(s) by image ID";
        }
    }
    
}
