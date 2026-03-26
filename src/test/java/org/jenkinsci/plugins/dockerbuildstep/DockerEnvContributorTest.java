/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.dockerbuildstep;

import hudson.EnvVars;
import hudson.model.FreeStyleBuild;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class DockerEnvContributorTest {

    private JenkinsRule j;

    private final DockerEnvContributor contributor = new DockerEnvContributor();

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void doNotTouchExistingContainerIds() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = existingEnvVars("existing,ids");

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals("existing,ids", envVars.get(contributor.CONTAINER_IDS_ENV_VAR));
    }

    @Test
    void addNewContainerIds() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = new EnvVars();
        build.addAction(contributedEnvVars("new,ids"));

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals("new,ids", envVars.get(contributor.CONTAINER_IDS_ENV_VAR));
    }

    @Test
    void mergeContainerIds() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = existingEnvVars("original");
        build.addAction(contributedEnvVars("new"));

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals("original,new", envVars.get(contributor.CONTAINER_IDS_ENV_VAR));
    }

    @Test
    void collapseSameContainerIds() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = existingEnvVars("existing,duplicate");

        build.addAction(contributedEnvVars("duplicate"));
        build.addAction(contributedEnvVars("new"));

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals("existing,duplicate,new", envVars.get(contributor.CONTAINER_IDS_ENV_VAR));
    }

    private EnvVars existingEnvVars(String ids) {
        return new EnvVars(contributor.CONTAINER_IDS_ENV_VAR, ids);
    }

    private EnvInvisibleAction contributedEnvVars(final String id) {
        return new EnvInvisibleAction() {
            @Override public String getId() {
                return id;
            }

            @Override public String getHostName() {
                return "";
            }

            @Override public String getIpAddress() {
                return "";
            }

            @Override public boolean hasPortBindings() {
                return false;
            }
        };
    }
}
