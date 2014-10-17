package org.jenkinsci.plugins.dockerbuildstep.util;

import static org.apache.commons.lang.StringUtils.isEmpty;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;

/**
 * Parser for bind mount definitions.
 * A bind mount is expressed as a {@link Bind} that represents a host path 
 * being bind mounted as a {@link Volume} in a Docker container. 
 * The Bind can be in read only or read write {@link AccessMode}.
 */
public class BindParser {

    /**
     * Parses a textual bind mount definition to an array of {@link Bind}s.
     * The input may describe multiple bind mounts, each on a line of its own.
     * The syntax for a bind mount definition is
     * <code>hostPath:containerPath[:rw|ro]</code>.
     * The elements may alternatively be delimited by a blank character.
     *  
     * @param definition the bind mount definition
     * @return an array containing the parsing results. 
     *         Empty if the definition was an empty string or <code>null</code>
     * @throws IllegalArgumentException if any error occurs during parsing
     */
    public static Bind[] parse(String definition) throws IllegalArgumentException {
        if (isEmpty(definition)) return new Bind[0];
        
        String[] lines = definition.split("\\r?\\n");
        
        Bind[] binds = new Bind[lines.length];
        for (int i = 0; i < lines.length; i++) {
            binds[i] = parseOneBind(lines[i]);
        }
        return binds;
    }

    private static Bind parseOneBind(String definition) throws IllegalArgumentException {
        try {
            // first try as-is (using ":" as delimiter) in order to 
            // preserve whitespace in paths
            return Bind.parse(definition);
        } catch (IllegalArgumentException e1) {
            try {
                // give it a second try assuming blanks as delimiter
                return Bind.parse(definition.replace(' ', ':'));
            } catch (Exception e2) {
                throw new IllegalArgumentException(
                        "Bind mount needs to be in format 'hostPath containerPath[ rw|ro]'");
            }
        }
    }

}
