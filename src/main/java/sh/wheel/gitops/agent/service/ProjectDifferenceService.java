package sh.wheel.gitops.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.DiffFlags;
import com.flipkart.zjsonpatch.JsonDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sh.wheel.gitops.agent.model.*;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectDifferenceService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        updateNode.fieldNames().forEachRemaining(fieldName -> {
            JsonNode jsonNode = mainNode.get(fieldName);
            // if field exists and is an embedded object
            if (jsonNode != null && jsonNode.isObject()) {
                merge(jsonNode, updateNode.get(fieldName));
            } else {
                if (mainNode instanceof ObjectNode) {
                    // Overwrite field
                    JsonNode value = updateNode.get(fieldName);
                    ((ObjectNode) mainNode).replace(fieldName, value);
                }
            }
        });
        return mainNode;
    }

    public List<ResourceDifference> evaluateDifference(ProjectState processed, ProjectState cluster) {
        List<ResourceDifference> resourceDifferences = new ArrayList<>();
        Map<ResourceKey, Resource> processedResourcesByKind = processed.getResourcesByKey();
        Map<ResourceKey, Resource> clusterResourcesByKind = cluster.getResourcesByKey();
        Map<ResourceKey, Resource> processedOnly = getLeftSideOnly(processedResourcesByKind, clusterResourcesByKind);
        Map<ResourceKey, Resource> clusterOnly = getLeftSideOnly(clusterResourcesByKind, processedResourcesByKind);
        Map<ResourceKey, Resource> leftAndRightClusterResources = getLeftWhichIsinRightToo(clusterResourcesByKind, processedResourcesByKind);
        Set<String> uids = getUids(leftAndRightClusterResources);
        Map<ResourceKey, Resource> filteredClusterOnly = filterResourcesInUids(uids, clusterOnly);
        Map<ResourceKey, Resource> leftAndRightProcessedResources = getLeftWhichIsinRightToo(processedResourcesByKind, clusterResourcesByKind);
        resourceDifferences.addAll(createResourceDifferences(ResourcePresence.PROCESSED_ONLY, processedOnly, null));
        resourceDifferences.addAll(createResourceDifferences(ResourcePresence.NAMESPACE_ONLY, null, clusterOnly));
        resourceDifferences.addAll(createResourceDifferences(ResourcePresence.BOTH, leftAndRightProcessedResources, leftAndRightClusterResources));
        return resourceDifferences;
    }

    private List<ResourceDifference> createResourceDifferences(ResourcePresence resourcePresence, Map<ResourceKey, Resource> processed, Map<ResourceKey, Resource> cluster) {
        switch (resourcePresence) {
            case NAMESPACE_ONLY:
                return cluster.values().stream().map(r -> new ResourceDifference(resourcePresence, null, r, null)).collect(Collectors.toList());
            case PROCESSED_ONLY:
                return processed.values().stream().map(r -> new ResourceDifference(resourcePresence, r, null, null)).collect(Collectors.toList());
            case BOTH:
                return processed.entrySet().stream().map(p -> {
                    Resource processedResource = p.getValue();
                    Resource clusterResource = cluster.get(p.getKey());
                    return new ResourceDifference(resourcePresence, processedResource, clusterResource, evaluateAttributeDifference(processedResource, clusterResource));
                }).collect(Collectors.toList());
            default:
                throw new IllegalStateException("Unkown ResourcePresence type: " + resourcePresence);
        }

    }

    private Map<ResourceKey, Resource> filterResourcesInUids(Set<String> uids, Map<ResourceKey, Resource> clusterOnly) {
        return clusterOnly.entrySet().stream().filter(es -> uids.contains(es.getValue().getUid())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> getUids(Map<ResourceKey, Resource> leftAndRightClusterResources) {
        return leftAndRightClusterResources.values().stream().map(Resource::getUid).collect(Collectors.toSet());
    }

    private Map<ResourceKey, Resource> getLeftWhichIsinRightToo(Map<ResourceKey, Resource> left, Map<ResourceKey, Resource> right) {
        return left.entrySet().stream().filter(es -> right.containsKey(es.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<ResourceKey, Resource> getLeftSideOnly(Map<ResourceKey, Resource> left, Map<ResourceKey, Resource> right) {
        return left.entrySet().stream().filter(es -> !right.containsKey(es.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<AttributeDifference> evaluateAttributeDifference(Resource proccessed, Resource cluster) {
        JsonNode processedJsonNode = proccessed.getJsonNode();
        JsonNode clusterJsonNode = cluster.getJsonNode();
        if (processedJsonNode == null && clusterJsonNode == null) {
            throw new IllegalStateException("Cannot evaluate difference between two null resources");
        }
        JsonNode diffs = JsonDiff.asJson(processedJsonNode, clusterJsonNode, DiffFlags.dontNormalizeOpIntoMoveAndCopy());
        List<AttributeDifference> attributeDifferences = new ArrayList<>();
        for (JsonNode diff : diffs) {
            String path = diff.get("path").textValue();
            Operation op = Operation.byName(diff.get("op").textValue());
            JsonNode jsonNodeValue = diff.get("value");
            String value = jsonNodeValue.textValue() != null ? jsonNodeValue.textValue() : jsonNodeValue.toString();
            attributeDifferences.add(new AttributeDifference(proccessed.getName(), proccessed.getKind(), path, value, op, diff));
        }
        return attributeDifferences;
    }

    private boolean isAddedByDefault(JsonNode diff) {
        return "add".equals(diff.get("op").textValue());
    }

    private boolean isRemoveEmptyNode(JsonNode diff) {
        JsonNode value = diff.get("value");
        return "remove".equals(diff.get("op").textValue()) && value.isObject() && value.size() == 0;
    }

}
