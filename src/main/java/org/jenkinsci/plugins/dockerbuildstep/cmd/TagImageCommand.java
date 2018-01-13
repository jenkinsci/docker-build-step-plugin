package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.TagImageRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This command tags the specified Docker image.
 *
 * @author draoullig
 * @see https://docs.docker.com/reference/api/docker_remote_api_v1.19/#tag-an-image-into-a-repository
 */
public class TagImageCommand extends DockerCommand {

    private final String image;
    private final String repository;
    private final String tag;
    private final boolean ignoreIfNotFound;
    private final boolean withForce;

    @DataBoundConstructor
    public TagImageCommand(final String image, final String repository, final String tag,
                           final boolean ignoreIfNotFound, final boolean withForce) {
        this.image = image;
        this.repository = repository;
        this.tag = tag;
        this.ignoreIfNotFound = ignoreIfNotFound;
        this.withForce = withForce;
    }

    public String getImage() {
        return image;
    }

    public String getRepository() {
        return repository;
    }

    public String getTag() {
        return tag;
    }

    public boolean getIgnoreIfNotFound() {
        return ignoreIfNotFound;
    }

    public boolean getWithForce() {
        return withForce;
    }

    @Override
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build,
                        ConsoleLogger console) throws DockerException {
        // TODO check it when submitting the form
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please provide an image name");
        } else if (repository == null || repository.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please provide a repository");
        } else if (tag == null || tag.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please provide a tag for the image");
        }

        final String imageRes = Resolver.buildVar(build, image);
        final String repositoryRes = Resolver.buildVar(build, repository);
        final String tagRes = Resolver.buildVar(build, tag);

        console.logInfo("start tagging image " + imageRes + " in " + repositoryRes + " as " + tagRes);
        
        try {
            Config cfgData = getConfig(build);
            Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
            
            launcher.getChannel().call(new TagImageRemoteCallable(cfgData, descriptor, imageRes, repositoryRes, tagRes, withForce));
        } catch (NotFoundException e) {
            if (!ignoreIfNotFound) {
                console.logError(String.format("image '%s' not found ",
                        imageRes));
                throw e;
            } else {
                console.logInfo(String
                        .format("image '%s' not found, but skipping this error is turned on, let's continue ... ",
                                imageRes));
            }
        } catch (Exception e) {
            console.logError("Failed to tag image: " + imageRes + ". Error: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
        
        console.logInfo("Tagged image " + imageRes + " in " + repositoryRes + " as " + tagRes);
    }

    @Extension
    public static class RemoveImageCommandDescriptor extends
            DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Tag image";
        }
    }

}
