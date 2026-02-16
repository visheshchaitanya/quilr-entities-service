package com.quilr.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.quilr.dto.RawEntityMessage;
import com.quilr.model.mapping.DataType;
import com.quilr.model.mapping.FieldMapping;
import com.quilr.repository.FieldMappingRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Field mapping engine that extracts values from JSON payloads using JsonPath
 * and maps them to entity objects dynamically
 */
@Component
@Log4j2
public class FieldMappingEngine {
    
    private final FieldMappingRepository fieldMappingRepository;
    private final TransformEngine transformEngine;
    private final ObjectMapper objectMapper;
    private final Configuration jsonPathConfig;
    
    public FieldMappingEngine(
            FieldMappingRepository fieldMappingRepository,
            TransformEngine transformEngine,
            ObjectMapper objectMapper) {
        this.fieldMappingRepository = fieldMappingRepository;
        this.transformEngine = transformEngine;
        this.objectMapper = objectMapper;
        
        // Configure JsonPath to use Jackson
        this.jsonPathConfig = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .options(Option.SUPPRESS_EXCEPTIONS, Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();
    }
    
    /**
     * Map a single entity from JSON payload
     * 
     * @param source Source JSON payload
     * @param context Raw entity message context
     * @param targetClass Target entity class
     * @return Mapped entity instance
     */
    public <T> T mapEntity(JsonNode source, RawEntityMessage context, Class<T> targetClass) {
        String vendor = context.getVendor();
        String entityType = context.getEntityType().name().toLowerCase();
        String targetEntity = getTargetEntityName(targetClass);
        
        log.debug("Mapping entity: vendor={}, entityType={}, targetEntity={}", vendor, entityType, targetEntity);
        
        // Load field mappings for this target entity
        List<FieldMapping> mappings = fieldMappingRepository.findByVendorAndEntityTypeAndTargetEntity(
            vendor, entityType, targetEntity
        );
        
        if (mappings.isEmpty()) {
            log.warn("No field mappings found for vendor={}, entityType={}, targetEntity={}", 
                vendor, entityType, targetEntity);
            return null;
        }
        
        // Build full context by merging source with context fields
        JsonNode fullContext = buildFullContext(source, context);
        
        // Extract and map all fields
        Map<String, Object> fieldValues = new HashMap<>();
        
        for (FieldMapping mapping : mappings) {
            try {
                Object value = extractAndTransformField(mapping, fullContext);
                
                // Handle default values
                if (value == null && mapping.getDefaultValue() != null) {
                    value = parseDefaultValue(mapping.getDefaultValue(), mapping.getDataType());
                    log.trace("Using default value for {}.{}: {}", targetEntity, mapping.getTargetField(), value);
                }
                
                // Add tenantId from context for entities that need it
                if ("tenantId".equals(mapping.getTargetField()) && value == null) {
                    value = extractTenantId(context);
                }
                
                // Add instanceId from context for entities that need it
                if ("instanceId".equals(mapping.getTargetField()) && value == null) {
                    value = extractInstanceId(context);
                }
                
                if (value != null) {
                    fieldValues.put(mapping.getTargetField(), value);
                    log.trace("Mapped {}.{} = {} (from: {})", 
                        targetEntity, mapping.getTargetField(), value, mapping.getSourcePath());
                } else if (mapping.getRequired() != null && mapping.getRequired()) {
                    log.warn("Required field {}.{} is null (source: {})", 
                        targetEntity, mapping.getTargetField(), mapping.getSourcePath());
                }
                
            } catch (Exception e) {
                log.error("Error mapping field {}.{}: {}", targetEntity, mapping.getTargetField(), e.getMessage());
            }
        }
        
        // Validate critical fields before building entity
        if ("user".equals(targetEntity) && !fieldValues.containsKey("id")) {
            log.error("CRITICAL: user.id is missing from field values! Available fields: {}", fieldValues.keySet());
        }
        
        // Build entity instance from field values
        return buildEntityInstance(targetClass, fieldValues);
    }
    
    /**
     * Map a list of entities from an array in the JSON payload
     * Critical for handling groups, roles, departments, office locations
     * 
     * @param source Source JSON payload
     * @param context Raw entity message context
     * @param targetClass Target entity class
     * @param arrayPath Path to the array (e.g., "data.groups")
     * @return List of mapped entity instances
     */
    public <T> List<T> mapEntityList(JsonNode source, RawEntityMessage context, Class<T> targetClass, String arrayPath) {
        String vendor = context.getVendor();
        String entityType = context.getEntityType().name().toLowerCase();
        String targetEntity = getTargetEntityName(targetClass);
        
        log.debug("Mapping entity list: vendor={}, entityType={}, targetEntity={}, arrayPath={}", 
            vendor, entityType, targetEntity, arrayPath);
        
        // Load field mappings for this target entity
        List<FieldMapping> mappings = fieldMappingRepository.findByVendorAndEntityTypeAndTargetEntity(
            vendor, entityType, targetEntity
        );
        
        if (mappings.isEmpty()) {
            log.warn("No field mappings found for vendor={}, entityType={}, targetEntity={}", 
                vendor, entityType, targetEntity);
            return Collections.emptyList();
        }
        
        // Build full context
        JsonNode fullContext = buildFullContext(source, context);
        
        // Extract array from source
        JsonNode arrayNode = extractArrayNode(fullContext, arrayPath);
        
        if (arrayNode == null) {
            log.warn("Array extraction returned null for path: {} (targetEntity: {})", arrayPath, targetEntity);
            return Collections.emptyList();
        }
        
        if (!arrayNode.isArray()) {
            log.warn("Extracted node at path {} is not an array, it's a {}: {}", 
                arrayPath, arrayNode.getNodeType(), arrayNode);
            return Collections.emptyList();
        }
        
        if (arrayNode.isEmpty()) {
            log.debug("Array at path {} is empty (targetEntity: {})", arrayPath, targetEntity);
            return Collections.emptyList();
        }
        
        log.debug("Successfully extracted array at path {} with {} elements (targetEntity: {})", 
            arrayPath, arrayNode.size(), targetEntity);
        
        List<T> entities = new ArrayList<>();
        UUID tenantId = extractTenantId(context);
        
        // Iterate over each element in the array
        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode element = arrayNode.get(i);
            
            try {
                // Create element context by merging array element with parent context
                JsonNode elementContext = createElementContext(fullContext, element, arrayPath, i);
                
                Map<String, Object> fieldValues = new HashMap<>();
                
                for (FieldMapping mapping : mappings) {
                    try {
                        Object value = extractAndTransformFieldForArrayElement(
                            mapping, elementContext, element, i
                        );
                        
                        // Handle default values
                        if (value == null && mapping.getDefaultValue() != null) {
                            value = parseDefaultValue(mapping.getDefaultValue(), mapping.getDataType());
                        }
                        
                        // Inject tenantId for all array entities
                        if ("tenantId".equals(mapping.getTargetField())) {
                            value = tenantId;
                        }
                        
                        if (value != null) {
                            fieldValues.put(mapping.getTargetField(), value);
                        }
                        
                    } catch (Exception e) {
                        log.error("Error mapping array field {}.{} at index {}: {}", 
                            targetEntity, mapping.getTargetField(), i, e.getMessage());
                    }
                }
                
                // Build entity instance
                T entity = buildEntityInstance(targetClass, fieldValues);
                if (entity != null) {
                    entities.add(entity);
                }
                
            } catch (Exception e) {
                log.error("Error mapping array element at index {}: {}", i, e.getMessage());
            }
        }
        
        log.debug("Mapped {} entities for {}", entities.size(), targetEntity);
        return entities;
    }
    
    /**
     * Extract and transform a field value
     */
    private Object extractAndTransformField(FieldMapping mapping, JsonNode context) {
        // Extract raw value
        Object value = extractValue(context, mapping.getSourcePath(), mapping.getDataType());
        
        // Try fallback paths if value is null
        if (value == null && mapping.getFallbackPaths() != null && mapping.getFallbackPaths().isArray()) {
            for (JsonNode fallbackPath : mapping.getFallbackPaths()) {
                value = extractValue(context, fallbackPath.asText(), mapping.getDataType());
                if (value != null) {
                    break;
                }
            }
        }
        
        // Apply transformation
        // Some transforms (like build_account_id) extract from context directly and should run even if value is null
        if (mapping.getTransform() != null) {
            value = transformEngine.applyTransform(
                mapping.getTransform(),
                value,
                mapping.getTransformArgs(),
                context
            );
        }
        
        return value;
    }
    
    /**
     * Extract and transform field for array element
     * Handles [*] notation in source paths
     */
    private Object extractAndTransformFieldForArrayElement(
            FieldMapping mapping, JsonNode elementContext, JsonNode element, int index) {
        
        String sourcePath = mapping.getSourcePath();
        
        // If source path contains [*], replace with actual index or extract directly from element
        if (sourcePath.contains("[*]")) {
            // Extract the field name after [*]
            String[] parts = sourcePath.split("\\[\\*\\]\\.");
            if (parts.length > 1) {
                String fieldName = parts[1];
                // Extract directly from element
                Object value = extractValueFromNode(element, fieldName, mapping.getDataType());
                
                // Apply transformation
                if (value != null && mapping.getTransform() != null) {
                    value = transformEngine.applyTransform(
                        mapping.getTransform(),
                        value,
                        mapping.getTransformArgs(),
                        elementContext
                    );
                }
                
                return value;
            }
        }
        
        // Otherwise, extract normally from element context
        return extractAndTransformField(mapping, elementContext);
    }
    
    /**
     * Extract value from JSON using JsonPath
     */
    private Object extractValue(JsonNode context, String path, DataType dataType) {
        if (path == null || context == null) {
            log.trace("extractValue: path or context is null - path={}, context={}", path, context != null);
            return null;
        }
        
        log.trace("extractValue: path={}, dataType={}", path, dataType);
        
        try {
            // Convert JsonNode to string for JsonPath processing
            String jsonString = objectMapper.writeValueAsString(context);
            
            // Ensure path starts with $
            String jsonPath = path.startsWith("$") ? path : "$." + path;
            
            // Remove [*] for single value extraction
            if (jsonPath.contains("[*]")) {
                jsonPath = jsonPath.replace("[*]", "[0]");
            }
            
            log.trace("extractValue: Using JsonPath: {}", jsonPath);
            
            Object rawValue = JsonPath.using(jsonPathConfig).parse(jsonString).read(jsonPath);
            
            log.trace("extractValue: Raw value extracted: {} (type: {})", 
                rawValue, rawValue != null ? rawValue.getClass().getSimpleName() : "null");
            
            Object convertedValue = convertValue(rawValue, dataType);
            
            log.trace("extractValue: Converted value: {} (type: {})", 
                convertedValue, convertedValue != null ? convertedValue.getClass().getSimpleName() : "null");
            
            return convertedValue;
            
        } catch (Exception e) {
            log.warn("Failed to extract value at path {}: {} - attempting fallback", path, e.getMessage());
            
            // Fallback: try direct JsonNode navigation
            Object fallbackValue = extractValueDirectly(context, path, dataType);
            if (fallbackValue != null) {
                log.debug("extractValue: Fallback successful for path {}: {}", path, fallbackValue);
                return fallbackValue;
            }
            
            log.debug("extractValue: Both JsonPath and fallback failed for path {}", path);
            return null;
        }
    }
    
    /**
     * Fallback method to extract value directly from JsonNode without JsonPath
     */
    private Object extractValueDirectly(JsonNode context, String path, DataType dataType) {
        try {
            // Remove $ prefix if present
            String cleanPath = path.startsWith("$.") ? path.substring(2) : 
                              (path.startsWith("$") ? path.substring(1) : path);
            
            // Navigate through the path
            String[] parts = cleanPath.split("\\.");
            JsonNode current = context;
            
            for (String part : parts) {
                if (current == null || current.isNull()) {
                    return null;
                }
                current = current.get(part);
            }
            
            if (current == null || current.isNull()) {
                return null;
            }
            
            // Convert to target type
            return convertJsonNodeToValue(current, dataType);
            
        } catch (Exception e) {
            log.trace("Direct extraction failed for path {}: {}", path, e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract value directly from a JsonNode
     */
    private Object extractValueFromNode(JsonNode node, String fieldName, DataType dataType) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode.isNull()) {
            return null;
        }
        
        return convertJsonNodeToValue(fieldNode, dataType);
    }
    
    /**
     * Extract array node from context
     */
    private JsonNode extractArrayNode(JsonNode context, String arrayPath) {
        log.trace("extractArrayNode: Attempting to extract array at path: {}", arrayPath);
        
        try {
            String jsonString = objectMapper.writeValueAsString(context);
            String jsonPath = arrayPath.startsWith("$") ? arrayPath : "$." + arrayPath;
            
            log.trace("extractArrayNode: Using JsonPath: {}", jsonPath);
            
            Object result = JsonPath.using(jsonPathConfig).parse(jsonString).read(jsonPath);
            
            log.trace("extractArrayNode: Raw result type: {}", 
                result != null ? result.getClass().getSimpleName() : "null");
            
            JsonNode arrayNode = null;
            
            if (result instanceof JsonNode) {
                arrayNode = (JsonNode) result;
            } else if (result != null) {
                // Convert result to JsonNode
                arrayNode = objectMapper.valueToTree(result);
            }
            
            if (arrayNode != null && arrayNode.isArray()) {
                log.debug("extractArrayNode: Successfully extracted array at path {} with {} elements", 
                    arrayPath, arrayNode.size());
                return arrayNode;
            } else {
                log.debug("extractArrayNode: Result at path {} is not an array or is null. Node type: {}", 
                    arrayPath, arrayNode != null ? arrayNode.getNodeType() : "null");
            }
            
        } catch (Exception e) {
            log.warn("extractArrayNode: JsonPath failed for path {}: {} - attempting fallback", 
                arrayPath, e.getMessage());
        }
        
        // Fallback: try direct JsonNode navigation
        JsonNode fallbackArray = extractArrayNodeDirectly(context, arrayPath);
        if (fallbackArray != null && fallbackArray.isArray()) {
            log.debug("extractArrayNode: Fallback successful for path {} with {} elements", 
                arrayPath, fallbackArray.size());
            return fallbackArray;
        }
        
        log.debug("extractArrayNode: Both JsonPath and fallback failed for path {}", arrayPath);
        return null;
    }
    
    /**
     * Fallback method to extract array directly from JsonNode without JsonPath
     */
    private JsonNode extractArrayNodeDirectly(JsonNode context, String arrayPath) {
        try {
            // Remove $ prefix if present
            String cleanPath = arrayPath.startsWith("$.") ? arrayPath.substring(2) : 
                              (arrayPath.startsWith("$") ? arrayPath.substring(1) : arrayPath);
            
            // Navigate through the path
            String[] parts = cleanPath.split("\\.");
            JsonNode current = context;
            
            for (String part : parts) {
                if (current == null || current.isNull()) {
                    return null;
                }
                current = current.get(part);
            }
            
            if (current != null && current.isArray()) {
                return current;
            }
            
            return null;
            
        } catch (Exception e) {
            log.trace("Direct array extraction failed for path {}: {}", arrayPath, e.getMessage());
            return null;
        }
    }
    
    /**
     * Create element context by merging array element with parent context
     */
    private JsonNode createElementContext(JsonNode parentContext, JsonNode element, String arrayPath, int index) {
        ObjectNode elementContext = objectMapper.createObjectNode();
        
        // Copy all fields from parent context
        parentContext.fields().forEachRemaining(entry -> {
            elementContext.set(entry.getKey(), entry.getValue());
        });
        
        // Add element data
        elementContext.set("_element", element);
        elementContext.put("_index", index);
        
        return elementContext;
    }
    
    /**
     * Build full context by merging source with context fields
     * 
     * IMPORTANT: The source parameter is typically message.getData() which is just the 'data' node,
     * but field mappings use paths like $.data.id expecting the full payload structure.
     * We wrap the source in a 'data' key to match the expected structure.
     */
    private JsonNode buildFullContext(JsonNode source, RawEntityMessage context) {
        ObjectNode fullContext = objectMapper.createObjectNode();
        
        // Add context fields at root level
        fullContext.put("tenant", context.getTenant());
        fullContext.put("subscriber", context.getSubscriber());
        fullContext.put("instance_id", context.getInstanceId());
        fullContext.put("domain", context.getDomain());
        
        // Wrap source in 'data' key to match field mapping paths ($.data.id, $.data.groups, etc.)
        // The source is message.getData() which is already the 'data' node from the original payload
        if (source != null) {
            fullContext.set("data", source);
        }
        
        return fullContext;
    }
    
    /**
     * Convert raw value to appropriate type
     */
    private Object convertValue(Object rawValue, DataType dataType) {
        if (rawValue == null) {
            return null;
        }
        
        // Handle JsonNode values
        if (rawValue instanceof JsonNode) {
            return convertJsonNodeToValue((JsonNode) rawValue, dataType);
        }
        
        return switch (dataType) {
            case STRING -> rawValue.toString();
            case INTEGER -> rawValue instanceof Number ? ((Number) rawValue).intValue() : Integer.parseInt(rawValue.toString());
            case LONG -> rawValue instanceof Number ? ((Number) rawValue).longValue() : Long.parseLong(rawValue.toString());
            case DOUBLE -> rawValue instanceof Number ? ((Number) rawValue).doubleValue() : Double.parseDouble(rawValue.toString());
            case BOOLEAN -> rawValue instanceof Boolean ? rawValue : Boolean.parseBoolean(rawValue.toString());
            case UUID -> rawValue instanceof UUID ? rawValue : UUID.fromString(rawValue.toString());
            case ARRAY -> rawValue instanceof List ? rawValue : convertToList(rawValue);
            case JSONB, OBJECT -> rawValue;
            default -> rawValue;
        };
    }
    
    /**
     * Convert JsonNode to appropriate Java type
     */
    private Object convertJsonNodeToValue(JsonNode node, DataType dataType) {
        if (node.isNull()) {
            return null;
        }
        
        return switch (dataType) {
            case STRING -> node.asText();
            case INTEGER -> node.asInt();
            case LONG -> node.asLong();
            case DOUBLE -> node.asDouble();
            case BOOLEAN -> node.asBoolean();
            case UUID -> UUID.fromString(node.asText());
            case TIMESTAMP -> Instant.parse(node.asText());
            case DATE -> LocalDate.parse(node.asText());
            case ARRAY -> convertJsonArrayToList(node);
            case JSONB, OBJECT -> node;
            default -> node.asText();
        };
    }
    
    /**
     * Convert JsonNode array to List
     */
    private List<String> convertJsonArrayToList(JsonNode node) {
        if (!node.isArray()) {
            return Collections.emptyList();
        }
        
        List<String> list = new ArrayList<>();
        node.forEach(element -> {
            if (!element.isNull()) {
                list.add(element.asText());
            }
        });
        return list;
    }
    
    /**
     * Convert object to list
     */
    private List<?> convertToList(Object value) {
        if (value instanceof List) {
            return (List<?>) value;
        }
        return Collections.singletonList(value);
    }
    
    /**
     * Parse default value based on data type
     */
    private Object parseDefaultValue(String defaultValue, DataType dataType) {
        if (defaultValue == null) {
            return null;
        }
        
        return switch (dataType) {
            case STRING -> defaultValue;
            case INTEGER -> Integer.parseInt(defaultValue);
            case LONG -> Long.parseLong(defaultValue);
            case DOUBLE -> Double.parseDouble(defaultValue);
            case BOOLEAN -> Boolean.parseBoolean(defaultValue);
            case UUID -> UUID.fromString(defaultValue);
            default -> defaultValue;
        };
    }
    
    /**
     * Build entity instance from field values using reflection
     */
    private <T> T buildEntityInstance(Class<T> targetClass, Map<String, Object> fieldValues) {
        try {
            T instance = targetClass.getDeclaredConstructor().newInstance();
            
            for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                
                try {
                    // Find setter method
                    String setterName = "set" + capitalize(fieldName);
                    Method setter = findSetter(targetClass, setterName, value);
                    
                    if (setter != null) {
                        setter.invoke(instance, value);
                    } else {
                        log.debug("No setter found for field: {}", fieldName);
                    }
                    
                } catch (Exception e) {
                    log.warn("Failed to set field {}: {}", fieldName, e.getMessage());
                }
            }
            
            return instance;
            
        } catch (Exception e) {
            log.error("Failed to build entity instance for {}: {}", targetClass.getSimpleName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Find setter method for a field
     */
    private Method findSetter(Class<?> clazz, String setterName, Object value) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType.isAssignableFrom(value.getClass())) {
                    return method;
                }
            }
        }
        return null;
    }
    
    /**
     * Extract tenant ID from context
     */
    private UUID extractTenantId(RawEntityMessage context) {
        String tenantIdStr = context.getTenant();
        if (tenantIdStr != null) {
            try {
                return UUID.fromString(tenantIdStr);
            } catch (Exception e) {
                log.warn("Failed to parse tenant ID: {}", tenantIdStr);
            }
        }
        return null;
    }
    
    /**
     * Extract instance ID from context
     */
    private UUID extractInstanceId(RawEntityMessage context) {
        String instanceIdStr = context.getInstanceId();
        if (instanceIdStr != null) {
            try {
                return UUID.fromString(instanceIdStr);
            } catch (Exception e) {
                log.warn("Failed to parse instance ID: {}", instanceIdStr);
            }
        }
        return null;
    }
    
    /**
     * Get target entity name from class
     */
    private String getTargetEntityName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        
        // Remove "Entity" suffix and convert to lowercase
        if (className.endsWith("Entity")) {
            className = className.substring(0, className.length() - 6);
        }
        
        // Convert camelCase to snake_case
        return className.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * Capitalize first letter
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
