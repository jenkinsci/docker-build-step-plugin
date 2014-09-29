package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.util.FormValidation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.BindParser;
import org.jenkinsci.plugins.dockerbuildstep.util.PortBindingParser;
import org.jenkinsci.plugins.dockerbuildstep.util.PortUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Ports;

/**
 * This command starts one or more Docker containers.
 * It also exports some build environment variables like IP or started containers.
 * 
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#start-a-container
 * 
 * @author vjuranek
 * 
 */
public class StartCommand extends DockerCommand {

    private final String containerIds;
    private final boolean publishAllPorts;
    private final String portBindings;
    private final String waitPorts;
    private final String bindMounts;
    private final boolean privileged;

    @DataBoundConstructor
    public StartCommand(String containerIds, boolean publishAllPorts, String portBindings, String waitPorts,
            String bindMounts, boolean privileged) {
        this.containerIds = containerIds;
        this.publishAllPorts = publishAllPorts;
        this.portBindings = portBindings;
        this.waitPorts = waitPorts;
        this.bindMounts = bindMounts;
        this.privileged = privileged;
    }

    public String getContainerIds() {
        return containerIds;
    }

    public boolean getPublishAllPorts() {
        return publishAllPorts;
    }

    public String getPortBindings() {
        return portBindings;
    }

    public String getWaitPorts() {
        return waitPorts;
    }
    
    public String getBindMounts() {
        return bindMounts;
    }

    public boolean getPrivileged() {
        return privileged;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        if (containerIds == null || containerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        // expand build and env. variable
        String containerIdsRes = Resolver.buildVar(build, containerIds);
        String portBindingsRes = Resolver.buildVar(build, portBindings);
        String bindMountsRes = Resolver.buildVar(build, bindMounts);

        List<String> ids = Arrays.asList(containerIdsRes.split(","));
        Ports portBindings = PortBindingParser.parseBindings(portBindingsRes);
        Bind[] binds = BindParser.parse(bindMountsRes);
        DockerClient client = getClient();

        // TODO check, if container exists and is stopped (probably catch exception)
        for (String id : ids) {
            id = id.trim();
            client.startContainerCmd(id)
                    .withPublishAllPorts(publishAllPorts)
                    .withPortBindings(portBindings)
                    .withBinds(binds)
                    .withPrivileged(privileged)
                    .exec();
            console.logInfo("started container id " + id);

            InspectContainerResponse inspectResp = client.inspectContainerCmd(id).exec();
            EnvInvisibleAction envAction = new EnvInvisibleAction(inspectResp);
            build.addAction(envAction);
        }

        // wait for ports
        if (waitPorts != null && !waitPorts.isEmpty()) {
            String waitPortsResolved = Resolver.buildVar(build, waitPorts);
            waitForPorts(waitPortsResolved, client, console);
        }
    }

    private void waitForPorts(String waitForPorts, DockerClient client, ConsoleLogger console) throws DockerException {
        Map<String, List<Integer>> containers = PortUtils.parsePorts(waitForPorts);
        for (String cId : containers.keySet()) {
        	InspectContainerResponse inspectResp = client.inspectContainerCmd(cId).exec();
            String ip = inspectResp.getNetworkSettings().getIpAddress();
            List<Integer> ports = containers.get(cId);
            for (Integer port : ports) {
                console.logInfo("Waiting for port " + port + " on " + ip + " (conatiner ID " + cId + ")");
                boolean portReady = PortUtils.waitForPort(ip, port);
                if (portReady) {
                    console.logInfo(ip + ":" + port + " ready");
                } else {
                    // TODO fail the build, but make timeout configurable first
                    console.logWarn(ip + ":" + port + " still not available (conatiner ID " + cId
                            + "), but build continues ...");
                }
            }
        }
    }

    @Extension
    public static class StartCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Start container(s)";
        }

        public FormValidation doTestPortBindings(@QueryParameter String portBindings) {
            try {
                PortBindingParser.parseBindings(portBindings);
            } catch (IllegalArgumentException e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok();
        }

        public FormValidation doTestBindMounts(@QueryParameter String bindMounts) {
            try {
                BindParser.parse(bindMounts);
            } catch (IllegalArgumentException e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok();
        }
    }

}
