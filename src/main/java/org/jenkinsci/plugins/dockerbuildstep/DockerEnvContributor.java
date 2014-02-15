package org.jenkinsci.plugins.dockerbuildstep;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;

import java.io.IOException;
import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;

/**
 * This contributor adds various Docker relate variable like container IDs or IP addresses into build environment
 * variables.
 * 
 * @author vjuranek
 * 
 */
@Extension
public class DockerEnvContributor extends EnvironmentContributor {

    public final String ID_SEPARATOR = ",";

    @Override
    public void buildEnvironmentFor(@SuppressWarnings("rawtypes") Run r, EnvVars envs, TaskListener listener)
            throws IOException, InterruptedException {

        List<EnvInvisibleAction> envActions = r.getActions(EnvInvisibleAction.class);
        if (envActions.size() == 0) {
            return;
        }

        String containerIds = envs.get("DOCKER_CONTAINER_IDS", "");
        if (!containerIds.equals("")) {
            containerIds.concat(ID_SEPARATOR);
        }

        for (EnvInvisibleAction action : envActions) {
            containerIds = containerIds.concat(action.getId()).concat(ID_SEPARATOR);
            envs.put("DOCKER_IP_" + action.getHostName(), action.getIpAddress());
        }
        containerIds = containerIds.substring(0, containerIds.length() - 1);
        envs.put("DOCKER_CONTAINER_IDS", containerIds);

    }
}
