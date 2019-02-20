package sh.wheel.gitops.agent.okd;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.utils.ReplaceValueStream;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.Test;
import sh.wheel.gitops.agent.model.ProjectResources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProjectResourceLoaderIntegrationTest {

    @Test
    void getAll() {

        DefaultOpenShiftClient client = new DefaultOpenShiftClient();
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader();

        ProjectResources test2 = projectResourceLoader.getAll("test2", client);

        assertNotNull(test2);

    }

    @Test
    void loadAll() {
        DefaultOpenShiftClient client = new DefaultOpenShiftClient();
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader();

        Map<String, List<HasMetadata>> map = projectResourceLoader.loadAll("test2", client);

        assertNotNull(map);
    }

    @Test
    void templateCompare() {
        OpenShiftClient client = new DefaultOpenShiftClient();

        Map<String, String> params = new HashMap<>();
        params.put("REPLICA_COUNT", "2");
        params.put("IMAGE_NAME", "bitnami/nginx");
        params.put("IMAGE_VERSION", "1.14-ol-7");
        List<HasMetadata> items = client.templates().load(ReplaceValueStream.replaceValues(this.getClass().getResourceAsStream("/samples/testrepo1/apps/example-app/template/app.v1.yaml"), params)).processLocally(params).getItems();

        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader();

        projectResourceLoader.loadAll("test2", client);

    }
}