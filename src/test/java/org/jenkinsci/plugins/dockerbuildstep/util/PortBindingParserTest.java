package org.jenkinsci.plugins.dockerbuildstep.util;

import static org.junit.jupiter.api.Assertions.*;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

import org.junit.jupiter.api.Test;

/**
 *  Defines legal syntax for entering port bindings.
 */
class PortBindingParserTest {

    private static final ExposedPort TCP_8080 = ExposedPort.tcp(8080);

    @Test
    void fullDefinition_blank() {
        assertCreatesBindings("127.0.0.1:80 8080/tcp",
                new PortBinding(Ports.Binding.bindIpAndPort("127.0.0.1", 80), TCP_8080));
    }

    @Test
    void noProtocol_blank() {
        assertCreatesBindings("127.0.0.1:80 8080",
                new PortBinding(Ports.Binding.bindIpAndPort("127.0.0.1", 80), TCP_8080));
    }

    @Test
    void noHostIp_blank() {
        assertCreatesBindings("80 8080/tcp",
                new PortBinding(Ports.Binding.bindPort(80), TCP_8080));
    }

    @Test
    void portsOnly_blank() {
        assertCreatesBindings("80 8080",
                new PortBinding(Ports.Binding.bindPort(80), TCP_8080));
    }

    @Test
    void fullDefinition_colon() {
        assertCreatesBindings("127.0.0.1:80:8080/tcp",
                new PortBinding(Ports.Binding.bindIpAndPort("127.0.0.1", 80), TCP_8080));
    }

    @Test
    void noProtocol_colon() {
        assertCreatesBindings("127.0.0.1:80:8080",
                new PortBinding(Ports.Binding.bindIpAndPort("127.0.0.1", 80), TCP_8080));
    }

    @Test
    void noHostIp_colon() {
        assertCreatesBindings("80:8080/tcp",
                new PortBinding(Ports.Binding.bindPort(80), TCP_8080));
    }

    @Test
    void portsOnly_colon() {
        assertCreatesBindings("80:8080",
                new PortBinding(Ports.Binding.bindPort(80), TCP_8080));
    }

    @Test
    void syntaxError() {
        assertThrows(IllegalArgumentException.class, () ->
            PortBindingParser.parse("nonsense"));
    }

    @Test
    void parseEmptyString() {
        PortBinding[] bindings = PortBindingParser.parse("");
        assertEquals(0, bindings.length);
    }

    @Test
    void exposedUdpPort() {
        assertCreatesBindings("80 8080/udp",
                new PortBinding(Ports.Binding.bindPort(80), ExposedPort.udp(8080)));
    }

    @Test
    void dynamicHostPort_blank() {
        assertCreatesBindings("127.0.0.1: 8080",
                new PortBinding(Ports.Binding.bindIp("127.0.0.1"), TCP_8080));
    }

    @Test
    void dynamicHostPort_colon() {
        assertCreatesBindings("127.0.0.1::8080",
                new PortBinding(Ports.Binding.bindIp("127.0.0.1"), TCP_8080));
    }

    @Test
    void twoBindings_UnixStyle() {
        twoBindings("80 8080\n81 8081");
    }

    @Test
    void twoBindings_DosStyle() {
        twoBindings("80 8080\r\n81 8081");
    }
    
    private static void twoBindings(String input) {
        assertCreatesBindings(input,
                new PortBinding(Ports.Binding.bindPort(80), ExposedPort.tcp(8080)),
                new PortBinding(Ports.Binding.bindPort(81), ExposedPort.tcp(8081)));
    }
    
    private static void assertCreatesBindings(String input, PortBinding... expected) {
        PortBinding[] parsed = PortBindingParser.parse(input);
        assertEquals(expected.length, parsed.length, "wrong number of PortBindings created");
        
        for (int i = 0; i < parsed.length; i++) {
            assertEquals(expected[i], parsed[i]);
        }
    }
    
}
