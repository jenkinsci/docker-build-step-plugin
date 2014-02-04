package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;

public class RestartCommand implements DockerCommand {
	
	@Override
	public void execute(DockerClient client, String[] params) throws DockerException {
		if(params == null || params.length < 1) { 
			throw new IllegalArgumentException("At least one parameter is required");
		}
		
		Integer timeout = new Integer(5);
		if(params.length > 1) {
			try {
				timeout = new Integer(params[1]);
			} catch(NumberFormatException e) {
				// TODO log exception 
				// fall back to default
			}
		}
		
		client.restart(params[0], timeout);
	}

}