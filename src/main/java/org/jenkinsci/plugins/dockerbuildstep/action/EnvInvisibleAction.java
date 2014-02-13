package org.jenkinsci.plugins.dockerbuildstep.action;

import hudson.model.InvisibleAction;

import com.kpelykh.docker.client.model.ContainerInspectResponse;

public class EnvInvisibleAction extends InvisibleAction {
    
    private ContainerInspectResponse containerInfo;
    
    public EnvInvisibleAction() {
    }
    
    public EnvInvisibleAction(ContainerInspectResponse containerInfo) {
        this.containerInfo = containerInfo;
    }

    public ContainerInspectResponse getContainerInfo() {
        return containerInfo;
    }

    public void setContainerInfo(ContainerInspectResponse containerInfo) {
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
        return containerInfo.getNetworkSettings().ipAddress;
    }
    
}
