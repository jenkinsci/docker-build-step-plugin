package org.jenkinsci.plugins.dockerbuildstep.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

/**
 *  Defines legal syntax for entering port bindings
 */
public class PortBindingParserTest {
    
    @Test
    public void completeDefinition() throws Exception {
        assertCreatesBinding("127.0.0.1:80 8080/tcp", Ports.Binding("127.0.0.1", 80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void noScheme() throws Exception {
        assertCreatesBinding("127.0.0.1:80 8080", Ports.Binding("127.0.0.1", 80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void noHost() throws Exception {
        assertCreatesBinding("80 8080/tcp", Ports.Binding(80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void minimalDefiniton() throws Exception {
        assertCreatesBinding("80 8080", Ports.Binding(80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void completeDefinition_colon() throws Exception {
        assertCreatesBinding("127.0.0.1:80:8080/tcp", Ports.Binding("127.0.0.1", 80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void noScheme_colon() throws Exception {
        assertCreatesBinding("127.0.0.1:80:8080", Ports.Binding("127.0.0.1", 80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void noHost_colon() throws Exception {
        assertCreatesBinding("80:8080/tcp", Ports.Binding(80), ExposedPort.tcp(8080));
    }
    
    @Test
    public void minimalDefiniton_colon() throws Exception {
        assertCreatesBinding("80:8080", Ports.Binding(80), ExposedPort.tcp(8080));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void syntaxError() throws Exception {
        PortBindingParser.parseBindings("nonsense");
    }
    
    @Test
    public void exposedUdpPort() throws Exception {
        assertCreatesBinding("80 8080/udp", Ports.Binding(80), new ExposedPort("udp", 8080));
    }
    
    @Test
    public void dynamicPort() throws Exception {
        assertCreatesBinding("127.0.0.1: 8080", Ports.Binding("127.0.0.1", 0), ExposedPort.tcp(8080));
    }
    
    @Test
    public void dynamicPort_colon() throws Exception {
        assertCreatesBinding("127.0.0.1::8080", Ports.Binding("127.0.0.1", 0), ExposedPort.tcp(8080));
    }
    
    @Test
    public void twoBindingsUnixStyle() throws Exception {
        twoBindings("80 8080\n81 8081");
    }
    
    @Test
    public void twoBindingsDosStyle() throws Exception {
        twoBindings("80 8080\r\n81 8081");
    }
    
    private void twoBindings(String input) throws Exception {
        Ports ports = PortBindingParser.parseBindings(input);
        assertEquals(2, ports.getBindings().size());
        assertContainsBinding(ports, ExposedPort.tcp(8080), Ports.Binding(80));
        assertContainsBinding(ports, ExposedPort.tcp(8081), Ports.Binding(81));
    }
    
    private static void assertCreatesBinding(String input, Binding expectedBinding, ExposedPort exposedPort) {
        Ports ports = PortBindingParser.parseOneBinding(input);
        assertContainsBinding(ports, exposedPort, expectedBinding);
    }

    private static void assertContainsBinding(Ports ports, ExposedPort exposedPort, Binding expectedBinding) {
        Binding binding = ports.getBindings().get(exposedPort);
        assertNotNull("no binding was created for " + exposedPort, binding);
        assertEquals(expectedBinding, binding);
    }

}
