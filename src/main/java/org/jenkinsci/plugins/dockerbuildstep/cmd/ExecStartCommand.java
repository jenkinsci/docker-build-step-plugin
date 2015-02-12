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

public class ExecStartCommand extends DockerCommand {

	private final String commandIds;

	@DataBoundConstructor
	public ExecStartCommand(String commandIds) {
		this.commandIds = commandIds;
	}

	public String getCommandIds() {
		return commandIds;
	}

	@Override
	public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
			throws DockerException {

		if (commandIds == null || commandIds.isEmpty()) {
			console.logError("Command ID cannot be empty");
			throw new IllegalArgumentException("Command ID cannot be empty");
		}

		String commandIdsRes = Resolver.buildVar(build, commandIds);
		List<String> cmdIds = Arrays.asList(commandIdsRes.split(","));
		DockerClient client = getClient(null);

		// TODO execute async on containers
		for (String cmdId : cmdIds) {
			client.execStartCmd(cmdId).exec();
			console.logInfo(String.format("Executing command with ID '%s'", cmdId));
			// TODO show output?
		}

	}

	@Extension
	public static class ExecStartCommandDescriptor extends DockerCommandDescriptor {
		@Override
		public String getDisplayName() {
			return "Start exec instance in container(s)";
		}
	}

}
