package sh.wheel.gitops.agent.api;


import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sh.wheel.gitops.agent.service.AgentService;
import sh.wheel.gitops.agent.service.StateService;

import java.io.IOException;
import java.lang.invoke.MethodHandles;


@RestController
public class GitHookRestController {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private AgentService agentService;
    private StateService stateService;

    @Autowired
    public GitHookRestController(AgentService agentService, StateService stateService) {
        this.agentService = agentService;
        this.stateService = stateService;
    }

    @RequestMapping("/hook")
    public String doGet() {
        return "Hello from Spring Boot";
    }

    @RequestMapping("/sync")
    public String sync() {
        this.callAgentService();
        return "Synchronization started...";
    }

    @RequestMapping("/reload")
    public String reload() {
        this.callStateService();
        return "Reload started...";
    }

    @Async
    void callAgentService() {
        agentService.synchronize();
    }

    @Async
    void callStateService() {
        try {
            stateService.init();
        } catch (Exception e) {
            LOG.warn("Error occured while calling StateService", e);
        }
    }
}
