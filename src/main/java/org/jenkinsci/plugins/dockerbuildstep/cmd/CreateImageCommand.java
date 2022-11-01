package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.exception.DockerException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.CreateImageRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * This command creates a new image from specified Dockerfile.
 *
 * @author marcus
 * @see <a href="https://docs.docker.com/engine/api/v1.41/#tag/Image/operation/ImageBuild">Build an image</a>
 */
public class CreateImageCommand extends DockerCommand {

    private final String dockerFolder;
    private final String imageTag;
    private final String dockerFile;
    private final boolean pull;
    private final boolean noCache;
    private final boolean rm;
    private final String buildArgs;
    
    @DataBoundConstructor
    public CreateImageCommand(String dockerFolder, String imageTag, String dockerFile, boolean pull, boolean noCache, boolean rm, String buildArgs) {
        this.dockerFolder = dockerFolder;
        this.imageTag = imageTag;
        this.dockerFile = dockerFile;
        this.pull = pull;
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

    public boolean isPull() {
        return pull;
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
                String[] pair = arg.split("=", 2);
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
            Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);

            imageId = launcher.getChannel().call(new CreateImageRemoteCallable(console.getListener(), cfgData, descriptor, expandedDockerFolder, expandedImageTag, dockerFileRes, buildArgsMap, pull, noCache, rm));
        } catch (Exception e) {
            console.logError("Failed to create docker image: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
        
        console.logInfo("Build image id:" + imageId);
    }

    @Extension
    public static class CreateImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Create/build image";
        }
    }
}
