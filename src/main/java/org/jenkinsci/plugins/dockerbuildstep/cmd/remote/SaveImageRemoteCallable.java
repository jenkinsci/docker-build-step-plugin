package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;

import hudson.model.Descriptor;


/**
 * A Callable wrapping the save image command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class SaveImageRemoteCallable extends MasterToSlaveCallable<Void, Exception> {

    private static final long serialVersionUID = -6899484703281434847L;

    Config cfgData;
    Descriptor<?> descriptor;
    
    String destinationRes;
    String filenameRes;
    String imageNameRes;
    String imageTagRes;
    
    public SaveImageRemoteCallable(Config cfgData, Descriptor<?> descriptor, String destinationRes, String filenameRes,
            String imageNameRes, String imageTagRes) {
        super();
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.destinationRes = destinationRes;
        this.filenameRes = filenameRes;
        this.imageNameRes = imageNameRes;
        this.imageTagRes = imageTagRes;
    }

    public Void call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);
        
        if (!new File(destinationRes).exists()) {
            throw new IllegalArgumentException(
                    "Destination is not a valid path");
        }
        
        final OutputStream output = new FileOutputStream(new File(
                destinationRes + "/" + filenameRes));

        IOUtils.copy(client.saveImageCmd(imageNameRes + ":" + imageTagRes)
                .exec(), output);

        IOUtils.closeQuietly(output);
        
        return null;
    }
    
}