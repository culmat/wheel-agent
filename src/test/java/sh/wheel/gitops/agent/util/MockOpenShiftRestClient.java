package sh.wheel.gitops.agent.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Collectors;

public class MockOpenShiftRestClient extends OpenShiftRestClient {


    public static final ObjectMapper OM = new ObjectMapper();
    private final List<RecordedRequestResponse> mockData;

    public MockOpenShiftRestClient(List<RecordedRequestResponse> mockData, String apiServerUrl, RestTemplate restTemplate) {
        super(apiServerUrl, restTemplate);
        this.mockData = mockData;
    }

    public static MockOpenShiftRestClient createMockClient(Path mockDataDir) {
        try {
            List<RecordedRequestResponse> mockData = Files.walk(mockDataDir).filter(p -> p.toString().toLowerCase().endsWith(".json"))
                    .map(p -> {
                        try {
                            return OM.readValue(p.toFile(), RecordedRequestResponse.class);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }).collect(Collectors.toList());
            return new MockOpenShiftRestClient(mockData, null, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public JsonNode play(String type, String endpoint, Object requestObject) {
        try {
            JsonNode request = requestObject != null ? OM.readTree(requestObject.toString()) : null;
            return mockData.stream().filter(r -> r.matchesRequest(endpoint, type, request)).findFirst()
                    .orElseThrow(() -> new UnsupportedOperationException("No mocked data found for endpoint '" + endpoint + "', type '" + type + "' and '" + requestObject + "'"))
                    .getResponseObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    @Override
    JsonNode patch(String endpoint, Object patchObject) {
        return play("patch", endpoint, patchObject);
    }

    @Override
    JsonNode delete(String url) {
        return play("delete", url, null);
    }

    @Override
    JsonNode post(String endpoint, Object postRequest) {
        return play("post", endpoint, postRequest);
    }

    @Override
    JsonNode get(String url) {
        return play("get", url, null);
    }
}
