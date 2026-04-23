package com.c2.server;

import com.c2.socket.ImprovedServer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ServerManager {

    private static final List<String> ALLOWED_COMMANDS = List.of(
            "PING", "INFO", "TIME", "HELP", "WHOAMI", "HOSTNAME", "PWD", "DATE", "LS", "DIR"
    );
    private static final Set<String> NO_ARGUMENT_COMMANDS = Set.copyOf(ALLOWED_COMMANDS);

    private ServerManager() {
    }

    public static Map<String, String> getClients() {
        Map<String, String> clients = new TreeMap<>();

        ImprovedServer.getClients().forEach((key, client) ->
                clients.put(key, client.isConnected() ? "Online" : "Offline"));

        return clients;
    }

    public static String sendCommand(String agent, String cmd) {
        String command = normalizeCommand(cmd);
        if (!NO_ARGUMENT_COMMANDS.contains(command)) {
            return "Unsupported command. Try: " + String.join(", ", ALLOWED_COMMANDS);
        }

        ImprovedServer.ClientHandler client = ImprovedServer.getClients().get(agent);
        if (client == null || !client.isConnected()) {
            return "Agent not found";
        }

        return client.sendAndReceive(agent, command);
    }

    public static List<String> getAllowedCommands() {
        return ALLOWED_COMMANDS;
    }

    private static String normalizeCommand(String cmd) {
        if (cmd == null) {
            return "";
        }
        return cmd.trim().toUpperCase().replaceAll("\\s+", " ");
    }
}
