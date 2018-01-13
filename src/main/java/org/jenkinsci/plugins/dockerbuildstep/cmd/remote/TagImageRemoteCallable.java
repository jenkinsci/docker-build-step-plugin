package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.io.Serializable;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;

import hudson.model.Descriptor;
import hudson.remoting.Callable;

/**
 * A Callable wrapping the tag image command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class TagImageRemoteCallable implements Callable<Void, Exception>, Serializable {

    private static final long serialVersionUID = -6899484703281434847L;

    Config cfgData;
    Descriptor<?> descriptor;
    
    String imageRes;
    String repositoryRes;
    String tagRes;
    boolean withForce;
    
    public TagImageRemoteCallable(Config cfgData, Descriptor<?> descriptor, String imageRes, String repositoryRes, String tagRes, boolean withForce) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.imageRes = imageRes;
        this.repositoryRes = repositoryRes;
        this.tagRes = tagRes;
        this.withForce = withForce;
    }

    public Void call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);
        
        client.tagImageCmd(imageRes, repositoryRes, tagRes).withForce(withForce).exec();
        
        return null;
    }
    
}