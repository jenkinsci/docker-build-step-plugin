package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.AbstractBuild;

import java.io.InputStream;
import java.util.List;

import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.CommandUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.Image;

/**
 * This command pulls Docker image from a repository.
 * 
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#create-an-image
 * 
 * @author vjuranek
 * 
 */
public class PullImageCommand extends DockerCommand {

    private final String fromImage;
    private final String tag;
    private final String registry;

    @DataBoundConstructor
    public PullImageCommand(String fromImage, String tag, String registry, DockerRegistryEndpoint dockerRegistryEndpoint) {
        super(dockerRegistryEndpoint);
        this.fromImage = fromImage;
        this.tag = tag;
        this.registry = registry;
    }

    public String getFromImage() {
        return fromImage;
    }

    public String getTag() {
        return tag;
    }

    public String getRegistry() {
        return registry;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException, AbortException {
        // TODO check it when submitting the form
        if (fromImage == null || fromImage.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        // Docker daemon doesn't handle pull image command correctly when
        // PullImageCmd.withRegistry() is used. When a different registry
        // other than the default docker hub one is used with PullImageCmd.withRegistry(),
        // it still goes to the default docker hub registry.
        // This seems to be a bug in docker daemon's implementation.
        // So we construct full image path/name here as a workaround.
        String fromImageRes = CommandUtils.imageFullNameFrom(Resolver.buildVar(build, registry),
                Resolver.buildVar(build, fromImage), Resolver.buildVar(build, tag));

        console.logInfo("Pulling image " + fromImageRes);
        DockerClient client = getClient(getAuthConfig(build.getParent()));
        PullImageCmd pullImageCmd = client.pullImageCmd(fromImageRes);
        InputStream inputStream = pullImageCmd.exec();
        CommandUtils.logCommandResult(inputStream, console, "Failed to parse docker response when pulling image");

        // wait for the image to be downloaded
        final int loopMaxCount = 3;
        int loopCount = 0;
        // When the above InputStream finishes, the image should have already been
        // downloaded, so we don't need to infinitely loop and check if the image
        // has been pulled. Only a few iterations are good enough.
        while (!isImagePulled(fromImageRes)) {
            try {
                if (++loopCount > loopMaxCount) {
                    throw new DockerException("Can't find downloaded image " + fromImageRes, 200);
                }
                Thread.sleep(15 * 1000); // wait 15 sec
            } catch (InterruptedException e) {
                // TODO log
                throw new AbortException("Download of Docker image name " + fromImageRes + " was interrupted");
            }
        }

        console.logInfo("Done");
    }

    private boolean isImagePulled(String fromImageRes) throws DockerException {
        DockerClient client = getClient(null);
        // As of December 17, 2014, Docker list image command only support
        // one filter: dangling (true or fals).
        // See https://docs.docker.com/reference/commandline/cli/#filtering_1
        // So there is no way to specify the image in the list image command.
        List<Image> images = client.listImagesCmd().exec();

        String imageWithLatestTagIfNeeded = CommandUtils.addLatestTagIfNeeded(fromImageRes);
        for (Image img : images) {
            for (String repoTag : img.getRepoTags()) {
                if (imageWithLatestTagIfNeeded.equals(repoTag)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Extension
    public static class PullImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Pull image";
        }

        @Override
        public boolean showCredentials() {
            return true;
        }
    }
}
