package org.jenkinsci.plugins.dockerbuildstep.util;

import com.github.dockerjava.client.model.ExposedPort;
import com.github.dockerjava.client.model.Ports;
import com.github.dockerjava.client.model.Ports.Binding;

/**
 * Parser for port mapping definitions that define how to map docker ports to host ports
 */
public class PortBindingParser {

    /**
     * Assumes one port binding per line in format
     * <ul> 
     *  <li>dockerPort hostPort</li>
     *  <li>dockerPort/scheme hostPort</li>
     *  <li>dockerPort hostIP:hostPort</li>
     *  <li>dockerPort/scheme hostIP:hostPort</li>
     * </ul>
     */
    public static Ports parseBindings(String bindings) {
        if (bindings == null || bindings.isEmpty())
            return null;

        Ports ports = new Ports();
        String[] bindLines = bindings.split("\\r?\\n");
        for (String bind : bindLines) {
            Ports binding = parseOneBinding(bind);
            ports.getBindings().putAll(binding.getBindings());
        }
        return ports;
    }

    public static Ports parseOneBinding(String binding) throws IllegalArgumentException {
        String[] bindSplit = binding.trim().split(" ", 2);
        if(bindSplit.length != 2)
            throw new IllegalArgumentException("Port binding needs to be in format 'hostPort containerPort'");
        ExposedPort ep = bindSplit[0].contains("/") ? ExposedPort.parse(bindSplit[0].trim()) : ExposedPort.tcp(new Integer(bindSplit[0].trim()));
        String[] hostBind = bindSplit[1].trim().split(":", 2);
        Binding b = hostBind.length > 1 ? new Binding(hostBind[0], new Integer(hostBind[1])) : new Binding(new Integer(hostBind[0]));
        return new Ports(ep, b);
    }

}
