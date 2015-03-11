package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.CommandUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.LinkUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Links;

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
    private final String links;
    private final String exposedPorts;
    private final String cpuShares;
    private final String memoryLimit;

    @DataBoundConstructor
    public CreateContainerCommand(String image, String command, String hostName, String containerName, String envVars,
            String links, String exposedPorts, String cpuShares, String memoryLimit) throws IllegalArgumentException {
        this.image = image;
        this.command = command;
        this.hostName = hostName;
        this.containerName = containerName;
        this.envVars = envVars;
        this.links = links;
        this.exposedPorts = exposedPorts;
        this.cpuShares = cpuShares;
        this.memoryLimit = memoryLimit;
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

    public String getLinks() {
        return links;
    }

    public String getExposedPorts() {
        return exposedPorts;
    }

    public String getCpuShares() {
        return cpuShares;
    }

    public String getMemoryLimit() {
        return memoryLimit;
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
        Links linksRes = LinkUtils.parseLinks(Resolver.buildVar(build, links));
        String exposedPortsRes = Resolver.buildVar(build, exposedPorts);
        String cpuSharesRes = Resolver.buildVar(build, cpuShares);
        String memoryLimitRes = Resolver.buildVar(build, memoryLimit);

        DockerClient client = getClient(null);
        CreateContainerCmd cfgCmd = client.createContainerCmd(imageRes);
        if (!commandRes.isEmpty()) {
            cfgCmd.withCmd(new String[] { commandRes });
        }
        cfgCmd.withHostName(hostNameRes);
        cfgCmd.withName(containerNameRes);
        HostConfig hc = new HostConfig();
        hc.setLinks(new Links(linksRes.getLinks()));
        cfgCmd.withHostConfig(hc);
        if (!envVarsRes.isEmpty()) {
            String[] envVarResSplitted = envVarsRes.split(",");
            cfgCmd.withEnv(envVarResSplitted);
        }
        if (exposedPortsRes != null && !exposedPortsRes.isEmpty()) {
            String[] exposedPortsSplitted = exposedPortsRes.split(",");
            ExposedPort[] ports = new ExposedPort[exposedPortsSplitted.length];
            for (int i = 0; i < ports.length; i++) {
                ports[i] = ExposedPort.parse(exposedPortsSplitted[i]);
            }
            cfgCmd.withExposedPorts(ports);
        }
        if (cpuSharesRes != null && !cpuSharesRes.isEmpty()) {
            cfgCmd.withCpuShares(Integer.parseInt(cpuSharesRes));
        }
        if (memoryLimitRes != null && !memoryLimitRes.isEmpty()) {
            long ml = CommandUtils.sizeInBytes(memoryLimitRes);
            if (ml > -1) {
                cfgCmd.withMemoryLimit(ml);
            } else {
                console.logWarn("Unable to parse memory limit '" + memoryLimitRes + "', memory limit not enforced!");
            }
        }
        CreateContainerResponse resp = cfgCmd.exec();
        console.logInfo("created container id " + resp.getId() + " (from image " + imageRes + ")");

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
