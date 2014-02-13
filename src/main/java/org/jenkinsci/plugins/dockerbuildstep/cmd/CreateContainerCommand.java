package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.ContainerConfig;
import com.kpelykh.docker.client.model.ContainerCreateResponse;
import com.kpelykh.docker.client.model.ContainerInspectResponse;

public class CreateContainerCommand extends DockerCommand {

    private final String image;
    private final String command;

    @DataBoundConstructor
    public CreateContainerCommand(String image, String command) {
        this.image = image;
        this.command = command;
    }

    public String getImage() {
        return image;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, BuildListener listener) throws DockerException {
        // TODO check it when submitting the form
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
        ContainerConfig cfg = new ContainerConfig();
        cfg.setImage(image);
        cfg.setCmd(new String[] { command });
        DockerClient client = getClient();
        ContainerCreateResponse resp = client.createContainer(cfg);

        /*if (resp.getWarnings() != null) {
            for (String warn : resp.getWarnings())
                System.out.println("WARN: " + warn);
        }*/
        ContainerInspectResponse inspectResp = client.inspectContainer(resp.getId());
        EnvInvisibleAction envAction = new EnvInvisibleAction(inspectResp);
        build.addAction(envAction);
    }

    @Extension
    public static class CreateContainerCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Create constainer";
        }
    }

}
