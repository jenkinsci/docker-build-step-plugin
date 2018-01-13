package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.Serializable;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;

import hudson.model.Descriptor;
import hudson.remoting.Callable;


/**
 * A Callable wrapping the start container command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class StartContainerRemoteCallable implements Callable<InspectContainerResponse, Exception>, Serializable {

    private static final long serialVersionUID = 8479489609579635741L;

    Config cfgData;
    Descriptor<?> descriptor;
    
    String id;
    
    public StartContainerRemoteCallable(Config cfgData, Descriptor<?> descriptor, String id) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.id = id;
    }
    
    public InspectContainerResponse call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);

        client.startContainerCmd(id);
        InspectContainerResponse inspectResp = client.inspectContainerCmd(id).exec();

        return inspectResp;
    }
    
}
