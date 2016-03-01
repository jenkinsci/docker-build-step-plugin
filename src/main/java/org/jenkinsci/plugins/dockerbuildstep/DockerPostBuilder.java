package org.jenkinsci.plugins.dockerbuildstep;

import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixProject;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.cmd.RemoveCommand;
import org.jenkinsci.plugins.dockerbuildstep.cmd.StopCommand;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.NotModifiedException;

/**
 * Post build step which stops and removes the Docker container. Use to cleanup container(s) in case of a build failure.
 * 
 */
@Extension
public class DockerPostBuilder extends BuildStepDescriptor<Publisher> {

    public DockerPostBuilder() {
        super(DockerPostBuildStep.class);
    }

    @Override
    public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
        return FreeStyleProject.class.equals(jobType) || MatrixProject.class.equals(jobType);;
    }

    @Override
    public String getDisplayName() {
        return "Stop and remove Docker container";
    }

    public static class DockerPostBuildStep extends Recorder {

        private final String containerIds;
        private final boolean removeVolumes;
        private final boolean force;

        @DataBoundConstructor
        public DockerPostBuildStep(String containerIds, boolean removeVolumes, boolean force) {
            this.containerIds = containerIds;
            this.removeVolumes = removeVolumes;
            this.force = force;
        }

        public BuildStepMonitor getRequiredMonitorService() {
            return BuildStepMonitor.NONE;
        }

        public String getContainerIds() {
            return containerIds;
        }

        public boolean isRemoveVolumes() {
            return removeVolumes;
        }

        public boolean isForce() {
            return force;
        }

        @Override
        public boolean perform(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher,
                BuildListener listener) throws InterruptedException, IOException {
            ConsoleLogger clog = new ConsoleLogger(listener);

            List<String> ids = Arrays.asList(containerIds.split(","));
            for (String id : ids) {
                StopCommand stopCommand = new StopCommand(id);
                try {
                    stopCommand.execute(build, clog);
                } catch (NotFoundException e) {
                    clog.logWarn("unable to stop container id " + id + ", container not found!");
                } catch (NotModifiedException e) {
                    // ignore, container already stopped
                }
            }

            RemoveCommand removeCommand = new RemoveCommand(containerIds, true, removeVolumes, force);
            removeCommand.execute(build, clog);

            return true;
        }
    }
}
