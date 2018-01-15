package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.exception.DockerException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.ExecCreateRemoteCallable;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.ExecStartRemoteCallable;
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
        
        Config cfgData = getConfig(build);
        Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
        for (String id : ids) {
            id = id.trim();
            
            try {
                String commandId = launcher.getChannel().call(new ExecCreateRemoteCallable(cfgData, descriptor, id, commandRes.split(" "), true));
                console.logInfo(String.format("Exec command with ID '%s' created in container '%s' ", commandId, id));
                console.logInfo(String.format("Executing command with ID '%s'", commandId));
                
                launcher.getChannel().call(new ExecStartRemoteCallable(console, cfgData, descriptor, commandId));
            } catch (Exception e) {
                console.logError("failed to exec create and start command '" + commandRes + "' in containers " + ids);
                e.printStackTrace();
                throw new IllegalArgumentException(e);
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
