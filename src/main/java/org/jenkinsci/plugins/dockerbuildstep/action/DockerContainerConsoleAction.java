package org.jenkinsci.plugins.dockerbuildstep.action;

import hudson.console.AnnotatedLargeText;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.model.TaskThread;
import hudson.model.AbstractBuild;
import hudson.model.TaskAction;
import hudson.security.ACL;
import hudson.security.Permission;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;

import jenkins.model.Jenkins;

import org.apache.commons.io.IOUtils;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.log.container.DockerLogMessage;
import org.jenkinsci.plugins.dockerbuildstep.log.container.DockerLogStreamReader;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.AttachContainerCmd;
import com.google.common.base.Charsets;
import com.jcraft.jzlib.GZIPInputStream;

/**
 * Jenkins action to add a 'Console Output' like page for the Docker container output. 
 * Container output is gathered using the {@link AttachContainerCmd}.
 */
public class DockerContainerConsoleAction extends TaskAction implements Serializable {
	private static final long serialVersionUID = 1L;

	private final AbstractBuild<?, ?> build;

	private final String containerId;
	
	private String containerName;

	public DockerContainerConsoleAction(AbstractBuild<?, ?> build, String containerId) {
		super();
		this.build = build;
		this.containerId = containerId;
	}

	public String getIconFileName() {
		return Jenkins.RESOURCE_PATH+"/plugin/docker-build-step/icons/docker-icon-20x20.png";
	}

	public String getDisplayName() {
		if (containerName != null && !isSingleContainerBuild()) {
			return (containerName.startsWith("/") ? containerName.substring(1) : containerName) + " Output";
		}		
		return "Container Output";
	}
	
	private boolean isSingleContainerBuild(){
		return build.getActions(DockerContainerConsoleAction.class).size() == 1;
	}

	public String getFullDisplayName() {
		return build.getFullDisplayName() + ' ' + getDisplayName();
	}

	public String getUrlName() {
		return "dockerconsole_" + containerId;
	}

	public AbstractBuild<?, ?> getOwner() {
		return this.build;
	}

	@Override
	protected Permission getPermission() {
		return Item.READ;
	}

	@Override
	protected ACL getACL() {
		return build.getACL();
	}

	public String getBuildStatusUrl() {
		return build.getIconColor().getImage();
	}
	
	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public File getLogFile() {
		return new File(build.getRootDir(), "docker_" + containerId + ".log");
	}

	@Override
	public AnnotatedLargeText obtainLog() {
		return new AnnotatedLargeText(getLogFile(), Charsets.UTF_8,
				!isLogUpdated(), this);
	}

	public boolean isLogUpdated() {
		return workerThread != null;
	}

	public InputStream getLogInputStream() throws IOException {
		File logFile = getLogFile();

		if (logFile != null && logFile.exists()) {
			// Checking if a ".gz" file was return
			FileInputStream fis = new FileInputStream(logFile);
			if (logFile.getName().endsWith(".gz")) {
				return new GZIPInputStream(fis);
			} else {
				return fis;
			}
		}

		String message = "No such file: " + logFile;
		return new ByteArrayInputStream(message.getBytes(Charsets.UTF_8));
	}

	public void writeLogTo(long offset, XMLOutput out) throws IOException {
		try {
			obtainLog().writeHtmlTo(offset, out.asWriter());
		} catch (IOException e) {
			// try to fall back to the old getLogInputStream()
			// mainly to support .gz compressed files
			// In this case, console annotation handling will be turned off.
			InputStream input = getLogInputStream();
			try {
				IOUtils.copy(input, out.asWriter());
			} finally {
				IOUtils.closeQuietly(input);
			}
		}
	}
	
	public DockerContainerConsoleAction start() throws IOException {
		workerThread = new DockerLogWorkerThread(getLogFile());
		workerThread.start();
		return this;
	}

	public void stop() {
		workerThread.interrupt();
		workerThread = null;
	}

	public final class DockerLogWorkerThread extends TaskThread {

		protected DockerLogWorkerThread(File logFile) throws IOException {
			super(DockerContainerConsoleAction.this, ListenerAndText.forFile(logFile,
					DockerContainerConsoleAction.this));
		}

		@Override
		protected void perform(final TaskListener listener) throws Exception {
			DockerClient client = ((DockerBuilder.DescriptorImpl) Jenkins
					.getInstance().getDescriptor(DockerBuilder.class))
					.getDockerClient(null);
			InputStream is = client.attachContainerCmd(containerId)
					.withFollowStream().withStdOut().withStdErr().exec();
			DockerLogStreamReader reader = new DockerLogStreamReader(is);

			OutputStreamWriter writer = new OutputStreamWriter(listener.getLogger(),
					Charsets.UTF_8);

			try {
				while (!isInterrupted() && build.isBuilding()) {
					process(reader, writer);
					Thread.sleep(2000);
				}
				process(reader, writer);
			} finally {
				if (writer != null) {
					writer.close();
				}
				if (reader != null) {
					reader.close();
				}
			}
			workerThread = null;
		}

		private void process(DockerLogStreamReader ls, OutputStreamWriter w)
				throws IOException {
			DockerLogMessage m;
			while ((m = ls.nextMessage()) != null) {
				w.append(Charsets.UTF_8.decode(m.content()));
				w.flush();
			}
		}
	}
}
