package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.Serializable;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;

import hudson.model.Descriptor;
import hudson.remoting.Callable;


/**
 * A Callable wrapping the stop container command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class StopContainerRemoteCallable implements Callable<Void, Exception>, Serializable {

    private static final long serialVersionUID = -2282315761017156001L;
    
    Config cfgData;
    Descriptor<?> descriptor;
    
    String id;
    
    public StopContainerRemoteCallable(Config cfgData, Descriptor<?> descriptor, String id) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.id = id;
    }

    public Void call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);

        //TODO check, if container is actually running
        client.stopContainerCmd(id).exec();
        
        return null;
    }
    
}
