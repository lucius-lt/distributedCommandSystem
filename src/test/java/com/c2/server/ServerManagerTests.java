package com.c2.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.c2.config.C2WebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerManagerTests {

    private ServerManager serverManager;

    @BeforeEach
    void setUp() {
        serverManager = new ServerManager(new C2WebSocketHandler());
    }

    @Test
    void rejectsUnsupportedCommandsBeforeLookingUpAgent() {
        String response = serverManager.sendCommand("Agent 1", "whoami && calc");

        assertThat(response).contains("Unsupported command");
    }

    @Test
    void reportsMissingAgentForAllowedCommand() {
        String response = serverManager.sendCommand("Agent 1", "whoami");

        assertThat(response).isEqualTo("Agent not found");
    }

    @Test
    void exposesAllowedTypedCommands() {
        assertThat(serverManager.getAllowedCommands())
                .containsExactly("PING", "INFO", "TIME", "HELP", "WHOAMI", "HOSTNAME", "PWD", "DATE", "LS", "DIR");
    }
}
