package sh.wheel.gitops.agent.service;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.wheel.gitops.agent.testutil.GitTestUtil;
import sh.wheel.gitops.agent.testutil.Samples;
import sh.wheel.gitops.agent.util.OpenShiftCli;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class AgentServiceIntegrationTest {

    private static Path REPOSITORIES_BASE_PATH;
    private static Path TESTREPO1_PATH;
    private AgentService agentService;


    @BeforeAll
    static void initRepo() throws URISyntaxException, IOException, GitAPIException {
        TESTREPO1_PATH = Paths.get(WheelRepositoryServiceTest.class.getResource(Samples.TESTREPO1_PATH).toURI());
        TESTREPO1_PATH = Paths.get(WheelRepositoryServiceTest.class.getResource(Samples.TESTREPO1_PATH).toURI());
        GitTestUtil.initGitRepoIfNotExists(TESTREPO1_PATH);
        URI resourceBasePath = WheelRepositoryServiceTest.class.getResource("/").toURI();
        REPOSITORIES_BASE_PATH = Paths.get(resourceBasePath).resolve("wheel-test-repos");
        Files.createDirectories(REPOSITORIES_BASE_PATH);
    }


    @BeforeEach
    void setUp() throws IOException, GitAPIException {
        WheelRepositoryService wheelRepositoryService = new WheelRepositoryService();
        wheelRepositoryService.repositoryBasePath = REPOSITORIES_BASE_PATH;
        OpenShiftCli openShiftCli = new OpenShiftCli();
        OpenShiftService openShiftService = new OpenShiftService(openShiftCli);
        openShiftService.init();
        ConfigProcessingService configProcessingService = new ConfigProcessingService(openShiftService);
        ResourceDifferenceService resourceDifferenceService = new ResourceDifferenceService();
        ProjectDifferenceService projectDifferenceService = new ProjectDifferenceService();
        StateService stateService = new StateService(wheelRepositoryService, configProcessingService, openShiftService);
        stateService.repositoryUrl = TESTREPO1_PATH.toAbsolutePath().toString();
        stateService.repositoryBranch = "master";
        stateService.init();
        agentService = new AgentService(stateService, projectDifferenceService, resourceDifferenceService, openShiftService);

    }

    @Test
    void synchronize() {
        agentService.synchronize();
    }
}