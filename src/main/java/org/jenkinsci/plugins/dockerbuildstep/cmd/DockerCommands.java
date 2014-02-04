package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

public enum DockerCommands {

	START("Start container", new StartCommand()), //TODO replace DockerCommand by abstract class and static methods?
	STOP("Stop container", new StopCommand());
	
	private final String cmdName;
	private final DockerCommand cmd;
	
	
	private DockerCommands(String cmdName, DockerCommand cmd) {
		this.cmdName = cmdName;
		this.cmd = cmd;
	}
	
	public String getCmdName() {
		return cmdName;
	}
	
	public DockerCommand getCommand() {
		return cmd;
	}
	
	public void execute(DockerClient client, String[] params) throws DockerException {
		cmd.execute(client, params);
	}
	
}
