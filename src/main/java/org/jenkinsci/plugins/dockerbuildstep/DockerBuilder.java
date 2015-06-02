package org.jenkinsci.plugins.dockerbuildstep;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;
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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand.DockerCommandDescriptor;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;
import com.github.dockerjava.core.SSLConfig;

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

		if (getDescriptor().getDockerClient(null) == null) {
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
		public DescriptorImpl() {
			load();

			if (isEmpty(dockerUrl)) {
				LOGGER.warning("Docker URL is not set, docker client won't be initialized");
				return;
			}

			try {
				createDockerClient(dockerUrl, dockerVersion, null);
			} catch (DockerException e) {
				LOGGER.warning("Cannot create Docker client: " + e.getCause());
			}
		}

		private static DockerClient createDockerClient(String dockerUrl, String dockerVersion,
				AuthConfig authConfig) {
			// TODO JENKINS-26512
			SSLConfig dummySSLConf = (new SSLConfig() {
				public SSLContext getSSLContext() throws KeyManagementException, UnrecoverableKeyException,
						NoSuchAlgorithmException, KeyStoreException {
					return null;
				}
			});

			DockerClientConfigBuilder configBuilder = new DockerClientConfigBuilder().withUri(dockerUrl)
				.withVersion(dockerVersion).withSSLConfig(dummySSLConf)
	     			// Each Docker command will create its own docker client, which means
				// each client only needs 1 connection.
				.withMaxTotalConnections(1).withMaxPerRouteConnections(1);
			if (authConfig != null) {
				configBuilder.withUsername(authConfig.getUsername())
					.withEmail(authConfig.getEmail())
					.withPassword(authConfig.getPassword())
					.withServerAddress(authConfig.getServerAddress());
			}
			ClassLoader classLoader = Jenkins.getInstance().getPluginManager().uberClassLoader;
			return DockerClientBuilder.getInstance(configBuilder).withServiceLoaderClassLoader(classLoader).build();
		}

		public FormValidation doTestConnection(@QueryParameter String dockerUrl, @QueryParameter String dockerVersion) {
			LOGGER.fine(String.format("Trying to get client for %s and version %s", dockerUrl, dockerVersion));
			try {
				DockerClient dockerClient = createDockerClient(dockerUrl, dockerVersion, null);
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
			return "Execute Docker container";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			dockerUrl = formData.getString("dockerUrl");
			dockerVersion = formData.getString("dockerVersion");

			if (isBlank(dockerUrl)) {
				LOGGER.severe("Docker URL is empty, Docker build test plugin cannot work without Docker URL being set up properly");
				// JENKINS-23733 doen't block user to save the config if admin decides so
				return true;
			}

			save();

			try {
				createDockerClient(dockerUrl, dockerVersion, null);
			} catch (DockerException e) {
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
			return createDockerClient(dockerUrl, dockerVersion, authConfig);
		}

		public DescriptorExtensionList<DockerCommand, DockerCommandDescriptor> getCmdDescriptors() {
			return DockerCommand.all();
		}

	}

	private static Logger LOGGER = Logger.getLogger(DockerBuilder.class.getName());

}
