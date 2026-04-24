/*package com.c2.server;

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
                clients.put(key, client.isOpen() ? "Online" : "Offline"));

        return clients;
    }

    public static String sendCommand(String agent, String cmd) {
        String command = normalizeCommand(cmd);

        if (!NO_ARGUMENT_COMMANDS.contains(command)) {
            return "Unsupported command";
        }

        org.java_websocket.WebSocket client = ImprovedServer.getClients().get(agent);

        if (client == null || !client.isOpen()) {
            return "Agent not found";
        }

        ImprovedServer.sendCommand(agent, command);

        return "Command sent";
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
}*/

package com.c2.server;

import com.c2.config.C2WebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class ServerManager {

    private final C2WebSocketHandler handler;

    public ServerManager(C2WebSocketHandler handler) {
        this.handler = handler;
    }

    private static final List<String> ALLOWED_COMMANDS = List.of(
            "PING", "INFO", "TIME", "HELP", "WHOAMI", "HOSTNAME", "PWD", "DATE", "LS", "DIR"
    );

    private static final Set<String> ALLOWED_COMMAND_SET = Set.copyOf(ALLOWED_COMMANDS);

    public Map<String, String> getClients() {
        Map<String, String> clients = new TreeMap<>();

        handler.getClients().forEach((key, session) ->
                clients.put(key, session.isOpen() ? "Online" : "Offline"));

        return clients;
    }

    public String sendCommand(String agent, String cmd) {
        if (cmd == null || cmd.isBlank()) {
            return "Empty command";
        }
        
        String[] parts = cmd.trim().split("\\s+");
        String baseCommand = parts[0].toUpperCase();

        if (!ALLOWED_COMMAND_SET.contains(baseCommand)) {
            return "Unsupported command: " + baseCommand;
        }

        WebSocketSession session = handler.getClients().get(agent);

        if (session == null || !session.isOpen()) {
            return "Agent not found";
        }

        return handler.sendCommand(agent, cmd.trim());
    }

    public List<String> getAllowedCommands() {
        return ALLOWED_COMMANDS;
    }
}