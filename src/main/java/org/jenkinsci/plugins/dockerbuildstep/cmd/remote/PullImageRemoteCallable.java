package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.Serializable;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;

import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.remoting.Callable;


/**
 * A Callable wrapping the pull command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class PullImageRemoteCallable implements Callable<Void, Exception>, Serializable {

    private static final long serialVersionUID = 1536648869989705828L;
    
    BuildListener listener;
    
    Config cfgData;
    Descriptor<?> descriptor;
    AuthConfig authConfig;
    
    String fromImageRes;
    
    public PullImageRemoteCallable(BuildListener listener, Config cfgData, Descriptor<?> descriptor, AuthConfig authConfig, String fromImageRes) {
        this.listener = listener;
    	this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.authConfig = authConfig;
        this.fromImageRes = fromImageRes;
    }

    public Void call() throws Exception {
        final ConsoleLogger console = new ConsoleLogger(listener);
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, authConfig);

        PullImageCmd pullImageCmd = client.pullImageCmd(fromImageRes);
        PullImageResultCallback callback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                String text = item.getStream();
                if (text != null) {
                    console.logInfo(text);
                }
                super.onNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                console.logError("Failed to exec start:" + throwable.getMessage());
                super.onError(throwable);
            }
        };
        pullImageCmd.exec(callback).awaitSuccess();
        
        return null;
    }
    
}
