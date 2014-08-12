package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.PortUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.DockerException;
import com.github.dockerjava.client.model.ContainerInspectResponse;
import com.github.dockerjava.client.model.ExposedPort;
import com.github.dockerjava.client.model.Ports;
import com.github.dockerjava.client.model.Ports.Binding;

/**
 * This command starts one or more Docker containers. It also exports some build environment variable like IP or started
 * containers.
 * 
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#start-a-container
 * 
 * @author vjuranek
 * 
 */
public class StartCommand extends DockerCommand {

    private final String containerIds;
    private final String portBindings;
    private final String waitPorts;

    @DataBoundConstructor
    public StartCommand(String containerIds, String portBindings, String waitPorts) {
        this.containerIds = containerIds;
        this.portBindings = portBindings;
        this.waitPorts = waitPorts;
    }

    public String getContainerIds() {
        return containerIds;
    }

    public String getPortBindings() {
        return portBindings;
    }

    public String getWaitPorts() {
        return waitPorts;
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

        List<String> ids = Arrays.asList(containerIdsRes.split(","));
        Ports bindPorts = parsePortBindings(portBindingsRes);
        DockerClient client = getClient();

        // TODO check, if container exists and is stopped (probably catch exception)
        for (String id : ids) {
            id = id.trim();
            client.execute(client.startContainerCmd(id).withPortBindings(bindPorts));
            console.logInfo("started container id " + id);

            ContainerInspectResponse inspectResp = client.execute(client.inspectContainerCmd(id));
            EnvInvisibleAction envAction = new EnvInvisibleAction(inspectResp);
            build.addAction(envAction);
        }

        // wait for ports
        if (waitPorts != null && !waitPorts.isEmpty()) {
            String waitPortsResolved = Resolver.buildVar(build, waitPorts);
            waitForPorts(waitPortsResolved, client, console);
        }
    }

    /**
     * Assumes one port binding per line in format
     * <ul> 
     *  <li>dockerPort hostPort</li>
     *  <li>dockerPort/scheme hostPort</li>
     *  <li>dockerPort hostIP:hostPort</li>
     *  <li>dockerPort/scheme hostIP:hostPort</li>
     * </ul>
     */
    private Ports parsePortBindings(String bindings) throws IllegalArgumentException {
        if (bindings == null || bindings.isEmpty())
            return null;

        Ports ports = new Ports();
        String[] bindLines = bindings.split("\\r?\\n");
        for (String bind : bindLines) {
            String[] bindSplit = bind.trim().split(" ", 2);
            if(bindSplit.length != 2)
                throw new IllegalArgumentException("Port binding needs to be in format 'hostPort containerPort'");
            ExposedPort ep = bindSplit[0].contains("/") ? ExposedPort.parse(bindSplit[0].trim()) : ExposedPort.tcp(new Integer(bindSplit[0].trim()));
            String[] hostBind = bindSplit[1].trim().split(":", 2);
            Binding b = hostBind.length > 1 ? new Binding(hostBind[0], new Integer(hostBind[1])) : new Binding(new Integer(hostBind[0]));
            ports.bind(ep, b);
        }
        return ports;
    }

    private void waitForPorts(String waitForPorts, DockerClient client, ConsoleLogger console) throws DockerException {
        Map<String, List<Integer>> containers = PortUtils.parsePorts(waitForPorts);
        for (String cId : containers.keySet()) {
            ContainerInspectResponse inspectResp = client.execute(client.inspectContainerCmd(cId));
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
    }

}
