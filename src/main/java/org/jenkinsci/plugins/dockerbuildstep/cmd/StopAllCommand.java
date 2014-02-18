package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.Container;

/**
 * This command stop all Docker containers.
 * 
 * @author vjuranek
 * 
 */
public class StopAllCommand extends DockerCommand {

    @DataBoundConstructor
    public StopAllCommand() {
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        
        List<Container> containers = getClient().listContainers(false);
        for (Container c : containers) {
            getClient().stopContainer(c.getId());
            console.logInfo("stopped container id " + c.getId());
        }
    }

    @Extension
    public static class StopAllCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Stop all constainers";
        }
    }

}