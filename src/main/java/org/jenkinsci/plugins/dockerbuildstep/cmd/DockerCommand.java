package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

public abstract class DockerCommand implements Describable<DockerCommand>, ExtensionPoint {

	public abstract void execute(@SuppressWarnings("rawtypes") AbstractBuild build, BuildListener listener) throws DockerException ;
	
	protected static DockerClient getClient() {
		return ((DockerBuilder.DescriptorImpl)Jenkins.getInstance().getDescriptor(DockerBuilder.class)).getDockerClient();
	}
	
	public DockerCommandDescriptor getDescriptor() {
        return (DockerCommandDescriptor)Jenkins.getInstance().getDescriptor(getClass());
    }

    public static DescriptorExtensionList<DockerCommand,DockerCommandDescriptor> all() {
        return Jenkins.getInstance().<DockerCommand,DockerCommandDescriptor>getDescriptorList(DockerCommand.class);
    }
	
    
    public abstract static class DockerCommandDescriptor extends Descriptor<DockerCommand> {
        protected DockerCommandDescriptor(Class<? extends DockerCommand> clazz) {
            super(clazz);
        }

        protected DockerCommandDescriptor() {
        }
    }
}
