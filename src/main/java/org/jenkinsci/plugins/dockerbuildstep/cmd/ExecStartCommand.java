package org.jenkinsci.plugins.dockerbuildstep.cmd;

import com.github.dockerjava.api.exception.DockerException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder;
import org.jenkinsci.plugins.dockerbuildstep.DockerBuilder.Config;
import org.jenkinsci.plugins.dockerbuildstep.cmd.remote.ExecStartRemoteCallable;
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
    public void execute(Launcher launcher, @SuppressWarnings("rawtypes") AbstractBuild build, final ConsoleLogger console)
            throws DockerException {

        if (commandIds == null || commandIds.isEmpty()) {
            console.logError("Command ID cannot be empty");
            throw new IllegalArgumentException("Command ID cannot be empty");
        }

        String commandIdsRes = Resolver.buildVar(build, commandIds);
        List<String> cmdIds = Arrays.asList(commandIdsRes.split(","));

        // TODO execute async on containers
        for (String cmdId : cmdIds) {
            console.logInfo(String.format("Executing command with ID '%s'", cmdId));
            
            try {
                Config cfgData = getConfig(build);
                Descriptor<?> descriptor = Jenkins.getInstance().getDescriptor(DockerBuilder.class);
                
                launcher.getChannel().call(new ExecStartRemoteCallable(console, cfgData, descriptor, cmdId));
            } catch (Exception e) {
                console.logError("failed to execute cmd id " + cmdId);
                e.printStackTrace();
                throw new IllegalArgumentException(e);
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
