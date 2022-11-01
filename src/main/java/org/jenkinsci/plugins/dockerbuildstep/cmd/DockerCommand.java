package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.AbortException;
import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.Describable;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Job;

import java.io.IOException;

import jenkins.model.Jenkins;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryToken;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.DockerCredConfig;
import org.jenkinsci.plugins.dockerbuildstep.action.DockerContainerConsoleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.base.Strings;

/**
 * Parent class of all Docker commands.
 * 
 * @author vjuranek
 * 
 */
public abstract class DockerCommand implements Describable<DockerCommand>, ExtensionPoint {

    private DockerRegistryEndpoint dockerRegistryEndpoint;
    @Deprecated
    private DockerCredConfig dockerCredentials;

    public DockerCommand() {
        this(null);
    }

    public DockerCommand(DockerRegistryEndpoint dockerRegistryEndpoint) {
        this.dockerRegistryEndpoint = dockerRegistryEndpoint;
    }

    public DockerRegistryEndpoint getDockerRegistryEndpoint() {
        return dockerRegistryEndpoint;
    }

    @SuppressWarnings("deprecation")
    protected Object readResolve() {
        if (dockerCredentials != null) {
            this.dockerRegistryEndpoint = new DockerRegistryEndpoint(dockerCredentials.getServerAddress(),
                    dockerCredentials.getCredentialsId());
            this.dockerCredentials = null;
        }
        return this;
    }

    public AuthConfig getAuthConfig(Job<?, ?> project) {
        if (dockerRegistryEndpoint == null || Strings.isNullOrEmpty(dockerRegistryEndpoint.getCredentialsId())) {
            return null;
        }

        AuthConfig authConfig = new AuthConfig();
        authConfig.withRegistryAddress(dockerRegistryEndpoint.getUrl());
        DockerRegistryToken token = this.dockerRegistryEndpoint.getToken(project);
        if (token != null) {
            String credentials = new String(Base64.decodeBase64(token.getToken()), Charsets.UTF_8);
            String[] usernamePassword = credentials.split(":");
            authConfig.withUsername(usernamePassword[0]);
            authConfig.withPassword(usernamePassword[1]);
            authConfig.withEmail(token.getEmail());
        }

        return authConfig;
    }

    public static final CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers.anyOf(CredentialsMatchers
            .instanceOf(StandardUsernamePasswordCredentials.class));

    public abstract void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException, AbortException;

    protected static DockerClient getClient(AbstractBuild<?,?> build, AuthConfig authConfig) {
        return ((DockerBuilder.DescriptorImpl) Jenkins.getInstance().getDescriptor(DockerBuilder.class))
                .getDockerClient(build, authConfig);
    }

    public static DockerClient getClient(Descriptor<?> descriptor, String dockerUrlRes, String dockerVersionRes, String dockerCertPathRes, AuthConfig authConfig) {
        return ((DockerBuilder.DescriptorImpl) descriptor)
                .getDockerClient(dockerUrlRes, dockerVersionRes, dockerCertPathRes, authConfig);
    }

    protected static Config getConfig(AbstractBuild<?,?> build) {
        return ((DockerBuilder.DescriptorImpl) Jenkins.getInstance().getDescriptor(DockerBuilder.class))
                .getConfig(build);
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
    protected static DockerContainerConsoleAction attachContainerOutput(
            @SuppressWarnings("rawtypes") AbstractBuild build, String containerId) throws DockerException {
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

        public DockerRegistryEndpoint.DescriptorImpl getDockerRegistryEndpointDescriptor() {
            return (DockerRegistryEndpoint.DescriptorImpl) Jenkins.getInstance().getDescriptor(
                    DockerRegistryEndpoint.class);
        }

        public String getInfoString() {
            return "Info from DockerCommand.DockerCommandDescriptor";
        }

        // To make a subclass docker command support credentials, do the following steps:
        // 1. In command's subclass, add DockerRegistryEndpoint dockerRegistryEndpoint to its
        // data bound constructor, then call super(dockerRegistryEndpoint).
        // 2. In command's subclass' descriptor, override showCredentials() and return true
        // 3. In command's subclass' exectue(...), do:
        // subclassCmd.withAuthconfig(getAuthConfig(build.getParent())
        public boolean showCredentials() {
            return false;
        }
    }
}
