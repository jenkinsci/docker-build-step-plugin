package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;

public class ExecCreateCommand extends DockerCommand {

	private final String containerIds;
	private final String command;
	// TODO advanced config - IO streams

	@DataBoundConstructor
	public ExecCreateCommand(String containerIds, String command) {
		this.containerIds = containerIds;
		this.command = command;
	}

	public String getContainerIds() {
		return containerIds;
	}

	public String getCommand() {
		return command;
	}

	@Override
	public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
			throws DockerException {
		if (containerIds == null || containerIds.isEmpty()) {
			console.logError("Container ID cannot be empty");
			throw new IllegalArgumentException("Container ID cannot be empty");
		}
		if (command == null || command.isEmpty()) {
			console.logError("Command cannot be empty");
			throw new IllegalArgumentException("Command cannot be empty");
		}

		String containerIdsRes = Resolver.buildVar(build, containerIds);
		String commandRes = Resolver.buildVar(build, command);

		List<String> ids = Arrays.asList(containerIdsRes.split(","));
		DockerClient client = getClient();
		for (String id : ids) {
			id = id.trim();
			ExecCreateCmdResponse res = client.execCreateCmd(id).withCmd(commandRes.split(" ")).exec();
			console.logInfo(String.format("Exec command with ID '%s' created in container '%s' ", res.getId(), id));
			// TODO export env. variables with command IDs
		}

	}

	@Extension
	public static class ExecCreateCommandDescriptor extends DockerCommandDescriptor {
		@Override
		public String getDisplayName() {
			return "Create exec instance in container(s)";
		}
	}

}
