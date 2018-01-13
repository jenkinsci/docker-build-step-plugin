package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.RemoveImageRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;

/**
 * This command removes specified Docker image.
 * 
 * @see http 
 *      ://docs.docker.com/reference/api/docker_remote_api_v1.13/#remove-an-image
 * 
 * @author draoullig
 * 
 */
public class RemoveImageCommand extends DockerCommand {

    private final String imageName;
    private final String imageId;
    private final boolean ignoreIfNotFound;

    @DataBoundConstructor
    public RemoveImageCommand(final String imageName, final String imageId,
            final boolean ignoreIfNotFound) {
        this.imageName = imageName;
        this.imageId = imageId;
        this.ignoreIfNotFound = ignoreIfNotFound;
    }

    public String getImageName() {
        return imageName;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean getIgnoreIfNotFound() {
        return ignoreIfNotFound;
    }

    @Override
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build,
            ConsoleLogger console) throws DockerException {
        // TODO check it when submitting the form
        if (imageName == null || imageName.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one parameter is required");
        }
        
        final String imageNameRes = Resolver.buildVar(build, imageName);
        final String imageIdRes = Resolver.buildVar(build, imageId);
        
        final String logInformation;
        if (imageIdRes == null || imageIdRes.isEmpty()) {
            logInformation = "image " + imageNameRes;
        } else {
            logInformation = "image " + imageNameRes + " with id " + imageIdRes;
        }
        
        try {
            Config cfgData = getConfig(build);
            Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
            
            launcher.getChannel().call(new RemoveImageRemoteCallable(cfgData, descriptor, imageNameRes, imageIdRes));
            console.logInfo("Removed " + logInformation);
        } catch (NotFoundException e) {
            if (!ignoreIfNotFound) {
                console.logError(String.format("image '%s' not found ",
                        imageNameRes + " with id " + imageIdRes));
                throw e;
            } else {
                console.logInfo(String
                        .format("image '%s' not found, but skipping this error is turned on, let's continue ... ",
                                imageNameRes + " with id " + imageIdRes));
            }
        } catch (Exception e) {
            console.logError("failed to remove " + logInformation);
            
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }

    }
    
    @Extension
    public static class RemoveImageCommandDescriptor extends
            DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Remove image";
        }
    }

}
