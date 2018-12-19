package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.remoting.Callable;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.PortUtils;
import org.jenkinsci.remoting.RoleChecker;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A Callable wrapping the inspect container command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class WaitForPortsRemoteCallable implements Callable<Void, Exception>, Serializable {

    private static final long serialVersionUID = 8479489609579635741L;

    BuildListener listener;
    
    Config cfgData;
    Descriptor<?> descriptor;
    
    String waitForPorts;
    
    public WaitForPortsRemoteCallable(BuildListener listener, Config cfgData, Descriptor<?> descriptor, String waitForPorts) {
        this.listener = listener;
    	this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.waitForPorts = waitForPorts;
    }
    
    public Void call() throws Exception {
        final ConsoleLogger console = new ConsoleLogger(listener);
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);
        
        Map<String, List<Integer>> containers = PortUtils.parsePorts(waitForPorts);
    	for (String cId : containers.keySet()) {
    		InspectContainerResponse response = client.inspectContainerCmd(cId).exec();
            String ip = response.getNetworkSettings().getIpAddress();
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
    	
    	return null;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {

    }
}
