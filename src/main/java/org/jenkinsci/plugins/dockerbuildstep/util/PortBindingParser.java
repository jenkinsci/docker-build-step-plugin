package org.jenkinsci.plugins.dockerbuildstep.util;

import static org.apache.commons.lang.StringUtils.isEmpty;

import com.github.dockerjava.api.model.PortBinding;

/**
 * Parser for port mapping definitions that define how exposed container ports 
 * are mapped to host ports of the Docker server.
 */
public class PortBindingParser {

    /**
     * Parses a textual port binding definition to an array of {@link PortBinding}s.
     * 
     * Assumes one port binding per line in format
     * <ul> 
     *  <li>hostPort containerPort</li>
     *  <li>hostPort containerPort/protocol</li>
     *  <li>hostIP:hostPort containerPort</li>
     *  <li>hostIP:hostPort containerPort/protocol</li>
     * </ul>
     * where host and container part can alternatively be delimited by a colon.
     * 
     * @throws IllegalArgumentException if any error occurs during parsing
     */
    public static PortBinding[] parse(String definition) throws IllegalArgumentException {
        if (isEmpty(definition))
            return new PortBinding[0];
        
        String[] lines = definition.split("\\r?\\n");
        PortBinding[] result = new PortBinding[lines.length];
        
        for (int i = 0; i < lines.length; i++) {
            result[i] = parseOnePortBinding(lines[i]);
        }
        
        return result;
    }

    private static PortBinding parseOnePortBinding(String definition) throws IllegalArgumentException {
        try {
            return PortBinding.parse(definition.replace(' ', ':'));
        } catch (Exception e) {
            throw new IllegalArgumentException("Port binding needs to be in format '[hostIP:]hostPort containerPort[/protocol]'");
        }
    }

}
