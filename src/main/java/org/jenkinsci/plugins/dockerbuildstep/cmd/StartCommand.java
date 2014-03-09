package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.PortUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.ContainerInspectResponse;

/**
 * This command starts one or more Docker containers. It also exports some build environment variable like IP or started
 * containers.
 * 
 * @see http://docs.docker.io/en/master/api/docker_remote_api_v1.8/#start-a-container
 * 
 * @author vjuranek
 * 
 */
public class StartCommand extends DockerCommand {

    private String containerIds;
    private final String waitPorts;

    @DataBoundConstructor
    public StartCommand(String containerIds, String waitPorts) {
        this.containerIds = containerIds;
        this.waitPorts = waitPorts;
    }

    public String getContainerIds() {
        return containerIds;
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

        List<String> ids = Arrays.asList(containerIds.split(","));
        DockerClient client = getClient();
        //TODO check, if container exists and is stopped (probably catch exception)
        for (String id : ids) {
            id = id.trim();
            client.startContainer(id);
            console.logInfo("started container id " + id);

            ContainerInspectResponse inspectResp = client.inspectContainer(id);
            EnvInvisibleAction envAction = new EnvInvisibleAction(inspectResp);
            build.addAction(envAction);
        }
        
        //wait for ports
        if(waitPorts != null && !waitPorts.isEmpty()) {
            Map<String, List<Integer>> containers = PortUtils.parsePorts(waitPorts);
            for(String cId : containers.keySet()) {
                ContainerInspectResponse inspectResp = client.inspectContainer(cId);
                String ip = inspectResp.getNetworkSettings().ipAddress;
                List<Integer> ports = containers.get(cId);
                for(Integer port : ports) {
                    System.out.println("Waiting for port " + port + " on " + ip);
                    boolean isOk = PortUtils.waitForPort(ip, port);
                    System.out.println("Wait for port went " + isOk);
                }
            }
        }
    }

    @Extension
    public static class StartCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Start constainer(s)";
        }
    }

}
