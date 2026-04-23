package com.c2.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServerManagerTests {

    @Test
    void rejectsUnsupportedCommandsBeforeLookingUpAgent() {
        String response = ServerManager.sendCommand("Agent 1", "whoami && calc");

        assertThat(response).contains("Unsupported command");
    }

    @Test
    void reportsMissingAgentForAllowedCommand() {
        String response = ServerManager.sendCommand("Agent 1", "whoami");

        assertThat(response).isEqualTo("Agent not found");
    }

    @Test
    void exposesAllowedTypedCommands() {
        assertThat(ServerManager.getAllowedCommands())
                .containsExactly("PING", "INFO", "TIME", "HELP", "WHOAMI", "HOSTNAME", "PWD", "DATE", "LS", "DIR");
    }
}
