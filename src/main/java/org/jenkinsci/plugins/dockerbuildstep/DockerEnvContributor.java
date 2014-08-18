package org.jenkinsci.plugins.dockerbuildstep;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;

import com.github.dockerjava.client.model.ExposedPort;
import com.github.dockerjava.client.model.Ports.Binding;

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
    public final String CONTAINER_IDS_ENV_VAR = "DOCKER_CONTAINER_IDS";
    public final String CONTAINER_IP_PREFIX = "DOCKER_IP_";
    public final String PORT_BINDINGS_ENV_VAR = "DOCKER_HOST_BIND_PORTS";
    public final String PORT_BINDING_PREFIX = "DOCKER_HOST_PORT_";

    @Override
    public void buildEnvironmentFor(@SuppressWarnings("rawtypes") Run r, EnvVars envs, TaskListener listener)
            throws IOException, InterruptedException {

        List<EnvInvisibleAction> envActions = r.getActions(EnvInvisibleAction.class);
        if (envActions.size() == 0) {
            return;
        }

        String containerIds = envs.get(CONTAINER_IDS_ENV_VAR, "");
        if (!containerIds.equals("")) {
            containerIds.concat(ID_SEPARATOR);
        }

        for (EnvInvisibleAction action : envActions) {
            containerIds = containerIds.concat(action.getId()).concat(ID_SEPARATOR);
            envs.put(CONTAINER_IP_PREFIX + action.getHostName(), action.getIpAddress());
            if (action.hasPortBindings()) {
                exportPortBindings(envs, action.getPortBindings());
            }
        }

        containerIds = containerIds.substring(0, containerIds.length() - 1);
        envs.put(CONTAINER_IDS_ENV_VAR, containerIds);

    }

    private void exportPortBindings(EnvVars envs, Map<ExposedPort, Binding> bindings) {
        StringBuilder ports = new StringBuilder();
        for (ExposedPort hostPort : bindings.keySet()) {
            ports.append(hostPort.toString()).append(ID_SEPARATOR);
            envs.put(PORT_BINDING_PREFIX + hostPort.getScheme().toUpperCase() + "_" + hostPort.getPort(),
                    Integer.toString(bindings.get(hostPort).getHostPort()));
        }
        String bindPorts = ports.substring(0, ports.length() - 1).toString();
        envs.put(PORT_BINDINGS_ENV_VAR, bindPorts);
    }

}
