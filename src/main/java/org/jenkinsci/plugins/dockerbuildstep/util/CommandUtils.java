package org.jenkinsci.plugins.dockerbuildstep.util;

import com.github.dockerjava.api.DockerException;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Util class to for docker commands.
 *
 * @auther wzheng2310@gmail.com (Wei Zheng)
 */
public class CommandUtils {
    public static String imageFullNameFrom(String registry, String repoAndImg, String tag) {
        if (StringUtils.isNotBlank(registry) || StringUtils.isNotBlank(tag)) {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotBlank(registry)) {
                sb.append(registry).append("/").append(repoAndImg);
            } else {
                sb.append(repoAndImg);
            }
            if (StringUtils.isNotBlank(tag)) {
                sb.append(":").append(tag);
            }
            return sb.toString();
        } else {
            return repoAndImg;
        }
    }

    public static void logCommandResult(InputStream inputStream,
            ConsoleLogger console, String errMessage) {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        try {
          while((line = in.readLine()) != null) {
            console.logInfo(line);
            JSONObject jsonResponse = JSONObject.fromObject(line);
            if (jsonResponse.containsKey("error") || jsonResponse.containsKey("errorDetail")) {
              throw new DockerException(line, 200);
            }
          }
        } catch (IOException e) {
          throw new DockerException(line == null ? errMessage : line, 200, e);
        }
    }

    public static String addLatestTagIfNeeded(String fullImageName) {
        // Assuming that the fullImageName is a valid name, the pattern is
        // enough to decide if it contains tag or not.
        if (fullImageName.matches(".+:[^:/]+$")) {
            return fullImageName;
        }
        return fullImageName + ":latest";
    }
}
