package org.jenkinsci.plugins.dockerbuildstep.cmd;

import java.io.IOException;

import com.google.common.base.Strings;

import hudson.AbortException;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerCredConfig;
import org.jenkinsci.plugins.dockerbuildstep.action.DockerContainerConsoleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;

import hudson.model.Job;
import hudson.util.Secret;
import hudson.security.ACL;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.model.AuthConfig;

/**
 * Parent class of all Docker commands.
 * 
 * @author vjuranek
 * 
 */
public abstract class DockerCommand implements Describable<DockerCommand>, ExtensionPoint {

    private DockerCredConfig dockerCredentials;

    public DockerCommand() {
        this(null);
    }

    public DockerCommand(DockerCredConfig dockerCredentials) {
        this.dockerCredentials = dockerCredentials;
    }

    public DockerCredConfig getDockerCredentials() {
        return dockerCredentials;
    }

    public AuthConfig getAuthConfig(Job project) {
        if (dockerCredentials == null || Strings.isNullOrEmpty(dockerCredentials.getCredentialsId())) {
            return null;
        }

        AuthConfig authConfig = new AuthConfig();
        authConfig.setServerAddress(dockerCredentials.getServerHost());

        StandardUsernamePasswordCredentials credentials = CredentialsMatchers
            .firstOrNull(
                CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, project,
                    ACL.SYSTEM, URIRequirementBuilder.fromUri(dockerCredentials.getServerAddress()).build()),
                CredentialsMatchers.allOf(CredentialsMatchers.withId(dockerCredentials.getCredentialsId()),
                    CREDENTIALS_MATCHER));

        if (credentials != null) {
            authConfig.setUsername(credentials.getUsername());
            authConfig.setPassword(Secret.toString(credentials.getPassword()));
            // TODO: email filed is not actually used by authentication, but
            // Docker java client requires this field. Use a dummy value for now,
            // Should extend a DockerCredentials from cloudbees credential type
            // which can return email field.
            authConfig.setEmail("dummy@dummy.com");
        }
        return authConfig;
    }

    public static CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers.anyOf(
        CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));

    public abstract void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException, AbortException;

    protected static DockerClient getClient(AuthConfig authConfig) {
        return ((DockerBuilder.DescriptorImpl) Jenkins.getInstance().getDescriptor(DockerBuilder.class))
                .getDockerClient(authConfig);
    }

    public DockerCommandDescriptor getDescriptor() {
        return (DockerCommandDescriptor) Jenkins.getInstance().getDescriptor(getClass());
    }

    public static DescriptorExtensionList<DockerCommand, DockerCommandDescriptor> all() {
        return Jenkins.getInstance().<DockerCommand, DockerCommandDescriptor> getDescriptorList(DockerCommand.class);
    }

    public String getInfoString() {
      return "Info from DockerCommand";
    }

    /**
     * Only the first container started is attached!
     */
    protected static DockerContainerConsoleAction attachContainerOutput(@SuppressWarnings("rawtypes") AbstractBuild build, String containerId)
    		throws DockerException {
   		try {
   			DockerContainerConsoleAction outAction = new DockerContainerConsoleAction(build, containerId).start(); 
   			build.addAction(outAction);
   			return outAction;
		} catch (IOException e) {
			throw new DockerException(e.getMessage(), 0);
		}
    }
    
    public abstract static class DockerCommandDescriptor extends Descriptor<DockerCommand> {
        protected DockerCommandDescriptor(Class<? extends DockerCommand> clazz) {
            super(clazz);
        }

        protected DockerCommandDescriptor() {
        }

        public DockerCredConfig.DescriptorImpl getDockerCredConfigDescriptor() {
          return (DockerCredConfig.DescriptorImpl) Jenkins.getInstance().getDescriptor(DockerCredConfig.class);
       }

       public String getInfoString() {
           return "Info from DockerCommand.DockerCommandDescriptor";
       }

       // To make a subclass docker command support credentials, do the following steps:
       // 1. In command's subclass, add DockerCredConfig dockerCredentials to its
       //    data bound constructor, then call super(dockerCredentials).
       // 2. In command's subclass' descriptor, override showCredentials() and return true
       // 3. In command's subclass' exectue(...), do:
       //    subclassCmd.withAuthconfig(getAuthConfig(build.getParent())
       public boolean showCredentials() {
           return false;
       }
    }
}
