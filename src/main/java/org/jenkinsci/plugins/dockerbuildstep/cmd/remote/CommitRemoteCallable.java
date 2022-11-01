package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CommitCmd;

import hudson.model.Descriptor;


/**
 * A Callable wrapping the commit command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class CommitRemoteCallable extends MasterToSlaveCallable<String, Exception> {
    
    private static final long serialVersionUID = -8663454265047375486L;

    Config cfgData;
    Descriptor<?> descriptor;
    
    String containerIdRes;
    String repoRes;
    String tagRes;
    String runCmdRes;
    
    public CommitRemoteCallable(Config cfgData, Descriptor<?> descriptor, String containerIdRes, String repoRes, String tagRes, String runCmdRes) {
        this.cfgData = cfgData;
        this.descriptor = descriptor;
        this.containerIdRes = containerIdRes;
        this.repoRes = repoRes;
        this.tagRes = tagRes;
        this.runCmdRes = runCmdRes;
    }

    public String call() throws Exception {
        DockerClient client = DockerCommand.getClient(descriptor, cfgData.dockerUrlRes, cfgData.dockerVersionRes, cfgData.dockerCertPathRes, null);
        CommitCmd commitCmd =
                client.commitCmd(containerIdRes).withRepository(repoRes).withTag(tagRes).withCmd(runCmdRes);
        String imageId = commitCmd.exec();
        return imageId;
    }
}
