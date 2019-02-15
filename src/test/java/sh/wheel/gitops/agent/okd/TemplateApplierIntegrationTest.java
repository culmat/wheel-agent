package sh.wheel.gitops.agent.okd;

import com.openshift.internal.restclient.model.KubernetesResource;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.ITemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.wheel.gitops.agent.config.ParameterConfig;
import sh.wheel.gitops.agent.testutil.ClientHelper;
import sh.wheel.gitops.agent.testutil.Samples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateApplierIntegrationTest {

    private IClient client;

    @BeforeEach
    void beforeAll() {
        client = ClientHelper.createClient();
    }

    @Test
    void apply() {
        String template = Samples.TEMPLATE1.getContentAsString();
        String namespace = "test1";
        List<ParameterConfig> parameters = new ArrayList<>();
        parameters.add(new ParameterConfig("REPLICA_COUNT", "2"));
        parameters.add(new ParameterConfig("IMAGE_NAME", "bitnami/nginx"));
        parameters.add(new ParameterConfig("IMAGE_VERSION", "1.14-ol-7"));
        TemplateProcessor templateProcessor = new TemplateProcessor();
        ITemplate processedTemplate = templateProcessor.process(template, parameters, namespace, client);
        // clean up
        for (IResource object : processedTemplate.getObjects()) {
            ((KubernetesResource) object).setNamespace(namespace);
            client.delete(object);
        }
        TemplateApplier applier = new TemplateApplier();

        Collection<IResource> apply = applier.apply(processedTemplate, namespace, client);

        assertEquals(3, apply.size());
        assertEquals(1, apply.stream().filter(r -> ResourceKind.SERVICE.equals(r.getKind())).count());
        assertEquals(1, apply.stream().filter(r -> ResourceKind.DEPLOYMENT_CONFIG.equals(r.getKind())).count());
        assertEquals(1, apply.stream().filter(r -> ResourceKind.ROUTE.equals(r.getKind())).count());
    }

}