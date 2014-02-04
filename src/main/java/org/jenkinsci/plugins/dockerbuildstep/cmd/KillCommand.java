package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

public class KillCommand implements DockerCommand {
	
	@Override
	public void execute(DockerClient client, String[] params) throws DockerException {
		if(params == null || params.length < 1) { 
			throw new IllegalArgumentException("At least one parameter is required");
		}
		client.kill(params[0]);
	}

}