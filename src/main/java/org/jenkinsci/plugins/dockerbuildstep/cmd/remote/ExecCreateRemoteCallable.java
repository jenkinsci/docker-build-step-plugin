package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.Serializable;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;

import hudson.model.Descriptor;
import hudson.remoting.Callable;


/**
 * A Callable wrapping the exec create command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class ExecCreateRemoteCallable implements Callable<String, Exception>, Serializable {

    private static final long serialVersionUID = 1536648869989705828L;

    Config cfgData;
    Descriptor<?> descriptor;

    String id;
    String[] cmd;
    boolean withAttachStdoutAndStderr;
    
    public ExecCreateRemoteCallable(Config cfgData, Descriptor<?> descriptor, String id, String[] cmd, boolean withAttachStdoutAndStderr) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.id = id;
        this.cmd = cmd;
        this.withAttachStdoutAndStderr = withAttachStdoutAndStderr;
    }

    public String call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);

        final ExecCreateCmdResponse response;
        if (withAttachStdoutAndStderr) {
            response = client.execCreateCmd(id).withCmd(cmd).withAttachStderr(true).withAttachStdout(true).exec();
        } else {
            response = client.execCreateCmd(id).withCmd(cmd).exec();
        }
        
        return response.getId();
    }

}
