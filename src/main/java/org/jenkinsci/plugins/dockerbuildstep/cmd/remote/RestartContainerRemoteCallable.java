package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import com.github.dockerjava.api.DockerClient;
import hudson.model.Descriptor;
import hudson.remoting.Callable;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.remoting.RoleChecker;

import java.io.Serializable;

/**
 * A Callable wrapping the restart container command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class RestartContainerRemoteCallable implements Callable<Void, Exception>, Serializable {

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

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {

    }
}
