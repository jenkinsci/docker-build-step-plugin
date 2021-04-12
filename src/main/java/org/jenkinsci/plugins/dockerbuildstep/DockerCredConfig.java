package org.jenkinsci.plugins.dockerbuildstep;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.github.dockerjava.api.model.AuthConfig;

import org.jenkinsci.plugins.dockerbuildstep.cmd.DockerCommand;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import hudson.Extension;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

import hudson.model.AbstractDescribableImpl;

/**
 * Credential configuration which allows docker commands to authenticate with registries.
 *
 * @author wzheng2310@gmail.com (Wei Zheng)
 */
@Deprecated
@ExportedBean
public class DockerCredConfig extends AbstractDescribableImpl<DockerCredConfig> implements Serializable {

    private final String credentialsId;
    private final String serverAddress;

    @DataBoundConstructor
    public DockerCredConfig(@Nullable String credentialsId, @Nullable String serverAddress) {
        Preconditions.checkArgument(
                Strings.isNullOrEmpty(credentialsId) || !Strings.isNullOrEmpty(serverAddress));
        this.credentialsId = credentialsId;
        this.serverAddress = serverAddress;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getServerHost() {
        try {
            URI uri = new URI(serverAddress);
            if (uri.getScheme() == null) {
                throw new IllegalArgumentException(
                    "Registry Server Addresses should contains URI scheme");
            } else {
                return uri.getHost();
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                "Invalid Registry Server Addresses: " + e.getMessage());
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DockerCredConfig> {

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
                                                     @QueryParameter String serverAddress) {
            if (project == null || !project.hasPermission(Item.CONFIGURE)) {
                return new StandardListBoxModel();
            }
            return new StandardListBoxModel()
                .withEmptySelection()
                .withMatching(
                    DockerCommand.CREDENTIALS_MATCHER,
                    CredentialsProvider.lookupCredentials(StandardCredentials.class,
                        project,
                        ACL.SYSTEM,
                        URIRequirementBuilder.fromUri(serverAddress).build()));
        }

        @Override
        public String getDisplayName() {
          return "";
        }

        public String getDefaultServerAddress() {
            return AuthConfig.DEFAULT_SERVER_ADDRESS;
        }
    }
}
