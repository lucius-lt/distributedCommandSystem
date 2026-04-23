package com.c2;

import com.c2.socket.ImprovedServer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class C2Application {

    private final boolean agentServerEnabled;

    public C2Application(@Value("${agent.server.enabled:true}") boolean agentServerEnabled) {
        this.agentServerEnabled = agentServerEnabled;
    }

    public static void main(String[] args) {
        SpringApplication.run(C2Application.class, args);
    }

    @PostConstruct
    public void startServer() {
        if (!agentServerEnabled) {
            return;
        }

        Thread serverThread = new Thread(ImprovedServer::startServer, "agent-diagnostic-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }
}
