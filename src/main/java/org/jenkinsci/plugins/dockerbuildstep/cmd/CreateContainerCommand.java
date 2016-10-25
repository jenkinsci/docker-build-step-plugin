package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * This command creates new container from specified image.
 *
 * @author vjuranek
 * @see @Link:http//docs.docker.com/reference/api/docker_remote_api_v1.13/#create-a-container
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
    private final String dns;
    private final String extraHosts;
    private final boolean publishAllPorts;
    private final String portBindings;
    private final String bindMounts;
    private final boolean privileged;
    private final boolean alwaysRestart;

    @DataBoundConstructor
    public CreateContainerCommand(String image, String command, String hostName, String containerName, String envVars,
                                  String links, String exposedPorts, String cpuShares, String memoryLimit, String dns,
                                  String extraHosts, boolean publishAllPorts, String portBindings,
                                  String bindMounts, boolean privileged, boolean alwaysRestart) throws IllegalArgumentException {
        this.image = image;
        this.command = command;
        this.hostName = hostName;
        this.containerName = containerName;
        this.envVars = envVars;
        this.links = links;
        this.exposedPorts = exposedPorts;
        this.cpuShares = cpuShares;
        this.memoryLimit = memoryLimit;
        this.dns = dns;
        this.extraHosts = extraHosts;
        this.publishAllPorts = publishAllPorts;
        this.portBindings = portBindings;
        this.bindMounts = bindMounts;
        this.privileged = privileged;
        this.alwaysRestart = alwaysRestart;
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

    public String getDns() {
        return dns;
    }

    public String getExtraHosts() {
        return extraHosts;
    }

    public boolean isPublishAllPorts() {
        return publishAllPorts;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public boolean getPublishAllPorts() {
        return publishAllPorts;
    }

    public String getPortBindings() {
        return portBindings;
    }

    public String getBindMounts() {
        return bindMounts;
    }

    public boolean getPrivileged() {
        return privileged;
    }

    public boolean isAlwaysRestart() {
        return alwaysRestart;
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

        DockerClient client = getClient(build, null);
        CreateContainerCmd cfgCmd = client.createContainerCmd(imageRes);
        if (!commandRes.isEmpty()) {
            cfgCmd.withCmd(commandRes.split(" "));
        }
        cfgCmd.withHostName(hostNameRes);
        cfgCmd.withName(containerNameRes);
        HostConfig hc = new HostConfig();
        cfgCmd.withLinks(linksRes.getLinks());
        if (!envVarsRes.isEmpty()) {
            String[] encVarResSlitted = envVarsRes.split("\\r?\\n");
            cfgCmd.withEnv(encVarResSlitted);
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
                cfgCmd.withMemory(ml);
            } else {
                console.logWarn("Unable to parse memory limit '" + memoryLimitRes + "', memory limit not enforced!");
            }
        }
        if (dns != null && !dns.isEmpty()) {
            console.logInfo("set dns: " + dns);
            String[] dnsArray = dns.split(",");
            if (dnsArray == null || dnsArray.length == 0) {
                cfgCmd.withDns(dns);
            } else {
                cfgCmd.withDns(dnsArray);
            }
        }
        if (extraHosts != null && !extraHosts.isEmpty()) {
            console.logInfo("set extraHosts: " + extraHosts);
            String[] extraHostsArray = extraHosts.split(",");
            if (extraHostsArray == null || extraHostsArray.length == 0) {
                cfgCmd.withExtraHosts(extraHosts);
            } else {
                cfgCmd.withExtraHosts(extraHostsArray);
            }
        }

        if (portBindings != null && !portBindings.isEmpty()) {
            console.logInfo("set portBindings: " + portBindings);
            PortBinding[] portBindingsRes = PortBindingParser.parse(Resolver.buildVar(build, portBindings));
            cfgCmd.withPortBindings(portBindingsRes);
        }

        if (bindMounts != null && !bindMounts.isEmpty()) {
            console.logInfo("set portBindings: " + bindMounts);
            Bind[] bindsRes = BindParser.parse(Resolver.buildVar(build, bindMounts));
            cfgCmd.withBinds(bindsRes);
        }
        if (alwaysRestart) {
            cfgCmd.withRestartPolicy(RestartPolicy.alwaysRestart());
        }
        CreateContainerResponse resp = cfgCmd.withPublishAllPorts(publishAllPorts).withPrivileged(privileged).exec();
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

        public FormValidation doTestPortBindings(@QueryParameter String portBindings) {
            try {
                PortBindingParser.parse(portBindings);
            } catch (IllegalArgumentException e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok("OK");
        }

        public FormValidation doTestBindMounts(@QueryParameter String bindMounts) {
            try {
                BindParser.parse(bindMounts);
            } catch (IllegalArgumentException e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok("OK");
        }

        public FormValidation doTestEnvVars(@QueryParameter String envVars) {
            try {
                envVars.split("\\r?\\n");
            } catch (IllegalArgumentException e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok("OK");
        }
    }

}
