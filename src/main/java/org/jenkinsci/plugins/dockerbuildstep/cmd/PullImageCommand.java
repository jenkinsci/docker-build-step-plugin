package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.List;

import javax.ws.rs.core.Response;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.DockerException;
import com.github.dockerjava.client.model.Image;
import com.sun.jersey.api.client.ClientResponse;

/**
 * This command pulls Docker image from a repository.
 * 
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#create-an-image
 * 
 * @author vjuranek
 * 
 */
public class PullImageCommand extends DockerCommand {

    private final String fromImage;
    private final String tag;
    private final String registry;

    @DataBoundConstructor
    public PullImageCommand(String fromImage, String tag, String registry) {
        this.fromImage = fromImage;
        this.tag = tag;
        this.registry = registry;
    }

    public String getFromImage() {
        return fromImage;
    }

    public String getTag() {
        return tag;
    }

    public String getRegistry() {
        return registry;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException, AbortException {
        // TODO check it when submitting the form
        if (fromImage == null || fromImage.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }

        String fromImageRes = Resolver.buildVar(build, fromImage);
        String tagRes = Resolver.buildVar(build, tag);
        String registryRes = Resolver.buildVar(build, registry);
        
        console.logInfo("Pulling image " + fromImageRes);
        DockerClient client = getClient();
        ClientResponse resp = client.execute(client.pullImageCmd(fromImageRes).withTag(tagRes).withRegistry(registryRes));
        if (Response.Status.Family.SUCCESSFUL.equals(resp.getStatusInfo().getFamily())) {
            console.logInfo("Downloading ... ");
        } else {
            console.logError("Something went wrong, response code is " + resp.getStatus() + ". Failing the build ...");
            throw new AbortException("Failed to pull Docker image name " + fromImageRes);
        }

        // wait for the image to be downloaded
        while (!isImagePulled()) {
            try {
                Thread.currentThread().sleep(15 * 1000); // wait 15 sec
            } catch (InterruptedException e) {
                // TODO log
                throw new AbortException("Download of Docker image name " + fromImageRes + " was interrupted");
            }
        }

        console.logInfo("Done");
    }

    private boolean isImagePulled() throws DockerException {
        DockerClient client = getClient();
        List<Image> images = client.execute(client.listImagesCmd().withFilter(fromImage));
        if (images.size() == 0) {
            return false;
        }
        // tag is not set, no need to compare repo tags
        if (tag == null || tag.isEmpty()) {
            return true;
        }

        String matchTag = fromImage + ":" + tag;
        for (Image img : images) {
            for (String repoTag : img.getRepoTags()) {
                if (matchTag.equals(repoTag)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Extension
    public static class PullImageCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Pull image";
        }
    }

}
