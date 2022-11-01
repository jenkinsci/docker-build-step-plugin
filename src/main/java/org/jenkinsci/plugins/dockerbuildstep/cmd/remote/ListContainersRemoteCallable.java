package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import java.util.List;

import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

import hudson.model.Descriptor;


/**
 * A Callable wrapping the list containers command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class ListContainersRemoteCallable extends MasterToSlaveCallable<List<Container>, Exception> {

    private static final long serialVersionUID = 8479489609579635741L;

    Config cfgData;
    Descriptor<?> descriptor;
    
    boolean showAll;
    
    public ListContainersRemoteCallable(Config cfgData, Descriptor<?> descriptor, boolean showAll) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.showAll = showAll;
    }
    
    public List<Container> call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);

        List<Container> containers = client.listContainersCmd().withShowAll(showAll).exec();
        return containers;
    }
    
}
