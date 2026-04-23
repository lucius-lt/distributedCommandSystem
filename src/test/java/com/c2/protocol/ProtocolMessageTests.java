package com.c2.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

class ProtocolMessageTests {

    @Test
    void serializesAndParsesCommandMessages() {
        ProtocolMessage original = ProtocolMessage.command("request-1", "Agent 1", "PING");

        ProtocolMessage parsed = ProtocolMessage.fromJson(original.toJson());

        assertThat(parsed.type()).isEqualTo("command");
        assertThat(parsed.id()).isEqualTo("request-1");
        assertThat(parsed.target()).isEqualTo("Agent 1");
        assertThat(parsed.command()).isEqualTo("PING");
    }

    @Test
    void serializesAndParsesResponseMessages() {
        ProtocolMessage original = ProtocolMessage.success("request-1", "Agent 1", "PONG");

        ProtocolMessage parsed = ProtocolMessage.fromJson(original.toJson());

        assertThat(parsed.type()).isEqualTo("response");
        assertThat(parsed.id()).isEqualTo("request-1");
        assertThat(parsed.status()).isEqualTo("success");
        assertThat(parsed.data()).isEqualTo("PONG");
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> ProtocolMessage.fromJson("not-json"))
                .isInstanceOf(JSONException.class);
    }
}
