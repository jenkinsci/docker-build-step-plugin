package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.CommitRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.exception.DockerException;

/**
 * This command commits changes done in specified container and create new image from it.
 * 
 * @see <a href="https://docs.docker.com/engine/api/v1.41/#tag/Image/operation/ImageCommit">Create a new image from a container</a>
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
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException, AbortException {
        // TODO check it when submitting the form
        if (containerId == null || containerId.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        String containerIdRes = Resolver.buildVar(build, containerId);
        String repoRes = Resolver.buildVar(build, repo);
        String tagRes = Resolver.buildVar(build, tag);
        String runCmdRes = Resolver.buildVar(build, runCmd);

        String imageId;
        try {
            Config cfgData = getConfig(build);
            Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
            
            imageId = launcher.getChannel().call(new CommitRemoteCallable(cfgData, descriptor, containerIdRes, repoRes, tagRes, runCmdRes));
        } catch (Exception e) {
            console.logError("Failed to commit image: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
        
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
