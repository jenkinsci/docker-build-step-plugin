package org.jenkinsci.plugins.dockerbuildstep;

import hudson.AbortException;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand.DockerCommandDescriptor;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

/**
 * Build step which executes various Docker commands via Docker REST API.
 * 
 * @author vjuranek
 * 
 */
public class DockerBuilder extends Builder {

    private DockerCommand dockerCmd;

    @DataBoundConstructor
    public DockerBuilder(DockerCommand dockerCmd) {
        this.dockerCmd = dockerCmd;
    }

    public DockerCommand getDockerCmd() {
        return dockerCmd;
    }

    @Override
    public boolean perform(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener)
            throws AbortException {
        
        ConsoleLogger clog = new ConsoleLogger(listener);
        try {
             
            dockerCmd.execute(build, clog);
        } catch (DockerException e) {
            clog.logError("command '" + dockerCmd.getDescriptor().getDisplayName() + "' failed: " + e.getMessage());
            LOGGER.severe("Failed to execute Docker command " + dockerCmd.getDescriptor().getDisplayName() + ": "
                    + e.getMessage());
            throw new AbortException(e.getMessage());
        }
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private String dockerUrl;
        private transient DockerClient dockerClient;

        public DescriptorImpl() {
            load();
            dockerClient = new DockerClient(dockerUrl);
        }

        public FormValidation doCheckName(@QueryParameter String dockerURL) throws IOException, ServletException {
            // TODO do validation
            return FormValidation.ok();
        }

        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Execute Docker container";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            dockerUrl = formData.getString("dockerUrl");
            save();
            dockerClient = new DockerClient(dockerUrl);
            return super.configure(req, formData);
        }

        public String getDockerUrl() {
            return dockerUrl;
        }

        public DockerClient getDockerClient() {
            return dockerClient;
        }

        public DescriptorExtensionList<DockerCommand, DockerCommandDescriptor> getCmdDescriptors() {
            return DockerCommand.all();
        }

    }

    private static Logger LOGGER = Logger.getLogger(DockerBuilder.class.getName());

}
