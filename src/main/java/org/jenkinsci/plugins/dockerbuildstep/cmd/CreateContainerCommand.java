package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.exception.DockerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.CreateContainerRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * This command creates new container from specified image.
 *
 * @author vjuranek
 * @see <a href="https://docs.docker.com/engine/api/v1.37/#operation/ContainerCreate">https://docs.docker.com/engine/api/v1.37/#operation/ContainerCreate</a>
 * @see <a href="https://docs.docker.com/engine/api/v1.18/#21-containers">https://docs.docker.com/engine/api/v1.18/#21-containers</a>
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
    private final String networkMode;
    private final boolean publishAllPorts;
    private final String portBindings;
    private final String bindMounts;
    private final boolean privileged;
    private final boolean alwaysRestart;

    @DataBoundConstructor
    public CreateContainerCommand(String image, String command, String hostName, String containerName, String envVars,
                                  String links, String exposedPorts, String cpuShares, String memoryLimit, String dns,
                                  String extraHosts, String networkMode, boolean publishAllPorts, String portBindings,
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
        this.networkMode = networkMode;
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

    public String getNetworkMode() {
        return networkMode;
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
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        // TODO check it when submitting the form
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        // Parse and log parameters

        final String imageRes = Resolver.buildVar(build, image);
        final String commandRawRes = Resolver.buildVar(build, command);
        final String[] commandRes = commandRawRes.isEmpty() ? null : commandRawRes.split(" ");
        final String hostNameRes = Resolver.buildVar(build, hostName);
        final String containerNameRes = Resolver.buildVar(build, containerName);
        final String envVarsRawRes = Resolver.buildVar(build, envVars);
        final String[] envVarsRes = envVarsRawRes.isEmpty() ? null : envVarsRawRes.split("\\r?\\n");
        final String linksRes = Resolver.buildVar(build, links);
        final String exposedPortsRes = Resolver.buildVar(build, exposedPorts);
        final String cpuSharesRawRes = Resolver.buildVar(build, cpuShares);
        final Integer cpuSharesRes = cpuSharesRawRes == null || cpuSharesRawRes.isEmpty() ? null : Integer.parseInt(cpuSharesRawRes);
        final String memoryLimitRawRes = Resolver.buildVar(build, memoryLimit);
        final Long memoryLimitRes;
        if (memoryLimitRawRes != null && !memoryLimitRawRes.isEmpty()) {
            long ml = CommandUtils.sizeInBytes(memoryLimitRawRes);
            if (ml > -1) {
                memoryLimitRes = ml;
            } else {
                memoryLimitRes = null;
                console.logWarn("Unable to parse memory limit '" + memoryLimitRawRes + "', memory limit not enforced!");
            }
        } else {
            memoryLimitRes = null;
        }

        final String[] dnsRes;
        if (dns != null && !dns.isEmpty()) {
            console.logInfo("set dns: " + dns);
            dnsRes = dns.split(",");
        } else {
            dnsRes = null;
        }

        final String[] extraHostsRes;
        if (extraHosts != null && !extraHosts.isEmpty()) {
            console.logInfo("set extraHosts: " + extraHosts);
            extraHostsRes = extraHosts.split(",");
        } else {
            extraHostsRes = null;
        }

        final String networkModeRes;
        if (networkMode != null && !networkMode.isEmpty()) {
            console.logInfo("set networkMode: " + networkMode);
            networkModeRes = networkMode;
        } else {
            networkModeRes = null;
        }

        final String portBindingsRes;
        if (portBindings != null && !portBindings.isEmpty()) {
            console.logInfo("set portBindings: " + portBindings);
            portBindingsRes = Resolver.buildVar(build, portBindings);
        } else {
            portBindingsRes = null;
        }

        final String bindMountsRes;
        if (bindMounts != null && !bindMounts.isEmpty()) {
            console.logInfo("set Mounts: " + bindMounts);
            bindMountsRes = Resolver.buildVar(build, bindMounts);
        } else {
            bindMountsRes = null;
        }

        // Call Docker

        try {
            Config cfgData = getConfig(build);
            Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);

            String inspectRespSerialized = launcher.getChannel().call(new CreateContainerRemoteCallable(cfgData, descriptor, imageRes, commandRes, hostNameRes, containerNameRes, linksRes, envVarsRes, exposedPortsRes, cpuSharesRes, memoryLimitRes, dnsRes, extraHostsRes, networkModeRes, portBindingsRes, bindMountsRes, alwaysRestart, publishAllPorts, privileged));
            ObjectMapper mapper = new ObjectMapper();
            InspectContainerResponse inspectResp = mapper.readValue(inspectRespSerialized, InspectContainerResponse.class);

            console.logInfo("created container id " + inspectResp.getId() + " (from image " + imageRes + ")");
            EnvInvisibleAction envAction = new EnvInvisibleAction(inspectResp);
            build.addAction(envAction);
        } catch (Exception e) {
            console.logError("failed to stop all containers");
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
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
