package org.jenkinsci.plugins.dockerbuildstep.action;

import hudson.model.InvisibleAction;

import java.util.Map;

import org.jenkinsci.plugins.dockerbuildstep.DockerEnvContributor;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

/**
 * Helper invisible action which is used for exchanging information between {@link DockerCommand}s and other object like
 * {@link DockerEnvContributor}.
 * 
 * @author vjuranek
 * 
 */
public class EnvInvisibleAction extends InvisibleAction {

    private InspectContainerResponse containerInfo;

    public EnvInvisibleAction() {
    }

    public EnvInvisibleAction(InspectContainerResponse containerInfo) {
        this.containerInfo = containerInfo;
    }

    public InspectContainerResponse getContainerInfo() {
        return containerInfo;
    }

    public void setContainerInfo(InspectContainerResponse containerInfo) {
        this.containerInfo = containerInfo;
    }

    // convenient shortcut methods

    public String getId() {
        return containerInfo.getId();
    }

    public String getHostName() {
        return containerInfo.getConfig().getHostName();
    }

    public String getIpAddress() {
        return containerInfo.getNetworkSettings().getIpAddress();
    }
    
    public boolean hasPortBindings() {
        Ports ports = containerInfo.getNetworkSettings().getPorts(); 
        return  ((ports !=  null) && (ports.getBindings() != null) && (!ports.getBindings().isEmpty()));
    }
    
    public Map<ExposedPort, Binding> getPortBindings() {
        return containerInfo.getNetworkSettings().getPorts().getBindings();
    }

}
