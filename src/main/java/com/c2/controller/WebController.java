package com.c2.controller;

import com.c2.server.ServerManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
/*
@Controller
public class WebController {
    private final ServerManager serverManager;
    public static Map<String, WebSocketSession> getClients()
    public WebController(ServerManager serverManager) {
        this.serverManager = serverManager;
    }
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("clients", serverManager.getClients());
        model.addAttribute("allowedCommands", serverManager.getAllowedCommands());
        return "index";
    }

    @GetMapping("/{agent}/diagnostics")
    public String diagnosticsPage(@PathVariable String agent, Model model) {
        model.addAttribute("name", agent);
        model.addAttribute("allowedCommands", serverManager.getAllowedCommands());
        return "execute";
    }

    @PostMapping("/{agent}/diagnostics")
    public String runDiagnostic(@PathVariable String agent,
                                @RequestParam String command,
                                Model model) {
        String output = ServerManager.sendCommand(agent, command);

        model.addAttribute("cmdoutput", output);
        model.addAttribute("name", agent);
        model.addAttribute("allowedCommands", ServerManager.getAllowedCommands());

        return "execute";
    }
}*/

@Controller
public class WebController {

    private final ServerManager serverManager;

    public WebController(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("clients", serverManager.getClients());
        model.addAttribute("allowedCommands", serverManager.getAllowedCommands());
        return "index";
    }

    @GetMapping("/{agent}/diagnostics")
    public String diagnosticsPage(@PathVariable String agent, Model model) {
        model.addAttribute("name", agent);
        model.addAttribute("allowedCommands", serverManager.getAllowedCommands());
        return "execute";
    }

    @PostMapping("/{agent}/diagnostics")
    public String runDiagnostic(@PathVariable String agent,
                                @RequestParam String command,
                                Model model) {
        String output = serverManager.sendCommand(agent, command);

        model.addAttribute("cmdoutput", output);
        model.addAttribute("name", agent);
        model.addAttribute("allowedCommands", serverManager.getAllowedCommands());

        return "execute";
    }
}
