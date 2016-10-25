package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
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
import com.github.dockerjava.api.exception.DockerException;
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
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build,final ConsoleLogger console)
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
        DockerClient client = getClient(build, getAuthConfig(build.getParent()));
        PullImageCmd pullImageCmd = client.pullImageCmd(fromImageRes);
        PullImageResultCallback callback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                console.logInfo(item.toString());
                super.onNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                console.logError("Failed to pulling image"+throwable.getMessage());
                super.onError(throwable);
            }
        };
        pullImageCmd.exec(callback).awaitSuccess();
        console.logInfo("Done");
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
