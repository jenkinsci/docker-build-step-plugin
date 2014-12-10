package org.jenkinsci.plugins.dockerbuildstep;

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

import jersey.repackaged.com.google.common.base.Preconditions;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import hudson.Extension;

import java.io.Serializable;

import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;

import hudson.model.AbstractDescribableImpl;

/**
 * Credential configuration which allows docker commands to authenticate with registries.
 *
 * @author wzheng2310@gmail.com (Wei Zheng)
 */
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
            int idxColonSlashSlash = AuthConfig.DEFAULT_SERVER_ADDRESS.indexOf("://");
            if (idxColonSlashSlash == -1) {
                return AuthConfig.DEFAULT_SERVER_ADDRESS;
            }
            return AuthConfig.DEFAULT_SERVER_ADDRESS.substring(
                    idxColonSlashSlash + 3);
        }
    }
}
