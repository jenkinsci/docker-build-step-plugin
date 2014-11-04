package org.jenkinsci.plugins.dockerbuildstep.util;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

/**
 *  Defines legal syntax for entering port bindings.
 */
public class PortBindingParserTest {
    
    @Test
    public void completeDefinition_blank() {
        assertCreatesBinding("127.0.0.1:80 8080/tcp", Ports.Binding("127.0.0.1", 80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void noProtocol_blank() {
        assertCreatesBinding("127.0.0.1:80 8080", Ports.Binding("127.0.0.1", 80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void noHost_blank() {
        assertCreatesBinding("80 8080/tcp", Ports.Binding(80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void minimalDefiniton_blank() {
        assertCreatesBinding("80 8080", Ports.Binding(80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void completeDefinition_colon() {
        assertCreatesBinding("127.0.0.1:80:8080/tcp", Ports.Binding("127.0.0.1", 80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void noProtocol_colon() {
        assertCreatesBinding("127.0.0.1:80:8080", Ports.Binding("127.0.0.1", 80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void noHost_colon() {
        assertCreatesBinding("80:8080/tcp", Ports.Binding(80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void minimalDefiniton_colon() {
        assertCreatesBinding("80:8080", Ports.Binding(80), ExposedPort.tcp(8080));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void syntaxError() {
        PortBindingParser.parse("nonsense");
    }
    
    @Test
    public void parseEmptyString() {
        Map<ExposedPort, Binding> bindings = PortBindingParser.parse("").getBindings();
        assertTrue("no binding", bindings.isEmpty());
    }
    
    @Test
    public void exposedUdpPort() {
        assertCreatesBinding("80 8080/udp", Ports.Binding(80), ExposedPort.udp(8080));
    }
    
    @Test
    public void dynamicPort() {
        assertCreatesBinding("127.0.0.1: 8080", Ports.Binding("127.0.0.1", 0), ExposedPort.tcp(8080));
    }
    
    @Test
    public void dynamicPort_colon() {
        assertCreatesBinding("127.0.0.1::8080", Ports.Binding("127.0.0.1", 0), ExposedPort.tcp(8080));
    }
    
    @Test
    public void twoBindings_UnixStyle() {
        twoBindings("80 8080\n81 8081");
    }
    
    @Test
    public void twoBindings_DosStyle() {
        twoBindings("80 8080\r\n81 8081");
    }
    
    private void twoBindings(String input) {
        Ports ports = PortBindingParser.parse(input);
        assertEquals(2, ports.getBindings().size());
        assertContainsBinding(ports, ExposedPort.tcp(8080), Ports.Binding(80));
        assertContainsBinding(ports, ExposedPort.tcp(8081), Ports.Binding(81));
    }
    
    private static void assertCreatesBinding(String input, Binding binding, ExposedPort exposedPort) {
        Ports ports = PortBindingParser.parse(input);
        assertContainsBinding(ports, exposedPort, binding);
    }

    private static void assertContainsBinding(Ports ports, ExposedPort exposedPort, Binding expectedBinding) {
        Binding binding = ports.getBindings().get(exposedPort);
        assertNotNull("no binding was created for " + exposedPort, binding);
        assertEquals(expectedBinding, binding);
    }

}
