package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PushImageResultCallback;
import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.CommandUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This command pulls Docker image from a repository.
 *
 * @author wzheng2310@gmail.com (Wei Zheng)
 * @see https://docs.docker.com/reference/api/docker_remote_api_v1.13/#push-an-image-on-the-registry
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
    public void execute(Launcher launcher, AbstractBuild build, final ConsoleLogger console) throws DockerException,
            AbortException {
        if (!StringUtils.isNotBlank(image)) {
            throw new IllegalArgumentException("Image name must be provided");
        }

        // Don't include tag in the image name. Docker daemon can't handle it.
        // put tag in query string parameter.
        String imageRes = CommandUtils.imageFullNameFrom(
                Resolver.buildVar(build, registry),
                Resolver.buildVar(build, image),
                Resolver.buildVar(build, tag));

        console.logInfo("Pushing image " + imageRes);
        final DockerClient client = getClient(build, getAuthConfig(build.getParent()));
        PushImageCmd pushImageCmd = client.pushImageCmd(imageRes).withTag(
                Resolver.buildVar(build, tag));
        PushImageResultCallback callback = new PushImageResultCallback() {
            @Override
            public void onNext(PushResponseItem item) {
                console.logInfo(item.toString());
                super.onNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                console.logError("Failed to push image:"+throwable.getMessage());
                super.onError(throwable);
            }
        };
        pushImageCmd.exec(callback).awaitSuccess();
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
