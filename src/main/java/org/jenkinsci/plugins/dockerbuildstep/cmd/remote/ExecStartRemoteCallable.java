package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.Serializable;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.ExecStartResultCallback;

import hudson.model.Descriptor;
import hudson.remoting.Callable;


/**
 * A Callable wrapping the exec start command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class ExecStartRemoteCallable implements Callable<Void, Exception>, Serializable {

    private static final long serialVersionUID = 1536648869989705828L;

    ConsoleLogger console;
    
    Config cfgData;
    Descriptor<?> descriptor;

    String cmdId;
    
    public ExecStartRemoteCallable(ConsoleLogger console, Config cfgData, Descriptor<?> descriptor, String cmdId) {
        this.console = console;
    	this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.cmdId = cmdId;
    }

    public Void call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);

        ExecStartResultCallback callback = new ExecStartResultCallback() {
            @Override
            public void onNext(Frame item) {
                console.logInfo(item.toString());
                super.onNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                console.logError("Failed to exec start:" + throwable.getMessage());
                super.onError(throwable);
            }
        };
        try {
            client.execStartCmd(cmdId).exec(callback).awaitCompletion();
        } catch (InterruptedException e) {
            console.logError("Failed to exec start:" + e.getMessage());
        }
        
        return null;
    }
    
}
