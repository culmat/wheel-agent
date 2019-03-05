package sh.wheel.gitops.agent.testutil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import sh.wheel.gitops.agent.util.OpenShiftCli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.doReturn;

public class OpenShiftCliMockUtil {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<JsonNode> EXAMPLE_TEST_APP_RESOURCES_SERVER_RESPONSE = ProjectStateUtil.createExampleTestAppResourcesServerResponse();
    private static final JsonNode EXAMPLE_TEST_APP_PROJECT_SERVER_RESPONSE = ProjectStateUtil.createExampleTestAppProjectServerResponse();
    private static final JsonNode EXAMPLE_TEST_APP_RESOURCES_PROCESSED = ProjectStateUtil.createExampleTestAppResourcesProcessed();
    private static final JsonNode EXAMPLE_TEST_APP_PROJECT_PROCESSED = ProjectStateUtil.createExampleTestAppProjectProcessed();
    private static final List<JsonNode> EXAMPLE_MANAGABLE_PROJECTS = ProjectStateUtil.createExampleManagableProjects();
    private static final List<String> EXAMPLE_API_RESOURCES_WIDE = ProjectStateUtil.createExampleApiResourcesWide();

    public static OpenShiftCli createOpenShiftCliMock() {
        OpenShiftCli mock = Mockito.mock(OpenShiftCli.class);
        JsonNode exampleTestAppResourcesProcessed = deepCopy(EXAMPLE_TEST_APP_RESOURCES_PROCESSED);
        JsonNode exampleTestAppProjectProcessed = deepCopy(EXAMPLE_TEST_APP_PROJECT_PROCESSED);
        JsonNode exampleTestAppProjectServerResponse = deepCopy(EXAMPLE_TEST_APP_PROJECT_SERVER_RESPONSE);
        List<JsonNode> exampleTestAppResourcesServerResponse = EXAMPLE_TEST_APP_RESOURCES_SERVER_RESPONSE.stream().map(OpenShiftCliMockUtil::deepCopy).collect(Collectors.toList());
        List<JsonNode> manageableProjects = EXAMPLE_MANAGABLE_PROJECTS.stream().map(OpenShiftCliMockUtil::deepCopy).collect(Collectors.toList());

        doReturn(exampleTestAppResourcesProcessed).when(mock).process(ArgumentMatchers.endsWith("app.v1.yaml"), ArgumentMatchers.notNull());
        doReturn(exampleTestAppProjectProcessed).when(mock).process(ArgumentMatchers.endsWith("project.json"), ArgumentMatchers.notNull());
        doReturn(exampleTestAppResourcesServerResponse).when(mock).getResources(ArgumentMatchers.any(), ArgumentMatchers.any());
        doReturn(exampleTestAppProjectServerResponse).when(mock).getResource(ArgumentMatchers.eq("project"), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());
        doReturn("system:serviceaccount:example-app-test:default").when(mock).getWhoAmI();
        doReturn(manageableProjects).when(mock).getManageableProjects();
        doReturn(EXAMPLE_API_RESOURCES_WIDE).when(mock).getApiResourcesWide(ArgumentMatchers.eq(true));
        return mock;
    }

    private static JsonNode deepCopy(JsonNode jsonNode) {
        try {
            TokenBuffer tb = new TokenBuffer(OBJECT_MAPPER, false);
            OBJECT_MAPPER.writeValue(tb, jsonNode);
            return OBJECT_MAPPER.readTree(tb.asParser());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
