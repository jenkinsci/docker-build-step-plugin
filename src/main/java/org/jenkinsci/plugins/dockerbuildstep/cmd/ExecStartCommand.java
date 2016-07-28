package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import hudson.Extension;
import hudson.model.AbstractBuild;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Arrays;
import java.util.List;

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
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, final ConsoleLogger console)
            throws DockerException {

        if (commandIds == null || commandIds.isEmpty()) {
            console.logError("Command ID cannot be empty");
            throw new IllegalArgumentException("Command ID cannot be empty");
        }

        String commandIdsRes = Resolver.buildVar(build, commandIds);
        List<String> cmdIds = Arrays.asList(commandIdsRes.split(","));
        DockerClient client = getClient(build, null);

        // TODO execute async on containers
        for (String cmdId : cmdIds) {
            console.logInfo(String.format("Executing command with ID '%s'", cmdId));
            ExecStartResultCallback callback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame item) {
                    console.logInfo(item.toString());
                    super.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    console.logError("Failed to exec start:" + throwable.getMessage());
                    super.onError(throwable);
                }
            };
            try {
                client.execStartCmd(cmdId).exec(callback).awaitCompletion();
            } catch (InterruptedException e) {
                console.logError("Failed to exec start:" + e.getMessage());
            }
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
