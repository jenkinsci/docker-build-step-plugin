package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.remoting.Callable;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
    private final String buildArgs;
    
    @DataBoundConstructor
    public CreateImageCommand(String dockerFolder, String imageTag, String dockerFile, boolean noCache, boolean rm, String buildArgs) {
        this.dockerFolder = dockerFolder;
        this.imageTag = imageTag;
        this.dockerFile = dockerFile;
        this.noCache = noCache;
        this.buildArgs = buildArgs;
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

    public String getBuildArgs() {
        return buildArgs;
    }

    @Override
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, final ConsoleLogger console)
            throws DockerException {

        if (dockerFolder == null) {
            throw new IllegalArgumentException("dockerFolder is not configured");
        }

        if (imageTag == null) {
            throw new IllegalArgumentException("imageTag is not configured");
        }
        final Map<String, String> buildArgsMap = new HashMap<String, String>();

        if ((buildArgs != null) && (!buildArgs.trim().isEmpty())) {
            console.logInfo("Parsing buildArgs: " + buildArgs);
            String[] split = Resolver.buildVar(build, buildArgs).split(",|;");
            for (String arg : split) {
                String[] pair = arg.split("=");
                if (pair.length == 2) {
                    buildArgsMap.put(pair[0].trim(), pair[1].trim());
                } else {
                    console.logError("Invalid format for " + arg + ". Buildargs should be formatted as key=value");
                }
            }
        }

        String dockerFolderRes = Resolver.buildVar(build, dockerFolder);
        String imageTagRes = Resolver.buildVar(build, imageTag);

        String expandedDockerFolder = Resolver.buildVar(build, dockerFolderRes);

        String expandedImageTag = Resolver.buildVar(build, imageTagRes);

        String dockerFileRes = dockerFile == null ? "Dockerfile" : Resolver.buildVar(build, dockerFile);

        String imageId = null;
        try {
            Config cfgData = getConfig(build);
            imageId = launcher.getChannel().call(new RemoteCallable(expandedDockerFolder, expandedImageTag, dockerFileRes, cfgData, buildArgsMap, noCache, rm, Jenkins.getInstance().getDescriptor(DockerBuilder.class)));
        } catch (Exception e) {
        	console.logError("Failed to create docker image: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
        
        console.logInfo("Build image id:" + imageId);
    }

    public static class RemoteCallable implements Callable<String, Exception>, Serializable {

        private static final long serialVersionUID = -6593420984897195978L;

        String expandedDockerFolder;
        String expandedImageTag;
        String dockerFileRes;
        Config cfgData;
        Map<String, String> buildArgsMap;
        boolean noCache;
        boolean rm;

        Descriptor<?> descriptor;

        public RemoteCallable(String expandedDockerFolder, String expandedImageTag, String dockerFileRes, Config cfgData, Map<String, String> buildArgsMap, boolean noCache, boolean rm, Descriptor descriptor) {
            this.expandedDockerFolder = expandedDockerFolder;
            this.expandedImageTag = expandedImageTag;
            this.dockerFileRes = dockerFileRes;
            this.cfgData = cfgData;
            this.buildArgsMap = buildArgsMap;
            this.noCache = noCache;
            this.rm = rm;
            this.descriptor = descriptor;
        }

        public String call() throws Exception {
            FilePath folder = new FilePath(new File(expandedDockerFolder));

            if (!exist(folder))
                throw new IllegalArgumentException(
                        "configured dockerFolder '" + expandedDockerFolder + "' does not exist.");

            if (!exist(folder.child(dockerFileRes))) {
                throw new IllegalArgumentException(
                        String.format("Configured Docker file '%s' does not exist.", dockerFileRes));
            }

            File docker = new File(expandedDockerFolder, dockerFileRes);

            BuildImageResultCallback callback = new BuildImageResultCallback() {
                @Override
                public void onNext(BuildResponseItem item) {
                    super.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    super.onError(throwable);
                }
            };

            DockerClient client = getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);
            BuildImageCmd buildImageCmd = client
                    .buildImageCmd(docker)
                    .withTag(expandedImageTag)
                    .withNoCache(noCache)
                    .withRemove(rm);
            if (!buildArgsMap.isEmpty()) {
                for (final Map.Entry<String, String> entry : buildArgsMap.entrySet()) {
                    buildImageCmd = buildImageCmd.withBuildArg(entry.getKey(), entry.getValue());
                }
            }

            BuildImageResultCallback result = buildImageCmd.exec(callback);

            return result.awaitImageId();
        }

        private boolean exist(FilePath filePath) throws DockerException {
            try {
                return filePath.exists();
            } catch (Exception e) {
                throw new DockerException("Could not check file", 0, e);
            }
        }
    }

    @Extension
    public static class CreateImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Create/build image";
        }
    }
}
