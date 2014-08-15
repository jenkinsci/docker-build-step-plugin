package org.jenkinsci.plugins.dockerbuildstep.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     *  <li>hostPort containerPort</li>
     *  <li>hostPort containerPort/scheme</li>
     *  <li>hostIP:hostPort containerPort</li>
     *  <li>hostIP:hostPort containerPort/scheme</li>
     * </ul>
     * where host and container part can alternatively be delimited by a colon.
     * 
     * @throws IllegalArgumentException if any error occurs during parsing
     */
    public static Ports parseBindings(String bindings) throws IllegalArgumentException {
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

    public static Ports parseOneBinding(String definition) throws IllegalArgumentException {
        Pattern pattern = Pattern.compile("((?<hIp>[0-9.]+):)?(?<hPort>\\d+)[ :](?<cPort>\\d+)(/tcp)?");
        Matcher matcher = pattern.matcher(definition);
        try {
            if (! matcher.matches())
                throw new IllegalArgumentException();
            Binding b = matcher.group("hIp") == null 
                    ? new Binding(Integer.parseInt(matcher.group("hPort")))
                    : new Binding(matcher.group("hIp"), Integer.parseInt(matcher.group("hPort")));
            ExposedPort ep = ExposedPort.tcp(Integer.parseInt(matcher.group("cPort")));
            return new Ports(ep, b);
        } catch (Exception e) {
            throw new IllegalArgumentException("Port binding needs to be in format '[hostIP:]hostPort containerPort[/scheme]'");
        }
    }

}
