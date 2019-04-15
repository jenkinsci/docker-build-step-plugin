package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.core.command.BuildImageResultCallback;

import hudson.model.BuildListener;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.remoting.Callable;

/**
 * A Callable wrapping the commands necessary to create an image.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class CreateImageRemoteCallable implements Callable<String, Exception>, Serializable {

    private static final long serialVersionUID = -6593420984897195978L;

    BuildListener listener;

    Config cfgData;
    Descriptor<?> descriptor;

    String expandedDockerFolder;
    String expandedImageTag;
    String dockerFileRes;
    Map<String, String> buildArgsMap;
    boolean noCache;
    boolean rm;

    public CreateImageRemoteCallable(BuildListener listener, Config cfgData, Descriptor<?> descriptor, String expandedDockerFolder, String expandedImageTag, String dockerFileRes, Map<String, String> buildArgsMap, boolean noCache, boolean rm) {
        this.listener = listener;
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
        final ConsoleLogger console = new ConsoleLogger(listener);
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
                String text = item.getStream();
                if (text != null) {
                    console.logInfo(text);
                }
                super.onNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                console.logError("Failed to exec start:" + throwable.getMessage());
                super.onError(throwable);
            }
        };

        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);
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
