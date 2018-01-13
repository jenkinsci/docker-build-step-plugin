package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.Serializable;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;

import hudson.model.Descriptor;
import hudson.remoting.Callable;


/**
 * A Callable wrapping the remove image command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class RemoveImageRemoteCallable implements Callable<Void, Exception>, Serializable {

    private static final long serialVersionUID = 1536648869989705828L;

    Config cfgData;
    Descriptor<?> descriptor;
    
    String imageNameRes;
    String imageIdRes;
    
    public RemoveImageRemoteCallable(Config cfgData, Descriptor<?> descriptor, String imageNameRes, String imageIdRes) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.imageNameRes = imageNameRes;
        this.imageIdRes = imageIdRes;
    }

    public Void call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);

        if (imageIdRes == null || imageIdRes.isEmpty()) {
            client.removeImageCmd(imageNameRes).exec();
        } else {
            client.removeImageCmd(imageNameRes).withImageId(imageIdRes).exec();
        }
        
        return null;
    }
    
}
