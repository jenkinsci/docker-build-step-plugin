package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.dockerbuildstep.action.DockerContainerConsoleAction;
import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.BindParser;
import org.jenkinsci.plugins.dockerbuildstep.util.LinkUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.PortBindingParser;
import org.jenkinsci.plugins.dockerbuildstep.util.PortUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Links;
import com.github.dockerjava.api.model.PortBinding;

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
    private final String links;
    private final String bindMounts;
    private final boolean privileged;
    private final String containerIdsLogging;

    @DataBoundConstructor
    public StartCommand(String containerIds, boolean publishAllPorts, String portBindings, String waitPorts,
            String links, String bindMounts, boolean privileged, String containerIdsLogging) {
        this.containerIds = containerIds;
        this.publishAllPorts = publishAllPorts;
        this.portBindings = portBindings;
        this.waitPorts = waitPorts;
        this.links = links;
        this.bindMounts = bindMounts;
        this.privileged = privileged;
        this.containerIdsLogging = containerIdsLogging;
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
    
    public String getLinks() {
        return links;
    }
    
    public String getBindMounts() {
        return bindMounts;
    }

    public boolean getPrivileged() {
        return privileged;
    }

    public String getContainerIdsLogging() {
		return containerIdsLogging;
	}

	@Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        if (containerIds == null || containerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        List<String> ids = Arrays.asList(Resolver.buildVar(build, containerIds).split(","));
        PortBinding[] portBindingsRes = PortBindingParser.parse(Resolver.buildVar(build, portBindings));
        Links linksRes = LinkUtils.parseLinks(Resolver.buildVar(build, links));
        Bind[] bindsRes = BindParser.parse(Resolver.buildVar(build, bindMounts));
        List<String> logIds = Arrays.asList(Resolver.buildVar(build, containerIdsLogging).split(","));
        
        DockerClient client = getClient(null);
        
        // TODO check, if container exists and is stopped (probably catch exception)
        for (String id : ids) {
            id = id.trim();
            
            DockerContainerConsoleAction outAction = null;
            if (logIds.contains(id)) {
            	outAction = attachContainerOutput(build, id);
            }

            client.startContainerCmd(id)
                    .withPublishAllPorts(publishAllPorts)
                    .withPortBindings(portBindingsRes)
                    .withLinks(linksRes.getLinks())
                    .withBinds(bindsRes)
                    .withPrivileged(privileged)
                    .exec();
            console.logInfo("started container id " + id);

            InspectContainerResponse inspectResp = client.inspectContainerCmd(id).exec();
            if (outAction != null) {
            	outAction.setContainerName(inspectResp.getName());
            }
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
                console.logInfo("Waiting for port " + port + " on " + ip + " (container ID " + cId + ")");
                boolean portReady = PortUtils.waitForPort(ip, port);
                if (portReady) {
                    console.logInfo(ip + ":" + port + " ready");
                } else {
                    // TODO fail the build, but make timeout configurable first
                    console.logWarn(ip + ":" + port + " still not available (container ID " + cId
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
    }

}
