package com.c2.socket;

import com.c2.protocol.ProtocolMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

public final class ImprovedServer {

    private static final int PORT = 1234;
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(5);
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration CLIENT_STALE_AFTER = Duration.ofSeconds(10);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_MISSED_HEARTBEATS = 3;
    private static final Logger LOGGER = Logger.getLogger(ImprovedServer.class.getName());
    private static final ConcurrentHashMap<String, ClientHandler> CLIENTS = new ConcurrentHashMap<>();
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);
    private static final AtomicBoolean HEARTBEAT_STARTED = new AtomicBoolean(false);
    private static final ExecutorService CLIENT_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("agent-handler-" + thread.getId());
        return thread;
    });
    private static final ScheduledExecutorService HEARTBEAT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "agent-heartbeat-monitor");
        thread.setDaemon(true);
        return thread;
    });

    private ImprovedServer() {
    }

    public static void startServer() {
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT));
            startHeartbeatMonitor();
            LOGGER.info(() -> "Diagnostic server started on 127.0.0.1:" + PORT);

            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = server.accept();
                String id = "Agent " + ID_COUNTER.getAndIncrement();
                ClientHandler handler = new ClientHandler(socket, id);

                CLIENTS.put(id, handler);
                CLIENT_EXECUTOR.execute(handler);

                LOGGER.info(() -> id + " connected");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Diagnostic server stopped unexpectedly", e);
        }
    }

    public static Map<String, ClientHandler> getClients() {
        return CLIENTS;
    }

    private static void startHeartbeatMonitor() {
        if (!HEARTBEAT_STARTED.compareAndSet(false, true)) {
            return;
        }

        HEARTBEAT_EXECUTOR.scheduleWithFixedDelay(
                ImprovedServer::removeStaleClients,
                HEARTBEAT_INTERVAL.toSeconds(),
                HEARTBEAT_INTERVAL.toSeconds(),
                TimeUnit.SECONDS
        );
    }

    private static void removeStaleClients() {
        long now = System.currentTimeMillis();

        CLIENTS.forEach((id, client) -> {
            if (!client.isConnected()) {
                LOGGER.info(() -> "Removing disconnected client: " + id);
                removeClient(id, client);
                return;
            }

            if (now - client.getLastSeen() > CLIENT_STALE_AFTER.toMillis()
                    && client.getMissedHeartbeats() >= MAX_MISSED_HEARTBEATS) {
                LOGGER.info(() -> "Client timeout: " + id);
                removeClient(id, client);
                return;
            }

            if (!client.heartbeat(HEARTBEAT_TIMEOUT)) {
                int missedHeartbeats = client.incrementMissedHeartbeats();
                LOGGER.info(() -> "Missed heartbeat " + missedHeartbeats + "/" + MAX_MISSED_HEARTBEATS + ": " + id);

                if (missedHeartbeats >= MAX_MISSED_HEARTBEATS) {
                    LOGGER.info(() -> "Removing dead client: " + id);
                    removeClient(id, client);
                }
            } else {
                client.resetMissedHeartbeats();
            }
        });
    }

    private static void removeClient(String id, ClientHandler client) {
        if (CLIENTS.remove(id, client)) {
            client.close();
        }
    }

    public static final class ClientHandler implements Runnable {

        private final Socket socket;
        private final String id;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private final Object writeLock = new Object();
        private final ConcurrentHashMap<String, CompletableFuture<ProtocolMessage>> pendingResponses = new ConcurrentHashMap<>();
        private final AtomicInteger missedHeartbeats = new AtomicInteger(0);
        private volatile long lastSeen = System.currentTimeMillis();

        public ClientHandler(Socket socket, String id) throws IOException {
            this.socket = socket;
            this.id = id;
            this.socket.setSoTimeout(5000);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        public String sendCommand(String command, Duration timeout) {
            return sendAndReceive(id, command, timeout);
        }

        public String sendAndReceive(String agentId, String command) {
            return sendAndReceive(agentId, command, COMMAND_TIMEOUT);
        }

        public String sendAndReceive(String agentId, String command, Duration timeout) {
            try {
                String requestId = UUID.randomUUID().toString();
                ProtocolMessage request = ProtocolMessage.command(requestId, agentId, command);
                ProtocolMessage response = sendRequest(request, timeout);

                if (response == null) {
                    return "No response";
                }

                return response.isSuccess() ? response.data() : "Error: " + response.data();
            } catch (Exception e) {
                CLIENTS.remove(agentId, this);
                close();
                return "Client disconnected";
            }
        }

        public boolean heartbeat(Duration timeout) {
            ProtocolMessage response = sendRequest(ProtocolMessage.heartbeat(UUID.randomUUID().toString(), id), timeout);
            return response != null && response.isSuccess() && "ALIVE".equals(response.data());
        }

        public long getLastSeen() {
            return lastSeen;
        }

        public int getMissedHeartbeats() {
            return missedHeartbeats.get();
        }

        public int incrementMissedHeartbeats() {
            return missedHeartbeats.incrementAndGet();
        }

        public void resetMissedHeartbeats() {
            missedHeartbeats.set(0);
        }

        public boolean isConnected() {
            return socket.isConnected() && !socket.isClosed();
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted() && isConnected()) {
                    String rawJson;

                    try {
                        rawJson = reader.readLine();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }

                    if (rawJson == null) {
                        break;
                    }

                    handleIncoming(rawJson);
                }
            } catch (IOException e) {
                LOGGER.info(() -> id + " disconnected");
            } finally {
                CLIENTS.remove(id, this);
                completePendingWithDisconnect();
                close();
            }
        }

        public void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        private ProtocolMessage sendRequest(ProtocolMessage request, Duration timeout) {
            CompletableFuture<ProtocolMessage> future = new CompletableFuture<>();
            pendingResponses.put(request.id(), future);

            try {
                send(request);
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                pendingResponses.remove(request.id());
                return ProtocolMessage.error(request.id(), request.target(), "Timeout waiting for response");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pendingResponses.remove(request.id());
                return ProtocolMessage.error(request.id(), request.target(), "Interrupted while waiting for response");
            } catch (ExecutionException e) {
                pendingResponses.remove(request.id());
                CLIENTS.remove(id, this);
                return null;
            } catch (RuntimeException e) {
                pendingResponses.remove(request.id());
                CLIENTS.remove(id, this);
                close();
                return null;
            }
        }

        private void send(ProtocolMessage message) {
            synchronized (writeLock) {
                String outgoingJson = message.toJson();
                LOGGER.info(() -> "Outgoing JSON to " + id + ": " + outgoingJson);
                writer.println(outgoingJson);
            }
        }

        private void handleIncoming(String rawJson) {
            LOGGER.info(() -> "Incoming JSON from " + id + ": " + rawJson);

            ProtocolMessage message;

            try {
                message = ProtocolMessage.fromJson(rawJson);
            } catch (JSONException e) {
                LOGGER.warning(() -> "Invalid JSON from " + id + ": " + rawJson);
                return;
            }

            lastSeen = System.currentTimeMillis();
            resetMissedHeartbeats();

            if (ProtocolMessage.TYPE_RESPONSE.equals(message.type())) {
                completeResponse(message);
                return;
            }

            if (ProtocolMessage.TYPE_HEARTBEAT.equals(message.type())) {
                LOGGER.fine(() -> "Heartbeat received from " + id);
                return;
            }

            LOGGER.warning(() -> "Unsupported message type from " + id + ": " + message.type());
        }

        private void completeResponse(ProtocolMessage message) {
            CompletableFuture<ProtocolMessage> future = pendingResponses.remove(message.id());

            if (future == null) {
                LOGGER.warning(() -> "Unexpected response id from " + id + ": " + message.id());
                return;
            }

            future.complete(message);
        }

        private void completePendingWithDisconnect() {
            pendingResponses.forEach((requestId, future) ->
                    future.completeExceptionally(new IOException("Client disconnected")));
            pendingResponses.clear();
        }
    }
}
