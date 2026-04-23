package org.jenkinsci.plugins.dockerbuildstep.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import hudson.model.StreamBuildListener;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.junit.Test;

import com.github.dockerjava.api.exception.DockerException;

/**
 * Class that tests CommandUtils
 *
 * @author wzheng2310@gmail.com (Wei Zheng)
 */
public class CommandUtilsTest {

    @Test
    public void testImageFullNameFrom() {
        assertEquals("img", CommandUtils.imageFullNameFrom("", "img", ""));
        assertEquals("img:tag", CommandUtils.imageFullNameFrom("", "img", "tag"));
        assertEquals("repo/img", CommandUtils.imageFullNameFrom("", "repo/img", ""));
        assertEquals("repo/img:tag", CommandUtils.imageFullNameFrom("", "repo/img", "tag"));
        assertEquals("reg/repo/img", CommandUtils.imageFullNameFrom("reg", "repo/img", ""));
        assertEquals("reg/repo/img:tag", CommandUtils.imageFullNameFrom("reg", "repo/img", "tag"));
    }

    @Test
    public void testAddLatestTagIfNeeded() {
        String[] input = {
                "img", "img:1", "img:123", "img:latest",
                "repo/img", "repo/img:1", "repo/img:123", "repo/img:latest",
                "reg/repo/img", "reg/repo/img:1", "reg/repo/img:123", "reg/repo/img:latest",
                "reg:80/repo/img", "reg:80/repo/img:1", "reg:80/repo/img:123", "reg:80/repo/img:latest"
            };
        String[] output = {
                "img:latest", "img:1", "img:123", "img:latest",
                "repo/img:latest", "repo/img:1", "repo/img:123", "repo/img:latest",
                "reg/repo/img:latest", "reg/repo/img:1", "reg/repo/img:123", "reg/repo/img:latest",
                "reg:80/repo/img:latest", "reg:80/repo/img:1", "reg:80/repo/img:123", "reg:80/repo/img:latest"
            };

        assertEquals("input length and output length differ!", input.length, output.length);
        for (int i = 0; i < input.length; i++ ) {
            assertEquals(output[i], CommandUtils.addLatestTagIfNeeded(input[i]));
        }
    }
    
    @Test
    public void testSizeInBytes() {
    	String[] input = {
    			"64", "128b", "256k", "512m", "1g", "666a", "-9mb"
    	};
    	long[] output = {
    			64, 128, 262144, 536870912, 1073741824, -1, -1
    	};
        assertEquals("input length and output length differ!", input.length, output.length);
        for (int i = 0; i < input.length; i++ ) {
            assertEquals(output[i], CommandUtils.sizeInBytes(input[i]));
        }
    }

    // --- logCommandResult tests ---

    private ConsoleLogger createConsoleLogger() {
        return new ConsoleLogger(new StreamBuildListener(new ByteArrayOutputStream()));
    }

    private ByteArrayInputStream toInputStream(String... lines) {
        return new ByteArrayInputStream(String.join("\n", lines).getBytes(Charset.defaultCharset()));
    }

    @Test
    public void logCommandResult_normalStatus_noException() {
        String input = "{\"status\":\"Pulling from library/ubuntu\",\"id\":\"latest\"}";
        CommandUtils.logCommandResult(toInputStream(input), createConsoleLogger(), "pull failed");
        // no exception expected
    }

    @Test
    public void logCommandResult_multipleNormalLines_noException() {
        CommandUtils.logCommandResult(
            toInputStream(
                "{\"status\":\"Pulling from library/ubuntu\"}",
                "{\"status\":\"Pulling fs layer\"}",
                "{\"status\":\"Download complete\"}"
            ),
            createConsoleLogger(),
            "pull failed"
        );
    }

    @Test(expected = DockerException.class)
    public void logCommandResult_errorKey_throwsDockerException() {
        String input = "{\"error\":\"manifest unknown\"}";
        CommandUtils.logCommandResult(toInputStream(input), createConsoleLogger(), "pull failed");
    }

    @Test(expected = DockerException.class)
    public void logCommandResult_errorDetailKey_throwsDockerException() {
        String input = "{\"errorDetail\":{\"message\":\"not found\"}}";
        CommandUtils.logCommandResult(toInputStream(input), createConsoleLogger(), "pull failed");
    }

    @Test
    public void logCommandResult_nonJsonLine_noException() {
        CommandUtils.logCommandResult(
            toInputStream("Some plain text that is not JSON"),
            createConsoleLogger(),
            "pull failed"
        );
    }

    @Test
    public void logCommandResult_mixedJsonAndNonJson_noException() {
        CommandUtils.logCommandResult(
            toInputStream(
                "Header line",
                "{\"status\":\"Pulling from library/ubuntu\"}",
                "Progress: 100%"
            ),
            createConsoleLogger(),
            "pull failed"
        );
    }

    @Test
    public void logCommandResult_emptyStream_noException() {
        CommandUtils.logCommandResult(
            toInputStream(""),
            createConsoleLogger(),
            "pull failed"
        );
    }

    @Test
    public void logCommandResult_errorAfterNormalLines_throwsDockerException() {
        try {
            CommandUtils.logCommandResult(
                toInputStream(
                    "{\"status\":\"Pulling from library/ubuntu\"}",
                    "{\"status\":\"Pulling fs layer\"}",
                    "{\"error\":\"unauthorized\"}"
                ),
                createConsoleLogger(),
                "pull failed"
            );
            fail("Should have thrown DockerException");
        } catch (DockerException e) {
            assertTrue(e.getMessage().contains("unauthorized"));
        }
    }

    @Test(expected = DockerException.class)
    public void logCommandResult_ioException_throwsDockerException() throws Exception {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws java.io.IOException {
                throw new java.io.IOException("simulated IO failure");
            }
        };
        CommandUtils.logCommandResult(broken, createConsoleLogger(), "fallback error");
    }
}
