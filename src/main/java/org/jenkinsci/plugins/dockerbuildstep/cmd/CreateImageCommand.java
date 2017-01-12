package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;

/**
 * This command creates a new image from specified Dockerfile.
 *
 * @author marcus
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#build-an-image-from-dockerfile-via-stdin
 */
public class CreateImageCommand extends DockerCommand {

    private final String dockerFolder;
    private final String imageTag;
    private final String dockerFile;
    private final boolean noCache;
    private final boolean rm;

    @DataBoundConstructor
    public CreateImageCommand(String dockerFolder, String imageTag, String dockerFile, boolean noCache, boolean rm) {
        this.dockerFolder = dockerFolder;
        this.imageTag = imageTag;
        this.dockerFile = dockerFile;
        this.noCache = noCache;
        this.rm = rm;
    }

    public String getDockerFolder() {
        return dockerFolder;
    }

    public String getImageTag() {
        return imageTag;
    }

    public String getDockerFile() {
        return dockerFile;
    }

    public boolean isNoCache() {
        return noCache;
    }

    public boolean isRm() {
        return rm;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, final ConsoleLogger console)
            throws DockerException {

        if (dockerFolder == null) {
            throw new IllegalArgumentException("dockerFolder is not configured");
        }

        if (imageTag == null) {
            throw new IllegalArgumentException("imageTag is not configured");
        }

        String dockerFolderRes = Resolver.buildVar(build, dockerFolder);
        String imageTagRes = Resolver.buildVar(build, imageTag);

        String expandedDockerFolder = Resolver.buildVar(build, dockerFolderRes);

        String expandedImageTag = Resolver.buildVar(build, imageTagRes);

        FilePath folder = new FilePath(new File(expandedDockerFolder));

        if (!exist(folder))
            throw new IllegalArgumentException(
                    "configured dockerFolder '" + expandedDockerFolder + "' does not exist.");

        String dockerFileRes = dockerFile == null ? "Dockerfile" : Resolver.buildVar(build, dockerFile);
        if (!exist(folder.child(dockerFileRes))) {
            throw new IllegalArgumentException(
                    String.format("Configured Docker file '%s' does not exist.", dockerFileRes));
        }

        DockerClient client = getClient(build, null);

        try {
            File docker = new File(expandedDockerFolder, dockerFileRes);
            console.logInfo("Creating docker image from " + docker.getAbsolutePath());
            BuildImageResultCallback callback = new BuildImageResultCallback() {
                @Override
                public void onNext(BuildResponseItem item) {
                    console.logInfo(item.toString());
                    super.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    console.logError("Failed to creating docker image:" + throwable.getMessage());
                    super.onError(throwable);
                }
            };
            BuildImageResultCallback result = client.buildImageCmd(docker).withTag(expandedImageTag)
                    .withNoCache(noCache).withRemove(rm).exec(callback);
            console.logInfo("Build image id:" + result.awaitImageId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Extension
    public static class CreateImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Create/build image";
        }
    }

    private boolean exist(FilePath filePath) throws DockerException {
        try {
            return filePath.exists();
        } catch (Exception e) {
            throw new DockerException("Could not check file", 0, e);
        }
    }

}
