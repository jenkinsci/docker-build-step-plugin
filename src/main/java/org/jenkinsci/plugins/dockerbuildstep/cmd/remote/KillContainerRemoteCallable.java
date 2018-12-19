package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import com.github.dockerjava.api.DockerClient;
import hudson.model.Descriptor;
import hudson.remoting.Callable;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.remoting.RoleChecker;

import java.io.Serializable;

/**
 * A Callable wrapping the kill container command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class KillContainerRemoteCallable implements Callable<Void, Exception>, Serializable {

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

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {

    }
}
