package org.jenkinsci.plugins.dockerbuildstep.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

/**
 *  Defines legal syntax for entering port bindings.
 */
public class PortBindingParserTest {

    private static final ExposedPort TCP_8080 = ExposedPort.tcp(8080);

    @Test
    public void fullDefinition_blank() {
        assertCreatesBindings("127.0.0.1:80 8080/tcp",
                new PortBinding(Ports.Binding("127.0.0.1", 80), TCP_8080));
    }
    
    @Test
    public void noProtocol_blank() {
        assertCreatesBindings("127.0.0.1:80 8080",
                new PortBinding(Ports.Binding("127.0.0.1", 80), TCP_8080));
    }
    
    @Test
    public void noHostIp_blank() {
        assertCreatesBindings("80 8080/tcp",
                new PortBinding(Ports.Binding(80), TCP_8080));
    }
    
    @Test
    public void portsOnly_blank() {
        assertCreatesBindings("80 8080",
                new PortBinding(Ports.Binding(80), TCP_8080));
    }
    
    @Test
    public void fullDefinition_colon() {
        assertCreatesBindings("127.0.0.1:80:8080/tcp",
                new PortBinding(Ports.Binding("127.0.0.1", 80), TCP_8080));
    }
    
    @Test
    public void noProtocol_colon() {
        assertCreatesBindings("127.0.0.1:80:8080",
                new PortBinding(Ports.Binding("127.0.0.1", 80), TCP_8080));
    }
    
    @Test
    public void noHostIp_colon() {
        assertCreatesBindings("80:8080/tcp",
                new PortBinding(Ports.Binding(80), TCP_8080));
    }
    
    @Test
    public void portsOnly_colon() {
        assertCreatesBindings("80:8080",
                new PortBinding(Ports.Binding(80), TCP_8080));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void syntaxError() {
        PortBindingParser.parse("nonsense");
    }
    
    @Test
    public void parseEmptyString() {
        PortBinding[] bindings = PortBindingParser.parse("");
        assertEquals(0, bindings.length);
    }
    
    @Test
    public void exposedUdpPort() {
        assertCreatesBindings("80 8080/udp",
                new PortBinding(Ports.Binding(80), ExposedPort.udp(8080)));
    }
    
    @Test
    public void dynamicHostPort_blank() {
        assertCreatesBindings("127.0.0.1: 8080",
                new PortBinding(Ports.Binding("127.0.0.1", null), TCP_8080));
    }
    
    @Test
    public void dynamicHostPort_colon() {
        assertCreatesBindings("127.0.0.1::8080",
                new PortBinding(Ports.Binding("127.0.0.1", null), TCP_8080));
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
        assertCreatesBindings(input,
                new PortBinding(Ports.Binding(80), ExposedPort.tcp(8080)), 
                new PortBinding(Ports.Binding(81), ExposedPort.tcp(8081))); 
    }
    
    private static void assertCreatesBindings(String input, PortBinding... expected) {
        PortBinding[] parsed = PortBindingParser.parse(input);
        assertEquals("wrong number of PortBindings created", expected.length, parsed.length);
        
        for (int i = 0; i < parsed.length; i++) {
            assertEquals(expected[i], parsed[i]);
        }
    }
    
}
