/*package com.c2;

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
*/
/*package com.c2;

import com.c2.socket.ImprovedServer;

public class C2Application {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        ImprovedServer server = new ImprovedServer(port);
        server.start();

        System.out.println("Server running on port: " + port);
    }
}
*/


package com.c2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class C2Application {
    public static void main(String[] args) {
        SpringApplication.run(C2Application.class, args);
    }
}
