package org.jenkinsci.plugins.dockerbuildstep;

import static hudson.init.InitMilestone.PLUGINS_STARTED;
import hudson.Plugin;
import hudson.init.Initializer;
import hudson.model.Run;

public class PluginImpl extends Plugin {

    /**
     * In docker-java 0.10.0, packages got renamed.
     * These aliases are required to read builds persisted by Jenkins Build Step 
     * Plugin prior to 1.10.
     * 
     * @since 1.10
     */
    @Initializer(before=PLUGINS_STARTED)
    public static void addXStreamAliases() {
        Run.XSTREAM2.addCompatibilityAlias(
                "com.github.dockerjava.client.model.ExposedPort", 
                 com.github.dockerjava.api.model.ExposedPort.class);
        Run.XSTREAM2.addCompatibilityAlias(
                "com.github.dockerjava.client.model.Ports$Binding", 
                 com.github.dockerjava.api.model.Ports.Binding.class);
    }

}
