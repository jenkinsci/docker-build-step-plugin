package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.Serializable;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.plugins.dockerbuildstep.util.BindParser;
import org.jenkinsci.plugins.dockerbuildstep.util.LinkUtils;
import org.jenkinsci.plugins.dockerbuildstep.util.PortBindingParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;

import hudson.model.Descriptor;
import hudson.remoting.Callable;


/**
 * A Callable wrapping the commands necessary to create a container.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class CreateContainerRemoteCallable implements Callable<String, Exception>, Serializable {

    private static final long serialVersionUID = -4028940605497568422L;
    
    Config cfgData;
    Descriptor<?> descriptor;
    
    String imageRes;
    String[] commandRes;
    String hostNameRes;
    String containerNameRes;
    String linksRes;
    String[] envVarsRes;
    String exposedPortsRes;
    Integer cpuSharesRes;
    Long memoryLimitRes;
    String[] dnsRes;
    String[] extraHostsRes;
    String networkModeRes;
    String portBindingsRes;
    String bindMountsRes;
    boolean alwaysRestart;
    boolean publishAllPorts;
    boolean privileged;
    
    public CreateContainerRemoteCallable(Config cfgData, Descriptor<?> descriptor, String imageRes, String[] commandRes,
            String hostNameRes, String containerNameRes, String linksRes, String[] envVarsRes, String exposedPortsRes,
            Integer cpuSharesRes, Long memoryLimitRes, String[] dnsRes, String[] extraHostsRes, String networkModeRes,
            String portBindingsRes, String bindMountsRes, boolean alwaysRestart, boolean publishAllPorts,
            boolean privileged) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.imageRes = imageRes;
        this.commandRes = commandRes;
        this.hostNameRes = hostNameRes;
        this.containerNameRes = containerNameRes;
        this.linksRes = linksRes;
        this.envVarsRes = envVarsRes;
        this.exposedPortsRes = exposedPortsRes;
        this.cpuSharesRes = cpuSharesRes;
        this.memoryLimitRes = memoryLimitRes;
        this.dnsRes = dnsRes;
        this.extraHostsRes = extraHostsRes;
        this.networkModeRes = networkModeRes;
        this.portBindingsRes = portBindingsRes;
        this.bindMountsRes = bindMountsRes;
        this.alwaysRestart = alwaysRestart;
        this.publishAllPorts = publishAllPorts;
        this.privileged = privileged;
    }

    public String call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);
        
        CreateContainerCmd cfgCmd = client.createContainerCmd(imageRes);
        if (commandRes != null) {
            cfgCmd.withCmd(commandRes);
        }
        cfgCmd.withHostName(hostNameRes);
        cfgCmd.withName(containerNameRes);
        HostConfig hc = new HostConfig();
        cfgCmd.withLinks(LinkUtils.parseLinks(linksRes).getLinks());
        if (envVarsRes != null) {
            cfgCmd.withEnv(envVarsRes);
        }
        if (exposedPortsRes != null && !exposedPortsRes.isEmpty()) {
        	final ExposedPort[] ports;
            String[] exposedPortsSplitted = exposedPortsRes.split(",");
            ports = new ExposedPort[exposedPortsSplitted.length];
            for (int i = 0; i < ports.length; i++) {
                ports[i] = ExposedPort.parse(exposedPortsSplitted[i]);
            }

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
        if (networkModeRes != null) {
            cfgCmd.withNetworkMode(networkModeRes);
        }
        if (portBindingsRes != null) {
            cfgCmd.withPortBindings(PortBindingParser.parse(portBindingsRes));
        }
        if (bindMountsRes != null) {
            cfgCmd.withBinds(BindParser.parse(bindMountsRes));
        }
        if (alwaysRestart) {
            cfgCmd.withRestartPolicy(RestartPolicy.alwaysRestart());
        }
        
        CreateContainerResponse resp = cfgCmd.withPublishAllPorts(publishAllPorts).withPrivileged(privileged).exec();
        InspectContainerResponse inspectResp = client.inspectContainerCmd(resp.getId()).exec();
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        String serialized = mapper.writeValueAsString(inspectResp);
        return serialized;
    }
}
