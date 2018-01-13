package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.Serializable;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Links;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.RestartPolicy;

import hudson.model.Descriptor;
import hudson.remoting.Callable;


/**
 * A Callable wrapping the commands necessary to create a container.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class CreateContainerRemoteCallable implements Callable<InspectContainerResponse, Exception>, Serializable {

    private static final long serialVersionUID = -4028940605497568422L;
    
    Config cfgData;
    Descriptor<?> descriptor;
    
    String imageRes;
    String[] commandRes;
    String hostNameRes;
    String containerNameRes;
    Links linksRes;
    String[] envVarsRes;
    ExposedPort[] ports;
    Integer cpuSharesRes;
    Long memoryLimitRes;
    String[] dnsRes;
    String[] extraHostsRes;
    PortBinding[] portBindingsRes;
    Bind[] bindMountsRes;
    boolean alwaysRestart;
    boolean publishAllPorts;
    boolean privileged;
    
    public CreateContainerRemoteCallable(Config cfgData, Descriptor<?> descriptor, String imageRes, String[] commandRes,
            String hostNameRes, String containerNameRes, Links linksRes, String[] envVarsRes, ExposedPort[] ports,
            Integer cpuSharesRes, Long memoryLimitRes, String[] dnsRes, String[] extraHostsRes,
            PortBinding[] portBindingsRes, Bind[] bindMountsRes, boolean alwaysRestart, boolean publishAllPorts,
            boolean privileged) {
        super();
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.imageRes = imageRes;
        this.commandRes = commandRes;
        this.hostNameRes = hostNameRes;
        this.containerNameRes = containerNameRes;
        this.linksRes = linksRes;
        this.envVarsRes = envVarsRes;
        this.ports = ports;
        this.cpuSharesRes = cpuSharesRes;
        this.memoryLimitRes = memoryLimitRes;
        this.dnsRes = dnsRes;
        this.extraHostsRes = extraHostsRes;
        this.portBindingsRes = portBindingsRes;
        this.bindMountsRes = bindMountsRes;
        this.alwaysRestart = alwaysRestart;
        this.publishAllPorts = publishAllPorts;
        this.privileged = privileged;
    }

    public InspectContainerResponse call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);
        
        CreateContainerCmd cfgCmd = client.createContainerCmd(imageRes);
        if (commandRes != null) {
            cfgCmd.withCmd(commandRes);
        }
        cfgCmd.withHostName(hostNameRes);
        cfgCmd.withName(containerNameRes);
        HostConfig hc = new HostConfig();
        cfgCmd.withLinks(linksRes.getLinks());
        if (envVarsRes != null) {
            cfgCmd.withEnv(envVarsRes);
        }
        if (ports != null) {
            cfgCmd.withExposedPorts(ports);
        }
        if (cpuSharesRes != null) {
            cfgCmd.withCpuShares(cpuSharesRes);
        }
        if (memoryLimitRes != null) {
            cfgCmd.withMemory(memoryLimitRes);
        }
        if (dnsRes != null) {
            cfgCmd.withDns(dnsRes);
        }
        if (extraHostsRes != null) {
            cfgCmd.withExtraHosts(extraHostsRes);
        }
        if (portBindingsRes != null) {
            cfgCmd.withPortBindings(portBindingsRes);
        }
        if (bindMountsRes != null) {
            cfgCmd.withBinds(bindMountsRes);
        }
        if (alwaysRestart) {
            cfgCmd.withRestartPolicy(RestartPolicy.alwaysRestart());
        }
        
        CreateContainerResponse resp = cfgCmd.withPublishAllPorts(publishAllPorts).withPrivileged(privileged).exec();
        InspectContainerResponse inspectResp = client.inspectContainerCmd(resp.getId()).exec();
        return inspectResp;
    }
}
