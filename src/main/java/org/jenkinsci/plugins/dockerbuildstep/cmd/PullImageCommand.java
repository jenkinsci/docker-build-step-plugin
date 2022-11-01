package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.model.AuthConfig;
import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.PullImageRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.CommandUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;

/**
 * This command pulls Docker image from a repository.
 *
 * @see <a href="https://docs.docker.com/engine/api/v1.41/#tag/Image/operation/ImageCreate">Create an image</a>
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
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, final ConsoleLogger console)
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
        
        try {
            Config cfgData = getConfig(build);
            Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
            AuthConfig authConfig = getAuthConfig(build.getParent());
            
            launcher.getChannel().call(new PullImageRemoteCallable(console.getListener(), cfgData, descriptor, authConfig, fromImageRes));
            console.logInfo("Done");
        } catch (Exception e) {
            console.logError("failed to pull image " + fromImageRes);
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
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
