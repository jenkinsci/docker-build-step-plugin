package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;

import hudson.model.Descriptor;


/**
 * A Callable wrapping the restart container command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class RestartContainerRemoteCallable extends MasterToSlaveCallable<Void, Exception> {

    private static final long serialVersionUID = 1536648869989705828L;

    Config cfgData;
    Descriptor<?> descriptor;
    
    String id;
    Integer timeout;
    
    public RestartContainerRemoteCallable(Config cfgData, Descriptor<?> descriptor, String id, Integer timeout) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.id = id;
        this.timeout = timeout;
    }

    public Void call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);

        if (timeout == null) {
            client.restartContainerCmd(id).exec();
        } else {
            client.restartContainerCmd(id).withtTimeout(timeout).exec();
        }
        
        return null;
    }
    
}
