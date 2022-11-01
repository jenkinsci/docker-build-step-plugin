package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.io.IOException;

import java.net.HttpURLConnection;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.SaveImageRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;

/**
 * This command saves the specified Docker image.
 * 
 * @see <a href="https://docs.docker.com/engine/reference/commandline/save/">docker save</a>
 * 
 * @author draoullig
 * 
 */
public class SaveImageCommand extends DockerCommand {

    private final String imageName;
    private final String imageTag;
    private final String destination;
    private final String filename;
    private final boolean ignoreIfNotFound;

    @DataBoundConstructor
    public SaveImageCommand(final String imageName, final String imageTag,
            final String destination, final String filename,
            final boolean ignoreIfNotFound) {

        this.imageName = imageName;
        this.imageTag = imageTag;
        this.destination = destination;
        this.filename = filename;
        this.ignoreIfNotFound = ignoreIfNotFound;
    }

    public String getImageName() {
        return imageName;
    }

    public String getImageTag() {
        return imageTag;
    }

    public String getDestination() {
        return destination;
    }

    public String getFilename() {
        return filename;
    }

    public boolean getIgnoreIfNotFound() {
        return ignoreIfNotFound;
    }

    @Override
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build,
            ConsoleLogger console) throws DockerException {

        if (imageName == null || imageName.isEmpty()) {
            throw new IllegalArgumentException("Image Name is not configured");
        }

        if (imageTag == null || imageTag.isEmpty()) {
            throw new IllegalArgumentException("Image Tag is not configured");
        }

        if (destination == null || destination.isEmpty()) {
            throw new IllegalArgumentException(
                    "Folder Destination is not configured");
        }

        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename is not configured");
        }
        
        final String imageNameRes = Resolver.buildVar(build, imageName);
        final String imageTagRes = Resolver.buildVar(build, imageTag);
        final String destinationRes = Resolver.buildVar(build, destination);
        final String filenameRes = Resolver.buildVar(build, filename);
        
        console.logInfo(String.format("Started save image '%s' ... ", imageNameRes + " " + imageTagRes));
        
        try {
            Config cfgData = getConfig(build);
            Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
            
            launcher.getChannel().call(new SaveImageRemoteCallable(cfgData, descriptor, destinationRes, filenameRes, imageNameRes, imageTagRes));
            
            console.logInfo("Finished save image " + imageNameRes + " " + imageTagRes);
        } catch (NotFoundException e) {
            if (!ignoreIfNotFound) {
                console.logError(String.format("image '%s' not found ",
                        imageNameRes + " " + imageTagRes));
                throw e;
            } else {
                console.logInfo(String
                        .format("image '%s' not found, but skipping this error is turned on, let's continue ... ",
                                imageNameRes + " " + imageTagRes));
            }
        } catch (IOException e) {
            console.logError(String.format("Error to save '%s' ", imageNameRes
                    + " " + imageTagRes)
                    + " " + e.getLocalizedMessage());
            throw new DockerException(
                    String.format("Error to save '%s' ", imageNameRes + " "
                            + imageTagRes)
                            + " " + e.getLocalizedMessage(),
                    HttpURLConnection.HTTP_INTERNAL_ERROR);
        } catch (Exception e) {
            console.logError("failed to save image " + imageNameRes);
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }
    
    @Extension
    public static class RemoveImageCommandDescriptor extends
            DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Save image";
        }
    }

}
