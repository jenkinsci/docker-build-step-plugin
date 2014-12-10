package org.jenkinsci.plugins.dockerbuildstep.util;

import static org.junit.Assert.*;

import org.junit.Test;

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
}
