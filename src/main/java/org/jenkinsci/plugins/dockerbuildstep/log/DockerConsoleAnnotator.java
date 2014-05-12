package org.jenkinsci.plugins.dockerbuildstep.log;

import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Console annotator which annotates Docker messages using {@link DockerConsoleNote}. Annotated message has to start
 * with <i>[Docker]</i> prefix.
 * 
 * @see {@link http://javadoc.jenkins-ci.org/hudson/console/LineTransformationOutputStream.html LineTransformationOutputStream} 
 * 
 * @author vjuranek
 * 
 */
public class DockerConsoleAnnotator extends LineTransformationOutputStream {

    private final OutputStream out;

    public DockerConsoleAnnotator(OutputStream out) {
        this.out = out;
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {
        String line = Charset.defaultCharset().decode(ByteBuffer.wrap(b, 0, len)).toString();
        if (line.startsWith("[Docker]"))
            new DockerConsoleNote().encodeTo(out);
        out.write(b, 0, len);
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }

}
