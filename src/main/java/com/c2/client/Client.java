package com.c2.client;

import com.c2.protocol.ProtocolMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Collectors;
import org.json.JSONException;

public class Client {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 1234;
    private static final long RECONNECT_DELAY_MS = 3000;

    public static void main(String[] args) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                connectAndListen();
            } catch (Exception e) {
                System.out.println("Disconnected from server, reconnecting in 3 seconds");
                sleepBeforeReconnect();
            }
        }
    }

    private static void connectAndListen() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            System.out.println("[+] Connected to server");

            String rawJson;
            while ((rawJson = input.readLine()) != null) {
                handleMessage(rawJson, output);
            }
        }
    }

    private static void handleMessage(String rawJson, PrintWriter output) {
        ProtocolMessage message;

        try {
            message = ProtocolMessage.fromJson(rawJson);
        } catch (JSONException e) {
            System.out.println("Invalid JSON from server: " + rawJson);
            return;
        }

        if (ProtocolMessage.TYPE_HEARTBEAT.equals(message.type())) {
            output.println(ProtocolMessage.success(message.id(), message.target(), "ALIVE").toJson());
            return;
        }

        if (!ProtocolMessage.TYPE_COMMAND.equals(message.type())) {
            output.println(ProtocolMessage.error(message.id(), message.target(), "Unsupported message type").toJson());
            return;
        }

        try {
            output.println(ProtocolMessage.success(message.id(), message.target(), handleCommand(message.command())).toJson());
        } catch (IllegalArgumentException e) {
            output.println(ProtocolMessage.error(message.id(), message.target(), e.getMessage()).toJson());
        } catch (Exception e) {
            output.println(ProtocolMessage.error(message.id(), message.target(), "Command failed: " + e.getMessage()).toJson());
        }
    }

    private static void sleepBeforeReconnect() {
        try {
            Thread.sleep(RECONNECT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String handleCommand(String command) throws IOException {
        String normalizedCommand = command == null ? "" : command.trim().toUpperCase();

        return switch (normalizedCommand) {
            case "PING" -> "PONG";
            case "TIME", "DATE" -> Instant.now().toString();
            case "INFO" -> "OS: " + System.getProperty("os.name")
                    + ", Java: " + System.getProperty("java.version")
                    + ", User: " + System.getProperty("user.name");
            case "WHOAMI" -> System.getProperty("user.name");
            case "HOSTNAME" -> InetAddress.getLocalHost().getHostName();
            case "PWD" -> Path.of("").toAbsolutePath().normalize().toString();
            case "LS", "DIR" -> listCurrentDirectory();
            case "HELP" -> "Supported commands: PING, INFO, TIME, HELP, WHOAMI, HOSTNAME, PWD, DATE, LS, DIR";
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        };
    }

    private static String listCurrentDirectory() throws IOException {
        try (var entries = Files.list(Path.of("").toAbsolutePath().normalize())) {
            String output = entries
                    .map(path -> Files.isDirectory(path) ? path.getFileName() + "/" : path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.joining(System.lineSeparator()));

            return output.isBlank() ? "[empty directory]" : output;
        }
    }
}
