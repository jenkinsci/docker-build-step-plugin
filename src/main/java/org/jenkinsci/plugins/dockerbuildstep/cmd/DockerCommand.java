package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

public interface DockerCommand {

	public void execute(DockerClient client, String[] params) throws DockerException ;
	
}
