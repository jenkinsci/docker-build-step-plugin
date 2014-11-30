package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * This command creates new container from specified image.
 * 
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#create-a-container
 * 
 * @author vjuranek
 * 
 */
public class CreateContainerCommand extends DockerCommand {

    private final String image;
    private final String command;
    private final String hostName;
    private final String containerName;
	private final String envVars;

    @DataBoundConstructor
    public CreateContainerCommand(String image, String command, String hostName, String containerName, String envVars) {
        this.image = image;
        this.command = command;
        this.hostName = hostName;
        this.containerName = containerName;
		this.envVars = envVars;
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

    public String getContainerName() {
        return containerName;
    }
    
	public String getEnvVars() {
		return envVars;
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
        String containerNameRes = Resolver.buildVar(build, containerName);
        String envVarsRes = Resolver.buildVar(build, envVars);
        
        DockerClient client = getClient();
        CreateContainerCmd cfgCmd = client.createContainerCmd(imageRes);
        if (!commandRes.isEmpty()) {
            cfgCmd.withCmd(new String[] { commandRes });
        }
        cfgCmd.withHostName(hostNameRes);
        cfgCmd.withName(containerNameRes);
		if(!envVarsRes.isEmpty()){
			String[] envVarResSplitted = envVarsRes.split(",");
			cfgCmd.withEnv(envVarResSplitted);
		}
        CreateContainerResponse resp = cfgCmd.exec();
        console.logInfo("created container id " + resp.getId() + " (from image " + imageRes + ")");

        /*
         * if (resp.getWarnings() != null) { for (String warn : resp.getWarnings()) System.out.println("WARN: " + warn);
         * }
         */
        InspectContainerResponse inspectResp = client.inspectContainerCmd(resp.getId()).exec();
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
