package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.AbstractBuild;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.CommitConfig;

/**
 * This command commits changes done in specified container and create new image from it.
 * 
 * @see http://docs.docker.io/en/latest/reference/api/docker_remote_api_v1.8/#create-a-new-image-from-a-container-s-changes
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

        CommitConfig cfg = new CommitConfig(containerId);
        cfg.setRepo(repo);
        cfg.setTag(tag);
        cfg.setRun(runCmd);
        String imageId = getClient().commit(cfg);

        console.logInfo("Container " + containerId + " commited as image " + imageId);
    }

    @Extension
    public static class CommitCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Commit container changes";
        }
    }

}
