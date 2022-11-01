package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PushImageResultCallback;

import hudson.model.BuildListener;
import hudson.model.Descriptor;


/**
 * A Callable wrapping the push command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class PushImageRemoteCallable extends MasterToSlaveCallable<Void, Exception> {

    private static final long serialVersionUID = 1536648869989705828L;

    BuildListener listener;
    
    Config cfgData;
    Descriptor<?> descriptor;
    AuthConfig authConfig;
    
    String imageRes;
    String tagRes;

    public PushImageRemoteCallable(BuildListener listener, Config cfgData, Descriptor<?> descriptor, AuthConfig authConfig, String imageRes, String tagRes) {
        this.listener = listener;
    	this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.authConfig = authConfig;
        this.imageRes = imageRes;
        this.tagRes = tagRes;
    }

    public Void call() throws Exception {
        final ConsoleLogger console = new ConsoleLogger(listener);
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, authConfig);

        PushImageCmd pushImageCmd = client.pushImageCmd(imageRes).withTag(tagRes);
        PushImageResultCallback callback = new PushImageResultCallback() {
            @Override
            public void onNext(PushResponseItem item) {
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
        pushImageCmd.exec(callback).awaitSuccess();
        
        return null;
    }
    
}
