package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.DockerException;
import com.github.dockerjava.client.command.CreateContainerCmd;
import com.github.dockerjava.client.model.ContainerCreateResponse;
import com.github.dockerjava.client.model.ContainerInspectResponse;
import com.github.dockerjava.client.model.CreateContainerConfig;

/**
 * This command creates new container from specified image.
 * 
 * @see http://docs.docker.io/en/master/api/docker_remote_api_v1.8/#create-a-container
 * 
 * @author vjuranek
 * 
 */
public class CreateContainerCommand extends DockerCommand {

    private final String image;
    private final String command;
    private final String hostName;

    @DataBoundConstructor
    public CreateContainerCommand(String image, String command, String hostName) {
        this.image = image;
        this.command = command;
        this.hostName = hostName;
    }

    public String getImage() {
        return image;
    }

    public String getCommand() {
        return command;
    }

    public String getHostName() {
        return hostName;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        // TODO check it when submitting the form
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
        
        String imageRes = Resolver.buildVar(build, image);
        String commandRes = Resolver.buildVar(build, command);
        String hostNameRes = Resolver.buildVar(build, hostName);
        
        DockerClient client = getClient();
        CreateContainerCmd cfgCmd = client.createContainerCmd(imageRes);
        if (!commandRes.isEmpty()) {
            cfgCmd.withCmd(new String[] { commandRes });
        }
        cfgCmd.withHostName(hostNameRes);
        ContainerCreateResponse resp = client.execute(cfgCmd);
        console.logInfo("created container id " + resp.getId() + " (from image " + imageRes + ")");

        /*
         * if (resp.getWarnings() != null) { for (String warn : resp.getWarnings()) System.out.println("WARN: " + warn);
         * }
         */
        ContainerInspectResponse inspectResp = client.execute(client.inspectContainerCmd(resp.getId()));
        EnvInvisibleAction envAction = new EnvInvisibleAction(inspectResp);
        build.addAction(envAction);
    }

    @Extension
    public static class CreateContainerCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Create container";
        }
    }

}
