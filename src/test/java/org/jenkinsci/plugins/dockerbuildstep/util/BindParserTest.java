package org.jenkinsci.plugins.dockerbuildstep.util;

import static com.github.dockerjava.api.model.AccessMode.ro;
import static com.github.dockerjava.api.model.AccessMode.rw;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;

/**
 *  Defines legal syntax for entering bind mount definitions ({@link Bind}s).
 */
public class BindParserTest {

    @Test
    public void defaultAccessMode_blank() {
        assertCreatesBinds(input("/host /container"), 
                expected("/host", "/container", AccessMode.DEFAULT));
    }

    @Test
    public void readWrite_blank() {
        assertCreatesBinds(input("/host /container rw"), expected("/host", "/container", rw));
    }

    @Test
    public void readOnly_blank() {
        assertCreatesBinds(input("/host /container ro"), expected("/host", "/container", ro));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidAccessMode_blank() {
        BindParser.parse("/host /container xx");
    }

    @Test
    public void defaultAccessMode_colon() {
        assertCreatesBinds(input("/host:/container"), 
                expected("/host", "/container", AccessMode.DEFAULT));
    }

    @Test
    public void readWrite_colon() {
        assertCreatesBinds(input("/host:/container:rw"), expected("/host", "/container", rw));
    }

    @Test
    public void readOnly_colon() {
        assertCreatesBinds(input("/host:/container:ro"), expected("/host", "/container", ro));
    }

    @Test
    public void pathWithBlanks_colon() {
        // not sure if this would work in Docker and why anybody should want to do this,
        // but let's allow this as well.
        assertCreatesBinds(input("/host with blanks:/container with blanks:ro"), 
                expected("/host with blanks", "/container with blanks", ro));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidAccessMode_colon() {
        BindParser.parse("/host:/container:xx");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseInvalidInput() {
        BindParser.parse("nonsense");
    }
    
    @Test
    public void parseEmptyString() {
        assertEquals(0, BindParser.parse("").length);
    }
    
    @Test
    public void twoBindings_UnixStyle() {
    	assertCreatesBinds(input("/host1:/container1:rw\n/host2:/container2:ro"), 
    			expected("/host1", "/container1", rw),
    			expected("/host2", "/container2", ro));
    }
    
    @Test
    public void twoBindings_DosStyle() {
    	assertCreatesBinds(input("/host1:/container1:rw\r\n/host2:/container2:ro"), 
    			expected("/host1", "/container1", rw),
    			expected("/host2", "/container2", ro));
    }
    
    private void assertCreatesBinds(String input, Expected... expected) {
        Bind[] parsed = BindParser.parse(input);
        assertEquals("wrong number of Binds created", expected.length, parsed.length);
        for (int i = 0; i < parsed.length; i++) {
	        assertEquals("Bind #" + i, expected[i].hostPath, parsed[i].getPath());
	        assertEquals("Bind #" + i, expected[i].containerPath, parsed[i].getVolume().getPath());
	        assertEquals("Bind #" + i, expected[i].accessMode, parsed[i].getAccessMode());
		}
    }
    
    /**
     * Designates the argument as input for parsing.<br>
     * For increased readability.
     * 
     * @param input the input string for the parser
     * @return the input string, but semantically enhanced
     */
    private static String input(String input) {
        return input;
    }

    /**
     * Designates the arguments as the expected result of parsing.<br>
     * For increased readability.
     * 
     * @param hostPath the expected host path of the parsing result
     * @param containerPath the expected container path of the parsing result
     * @param accessMode the access mode of the parsing result
     * @return a container object for the expected values
     */
    private static Expected expected(String hostPath, String containerPath, AccessMode accessMode) {
        return new Expected(hostPath, containerPath, accessMode);
    }

    /** Parameter object for expected parsing results */
    private static class Expected {
        public final String hostPath;
        public final String containerPath;
        public final AccessMode accessMode;

        public Expected(String hostPath, String containerPath, AccessMode accessMode) {
            this.hostPath = hostPath;
            this.containerPath = containerPath;
            this.accessMode = accessMode;
        }
    }

}
