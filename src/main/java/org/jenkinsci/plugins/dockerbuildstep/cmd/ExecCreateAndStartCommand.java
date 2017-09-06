package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Arrays;
import java.util.List;

public class ExecCreateAndStartCommand extends DockerCommand {

    private final String containerIds;
    private final String command;
    // TODO advanced config - IO streams

    @DataBoundConstructor
    public ExecCreateAndStartCommand(String containerIds, String command) {
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
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, final ConsoleLogger console)
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
        DockerClient client = getClient(build, null);
        for (String id : ids) {
            id = id.trim();
            ExecCreateCmdResponse res = client.execCreateCmd(id).withCmd(commandRes.split(" ")).
                    withAttachStderr(true).withAttachStdout(true).exec();
            console.logInfo(String.format("Exec command with ID '%s' created in container '%s' ", res.getId(), id));
            console.logInfo(String.format("Executing command with ID '%s'", res.getId()));
            ExecStartResultCallback callback = new ExecStartResultCallback() {
                public void onNext(Frame item) {
                    console.logInfo(item.toString());
                    super.onNext(item);
                }

                public void onError(Throwable throwable) {
                    console.logError("Failed to exec start:" + throwable.getMessage());
                    super.onError(throwable);
                }
            };
            try {
                client.execStartCmd(res.getId()).exec(callback).awaitCompletion();
            } catch (InterruptedException e) {
                console.logError("Failed to exec start:" + e.getMessage());
            }
        }

    }

    @Extension
    public static class ExecCreateAndStartCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Create and start exec instance in container(s)";
        }
    }

}
