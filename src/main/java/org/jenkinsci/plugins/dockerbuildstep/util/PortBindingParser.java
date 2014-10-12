package org.jenkinsci.plugins.dockerbuildstep.util;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

/**
 * Parser for port mapping definitions that define how exposed container ports 
 * are mapped to host ports of the Docker server.
 * The result is represented as a {@link Ports} object exposing the individual 
 * bindings via a {@link Map}.
 */
public class PortBindingParser {

    /**
     * Parses a textual port binding definition to a {@link Ports} object that
     * contains the actual parsing results in a {@link Map} accessible via
     * {@link Ports#getBindings()}.
     * 
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
    public static Ports parse(String bindings) throws IllegalArgumentException {
        Ports ports = new Ports();
        if (isEmpty(bindings))
            return ports;

        String[] bindLines = bindings.split("\\r?\\n");
        for (String bind : bindLines) {
            Ports binding = parseOneBinding(bind);
            ports.getBindings().putAll(binding.getBindings());
        }
        return ports;
    }

    private static Ports parseOneBinding(String definition) throws IllegalArgumentException {
        Pattern pattern = Pattern.compile("((?<hIp>\\d+\\.\\d+\\.\\d+\\.\\d+):)?(?<hPort>\\d+)?[ :](?<cPort>\\d+)(/(?<protocol>tcp|udp))?");
        Matcher matcher = pattern.matcher(definition);
        if (matcher.matches()) {
            return new Ports(
                    createExposedPort(matcher.group("cPort"), matcher.group("protocol")), 
                    createBinding(matcher.group("hIp"), matcher.group("hPort")));
        } else {
            throw new IllegalArgumentException("Port binding needs to be in format '[hostIP:]hostPort containerPort[/scheme]'");
        }
    }
    
    private static Binding createBinding(String hIp, String hPort) {
        if (hPort == null) hPort = "0";
        return hIp == null 
                ? new Binding(Integer.parseInt(hPort))
                : new Binding(hIp, Integer.parseInt(hPort));
    }
    
    private static ExposedPort createExposedPort(String cPort, String protocol) {
        return protocol == null 
                ? ExposedPort.tcp(Integer.parseInt(cPort))
                : new ExposedPort(Integer.parseInt(cPort), InternetProtocol.parse(protocol));
    }
    
}
