package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;

import hudson.model.Descriptor;


/**
 * A Callable wrapping the start container command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class StartContainerRemoteCallable extends MasterToSlaveCallable<String, Exception> {

    private static final long serialVersionUID = 8479489609579635741L;

    Config cfgData;
    Descriptor<?> descriptor;
    
    String id;
    
    public StartContainerRemoteCallable(Config cfgData, Descriptor<?> descriptor, String id) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.id = id;
    }
    
    public String call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);

        client.startContainerCmd(id).exec();
        InspectContainerResponse inspectResp = client.inspectContainerCmd(id).exec();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        String serialized = mapper.writeValueAsString(inspectResp);
        return serialized;
    }
    
}
