package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.AbstractBuild;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.CommitCmd;

/**
 * This command commits changes done in specified container and create new image from it.
 * 
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#create-a-new-image-from-a-containers-changes
 * 
 * @author vjuranek
 * 
 */
public class CommitCommand extends DockerCommand {

    private final String containerId;
    private final String repo;
    private final String tag;
    private final String runCmd;

    @DataBoundConstructor
    public CommitCommand(String containerId, String repo, String tag, String runCmd) {
        this.containerId = containerId;
        this.repo = repo;
        this.tag = tag;
        this.runCmd = runCmd;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getRepo() {
        return repo;
    }

    public String getTag() {
        return tag;
    }

    public String getRunCmd() {
        return runCmd;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException, AbortException {
        // TODO check it when submitting the form
        if (containerId == null || containerId.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        String containerIdRes = Resolver.buildVar(build, containerId);
        String repoRes = Resolver.buildVar(build, repo);
        String tagRes = Resolver.buildVar(build, tag);
        String runCmdRes = Resolver.buildVar(build, runCmd);

        DockerClient client = getClient(build, null);
        CommitCmd commitCmd =
                client.commitCmd(containerIdRes).withRepository(repoRes).withTag(tagRes).withCmd(runCmdRes);
        String imageId = commitCmd.exec();

        console.logInfo("Container " + containerIdRes + " commited as image " + imageId);
    }

    @Extension
    public static class CommitCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Commit container changes";
        }
    }

}
