package org.jenkinsci.plugins.dockerbuildstep;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.core.SSLConfig;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;
import hudson.AbortException;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand.DockerCommandDescriptor;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Build step which executes various Docker commands via Docker REST API.
 *
 * @author vjuranek
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

        if (getDescriptor().getDockerClient(build, null) == null) {
            clog.logError("docker client is not initialized, command '" + dockerCmd.getDescriptor().getDisplayName()
                    + "' was aborted. Check Jenkins server log which Docker client wasn't initialized");
            throw new AbortException("Docker client wasn't initialized.");
        }

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
        private String dockerVersion;
        private String dockerCertPath;

        public DescriptorImpl() {
            load();

            if (isEmpty(dockerUrl)) {
                LOGGER.warning("Docker URL is not set, docker client won't be initialized");
                return;
            }

            try {
                getDockerClient(null, null);
            } catch (Exception e) {
                LOGGER.warning("Cannot create Docker client: " + e.getCause());
            }
        }

        private static DockerClient createDockerClient(String dockerUrl, String dockerVersion, String dockerCertPath,
                                                       AuthConfig authConfig) {
            // TODO JENKINS-26512
            SSLConfig dummySSLConf = (new SSLConfig() {
                public SSLContext getSSLContext() throws KeyManagementException, UnrecoverableKeyException,
                        NoSuchAlgorithmException, KeyStoreException {
                    return null;
                }
            });

            if (dockerCertPath != null) {
                dummySSLConf = new LocalDirectorySSLConfig(dockerCertPath);
            }
            
            DefaultDockerClientConfig.Builder configBuilder = new DefaultDockerClientConfig.Builder().withDockerHost(dockerUrl)
                    .withApiVersion(dockerVersion).withCustomSslConfig(dummySSLConf);
            if (authConfig != null) {
                configBuilder.withRegistryUsername(authConfig.getUsername())
                        .withRegistryEmail(authConfig.getEmail())
                        .withRegistryPassword(authConfig.getPassword())
                        .withRegistryUrl(authConfig.getRegistryAddress());
            }
            ClassLoader classLoader = Jenkins.getInstance().getPluginManager().uberClassLoader;
            // using jaxrs/jersey implementation here (netty impl is also available)
            DockerCmdExecFactory dockerCmdExecFactory = new DockerCmdExecFactoryImpl()
                    .withConnectTimeout(1000)
                    .withMaxTotalConnections(1)
                    .withMaxPerRouteConnections(1);
            return DockerClientBuilder.getInstance(configBuilder).withDockerCmdExecFactory(dockerCmdExecFactory).build();
        }

        public FormValidation doTestConnection(@QueryParameter String dockerUrl, @QueryParameter String dockerVersion, @QueryParameter String dockerCertPath) {
            LOGGER.fine(String.format("Trying to get client for %s and version %s and cert path %s", dockerUrl, dockerVersion, dockerCertPath));
            try {
                this.dockerUrl = dockerUrl;
                this.dockerVersion = dockerVersion;
                this.dockerCertPath = dockerCertPath;
                DockerClient dockerClient = getDockerClient(null, null);
                dockerClient.pingCmd().exec();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                return FormValidation.error("Something went wrong, cannot connect to " + dockerUrl + ", cause: "
                        + e.getCause());
            }
            return FormValidation.ok("Connected to " + dockerUrl);
        }

        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Execute Docker command";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            dockerUrl = formData.getString("dockerUrl");
            dockerVersion = formData.getString("dockerVersion");
            dockerCertPath = formData.getString("dockerCertPath");

            if (isBlank(dockerUrl)) {
                LOGGER.severe("Docker URL is empty, Docker build test plugin cannot work without Docker URL being set up properly");
                // JENKINS-23733 doen't block user to save the config if admin decides so
                return true;
            }

            save();

            try {
                getDockerClient(null, null);
            } catch (Exception e) {
                LOGGER.warning("Cannot create Docker client: " + e.getCause());
            }
            return super.configure(req, formData);
        }

        public String getDockerUrl() {
            return dockerUrl;
        }

        public String getDockerVersion() {
            return dockerVersion;
        }

        public String getDockerCertPath() {
            return dockerCertPath;
        }

        public DockerClient getDockerClient(AuthConfig authConfig) {
            // Reason to return a new DockerClient each time this function is called:
            // - It is a legitimate scenario that different jobs or different build steps
            //   in the same job may need to use one credential to connect to one 
            //   docker registry but needs another credential to connect to another docker
            //   registry.
            // - Recent docker-java client made some changes so that it requires valid
            //   AuthConfig to be provided when DockerClient is created for certain commands
            //   when auth is needed. We don't have control on how docker-java client is
            //   implemented.
            // So to satisfy thread safety on the returned DockerClient
            // (when different AuthConfig are are needed), it is better to return a new 
            // instance each time this function is called.
            return createDockerClient(dockerUrl, dockerVersion, dockerCertPath, authConfig);
        }

        public DockerClient getDockerClient(AbstractBuild<?, ?> build, AuthConfig authConfig) {
            String dockerUrlRes = build == null ? Resolver.envVar(dockerUrl) : Resolver.buildVar(build, dockerUrl);
            String dockerVersionRes = build == null ? Resolver.envVar(dockerVersion) : Resolver.buildVar(build, dockerVersion);
            String dockerCertPathRes = build == null ? Resolver.envVar(dockerCertPath) : Resolver.buildVar(build, dockerCertPath);
            return createDockerClient(dockerUrlRes, dockerVersionRes, dockerCertPathRes, authConfig);
        }

        public DescriptorExtensionList<DockerCommand, DockerCommandDescriptor> getCmdDescriptors() {
            return DockerCommand.all();
        }

    }

    private static Logger LOGGER = Logger.getLogger(DockerBuilder.class.getName());

}
