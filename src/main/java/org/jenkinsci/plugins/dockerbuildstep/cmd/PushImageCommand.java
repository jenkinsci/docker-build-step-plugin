package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.model.AuthConfig;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.CommandUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

import java.io.InputStream;

import hudson.AbortException;
import hudson.model.AbstractBuild;

/**
 * This command pulls Docker image from a repository.
 * 
 * @see https://docs.docker.com/reference/api/docker_remote_api_v1.13/#push-an-image-on-the-registry
 * 
 * @author wzheng2310@gmail.com (Wei Zheng)
 * 
 */
public class PushImageCommand extends DockerCommand {
    private final String image;
    private final String tag;
    private final String registry;

    @DataBoundConstructor
    public PushImageCommand(String image, String tag, String registry,
        DockerRegistryEndpoint dockerRegistryEndpoint) {
        super(dockerRegistryEndpoint);
        this.image = image;
        this.tag = tag;
        this.registry = registry;
    }

    public String getImage() {
        return image;
    }

    public String getTag() {
        return tag;
    }

    public String getRegistry() {
        return registry;
    }

    @Override
    public void execute(AbstractBuild build, ConsoleLogger console) throws DockerException,
        AbortException {
        if (!StringUtils.isNotBlank(image)) {
            throw new IllegalArgumentException("Image name must be provided");
        }
  
        // Don't include tag in the image name. Docker daemon can't handle it.
        // put tag in query string parameter.
        String imageRes = CommandUtils.imageFullNameFrom(
            Resolver.buildVar(build, registry),
            Resolver.buildVar(build, image),
            null);

        console.logInfo("Pushing image " + imageRes);
        DockerClient client = getClient(getAuthConfig(build.getParent()));
        PushImageCmd pushImageCmd = client.pushImageCmd(imageRes).withTag(
                Resolver.buildVar(build, tag));

        InputStream inputStream = pushImageCmd.exec();
        CommandUtils.logCommandResult(inputStream, console,
                "Failed to parse docker response when push image");

        // Why the code doesn't verify now if the image has been pushed to the
        // registry/repository:
        // 1. search image command doesn't support auth yet, so there is no way to
        //    see if images have been pushed successfully by examining the repository,
        //    if the repository is a private one.
        // 2. If the registry is a private one, it is not searchable, because docker
        //    search is for docker hub only.
        // 3. Even if the docker hub repository is public, I am not sure how fast
        //    that docker hub will index the newly pushed image and make it searchable.
        //
        // Another option to verify is to do a pull after push.
        // But since the image is already available locally when you do a push,
        // a pull isn't a very good idea (let's say the image in the repository
        // is stale and you want to update it by a push,
        // but the push for some reason failed but the InputStream returned by push
        // command doesn't show any error. If you do a pull now, and if the pull succeeds,
        // you will override your local fresh image with a stale version.

        console.logInfo("Done pushing image " + imageRes);
    }

    @Extension
    public static class PushImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Push image";
        }

        @Override
        public boolean showCredentials() {
            return true;
        }
    }
}
