package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.ContainerConfig;
import com.kpelykh.docker.client.model.ContainerCreateResponse;

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
    public void execute() throws DockerException {
        // TODO check it when submitting the form
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
        ContainerConfig cfg = new ContainerConfig();
        cfg.setImage(image);
        cfg.setCmd(new String[] { command });
        ContainerCreateResponse resp = getClient().createContainer(cfg);

        /*if (resp.getWarnings() != null) {
            for (String warn : resp.getWarnings())
                System.out.println("WARN: " + warn);
        }*/
        
    }

    @Extension
    public static class CreateContainerCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Create constainer";
        }
    }

}
