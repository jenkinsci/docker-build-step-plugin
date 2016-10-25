package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.dockerbuildstep.action.DockerContainerConsoleAction;
import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.BindParser;
import org.jenkinsci.plugins.dockerbuildstep.util.PortBindingParser;
import org.jenkinsci.plugins.dockerbuildstep.util.PortUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This command starts one or more Docker containers. It also exports some build environment variables like IP or
 * started containers.
 *
 * @author vjuranek
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#start-a-container
 */
public class StartCommand extends DockerCommand {

    private final String containerIds;
    private final String waitPorts;
    private final String containerIdsLogging;

    @DataBoundConstructor
    public StartCommand(String containerIds, String waitPorts, String containerIdsLogging) {
        this.containerIds = containerIds;
        this.waitPorts = waitPorts;
        this.containerIdsLogging = containerIdsLogging;
    }

    public String getContainerIds() {
        return containerIds;
    }
    public String getWaitPorts() {
        return waitPorts;
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
        List<String> logIds = Arrays.asList(Resolver.buildVar(build, containerIdsLogging).split(","));

        DockerClient client = getClient(build, null);

        // TODO check, if container exists and is stopped (probably catch exception)
        for (String id : ids) {
            id = id.trim();

            DockerContainerConsoleAction outAction = null;
            if (logIds.contains(id)) {
                outAction = attachContainerOutput(build, id);
            }

            client.startContainerCmd(id).exec();
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
    }

}
