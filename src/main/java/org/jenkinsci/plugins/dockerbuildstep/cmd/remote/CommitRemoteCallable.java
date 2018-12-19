package org.jenkinsci.plugins.dockerbuildstep.cmd.remote;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CommitCmd;
import hudson.model.Descriptor;
import hudson.remoting.Callable;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.remoting.RoleChecker;

import java.io.Serializable;

/**
 * A Callable wrapping the commit command.
 * It can be sent through a Channel to execute on the correct build node.
 * 
 * @author David Csakvari
 */
public class CommitRemoteCallable implements Callable<String, Exception>, Serializable {
    
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

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {

    }
}
