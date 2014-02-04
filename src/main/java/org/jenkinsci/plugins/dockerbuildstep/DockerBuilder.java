package org.jenkinsci.plugins.dockerbuildstep;

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommands;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

public class DockerBuilder extends Builder {
	
	private DockerCommands dockerCmd;
	private String cmdParams;
	
	@DataBoundConstructor
	public DockerBuilder(DockerCommands dockerCmd, String cmdParams) {
		this.dockerCmd = dockerCmd;
		this.cmdParams = cmdParams;
	}
	
	public DockerCommands getDockerCmd() {
		return dockerCmd;
	}

	public String getCmdParams() {
		return cmdParams;
	}

	@Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws AbortException {
        DockerClient dockerClient = getDescriptor().getDockerClient();
        try {
        	dockerCmd.execute(dockerClient, cmdParams.split(","));
        } catch(DockerException e) {
        	LOGGER.severe("Failed to execute Docker command " + dockerCmd.getCmdName() + ": " + e.getMessage());
        	throw new AbortException(e.getMessage());
        }
        return true;
    }
	
	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

	@Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        
        private String dockerUrl;
        private transient DockerClient dockerClient;

       
        public DescriptorImpl() {
            load();
            dockerClient =  new DockerClient(dockerUrl);
        }

        public FormValidation doCheckName(@QueryParameter String dockerURL)
                throws IOException, ServletException {
        	//TODO do validation
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) { 
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
            return super.configure(req,formData);
        }

        public String getDockerUrl() {
            return dockerUrl;
        }
        
        public DockerClient getDockerClient() {
        	return dockerClient;
        }
        
        public ListBoxModel doFillDockerCmdItems() {
            ListBoxModel commands = new ListBoxModel();
            for (DockerCommands cmd : DockerCommands.values()) {
            	commands.add(cmd.getCmdName(), cmd.toString());
            }
            return commands;
        }
    }
	
	private static Logger LOGGER = Logger.getLogger(DockerBuilder.class.getName());
	
}
