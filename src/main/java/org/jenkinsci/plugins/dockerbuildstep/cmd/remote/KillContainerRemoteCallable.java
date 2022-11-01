package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;

import hudson.model.Descriptor;


/**
 * A Callable wrapping the kill container command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class KillContainerRemoteCallable extends MasterToSlaveCallable<Void, Exception> {

    private static final long serialVersionUID = 1536648869989705828L;

    Config cfgData;
    Descriptor<?> descriptor;

    String id;
    
    
    public KillContainerRemoteCallable(Config cfgData, Descriptor<?> descriptor, String id) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.id = id;
    }

    public Void call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);

        client.killContainerCmd(id).exec();
        
        return null;
    }
    
}
