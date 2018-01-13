package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.Serializable;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;

import hudson.model.Descriptor;
import hudson.remoting.Callable;


/**
 * A Callable wrapping the remove container command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class RemoveContainerRemoteCallable implements Callable<Void, Exception>, Serializable {

    private static final long serialVersionUID = 1536648869989705828L;

    Config cfgData;
    Descriptor<?> descriptor;

    String id;
    boolean force;
    boolean removeVolumes;
    
    
    public RemoveContainerRemoteCallable(Config cfgData, Descriptor<?> descriptor, String id, boolean force, boolean removeVolumes) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.id = id;
        this.force = force;
        this.removeVolumes = removeVolumes;
    }

    public Void call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);

        client.removeContainerCmd(id).withForce(force).withRemoveVolumes(removeVolumes).exec();
                
        return null;
    }
    
}
