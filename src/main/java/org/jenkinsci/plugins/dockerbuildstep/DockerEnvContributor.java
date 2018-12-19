package org.jenkinsci.plugins.dockerbuildstep;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import com.google.common.base.Joiner;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;

import java.io.IOException;
import java.util.*;

/**
 * This contributor adds various Docker relate variable like container IDs or IP addresses into build environment
 * variables.
 *
 * @author vjuranek
 *
 */
@Extension
public class DockerEnvContributor extends EnvironmentContributor {

	public final static String ID_SEPARATOR = ",";
	public final static String CONTAINER_IDS_ENV_VAR = "DOCKER_CONTAINER_IDS";
	public final static String CONTAINER_IP_PREFIX = "DOCKER_IP_";
	public final static String PORT_BINDINGS_ENV_VAR = "DOCKER_HOST_BIND_PORTS";
	public final static String PORT_BINDING_PREFIX = "DOCKER_HOST_PORT_";
	public final static String HOST_SOCKET_PREFIX = "DOCKER_HOST_SOCKET_";

	@Override
	public void buildEnvironmentFor(@SuppressWarnings("rawtypes") Run r, EnvVars envs, TaskListener listener)
			throws IOException, InterruptedException {

		List<EnvInvisibleAction> envActions = r.getActions(EnvInvisibleAction.class);
		if (envActions.size() == 0) {
			return;
		}

		Set<String> containerIds = new LinkedHashSet<String>();
		containerIds.addAll(Arrays.asList(envs.get(CONTAINER_IDS_ENV_VAR, "").split(ID_SEPARATOR)));

		for (EnvInvisibleAction action : envActions) {
			containerIds.add(action.getId());

			envs.put(CONTAINER_IP_PREFIX + action.getHostName(), action.getIpAddress());
			if (action.hasPortBindings()) {
				exportPortBindings(envs, action.getPortBindings());
			}
		}

		containerIds.remove(null);
		containerIds.remove("");

		envs.put(CONTAINER_IDS_ENV_VAR, Joiner.on(ID_SEPARATOR).join(containerIds));
	}

	private void exportPortBindings(EnvVars envs, Map<ExposedPort, Binding[]> bindings) {
		StringBuilder ports = new StringBuilder();
		for (Map.Entry<ExposedPort, Binding[]> entry : bindings.entrySet()) {
			ExposedPort exposedPort = entry.getKey();
			ports.append(exposedPort.toString()).append(ID_SEPARATOR);
			Binding[] exposedPortBinding = entry.getValue();
			if (exposedPortBinding == null) {
				continue;
			}
			envs.put(PORT_BINDING_PREFIX + exposedPort.getProtocol().name() + "_" + exposedPort.getPort(),
					exposedPortBinding[0].getHostPortSpec());

			StringBuilder portBinding = new StringBuilder();
			String hostIp = exposedPortBinding[0].getHostIp();
			if (hostIp != null && hostIp.length() > 0) {
				portBinding.append(hostIp).append(":");
				portBinding.append(exposedPortBinding[0].getHostPortSpec());
				envs.put(HOST_SOCKET_PREFIX + exposedPort.getProtocol().name() + "_" + exposedPort.getPort(),
						portBinding.toString());
			}
		}
		String bindPorts = ports.substring(0, ports.length() - 1).toString();
		envs.put(PORT_BINDINGS_ENV_VAR, bindPorts);
	}

}
