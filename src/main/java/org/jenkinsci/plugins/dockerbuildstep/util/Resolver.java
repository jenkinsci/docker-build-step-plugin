package org.jenkinsci.plugins.dockerbuildstep.util;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.util.VariableResolver;

/**
 * Convenient class for resolving/expanding various variabales.
 * 
 * @author vjuranek
 * 
 */
public class Resolver {

    public static String buildVar(final AbstractBuild<?, ?> build,final String toResolve) {
        VariableResolver<String> vr = build.getBuildVariableResolver();
        String resolved = Util.replaceMacro(toResolve, vr);
        try {
            resolved = build.getEnvironment().expand(resolved);  //TODO avoid deprecated method
        } catch (Exception e) {
            //TODO no-op?
        }
        return resolved;
    }
}
