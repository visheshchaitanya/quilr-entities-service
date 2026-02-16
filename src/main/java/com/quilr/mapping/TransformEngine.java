package com.quilr.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Transform engine that applies transformation functions to field values
 * Implements all built-in transforms from the plan
 */
@Component
@Log4j2
public class TransformEngine {
    
    private final ObjectMapper objectMapper;
    
    public TransformEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Apply a transformation to a value
     * 
     * @param transformName Name of the transform function
     * @param value Input value
     * @param args Transform arguments (can be null)
     * @param context Full context for accessing other fields
     * @return Transformed value
     */
    public Object applyTransform(String transformName, Object value, JsonNode args, JsonNode context) {
        if (transformName == null || transformName.isBlank()) {
            return value;
        }
        
        try {
            return switch (transformName.toLowerCase()) {
                case "lowercase" -> lowercase(value);
                case "uppercase" -> uppercase(value);
                case "trim" -> trim(value);
                case "coalesce" -> coalesce(value, args, context);
                case "concat" -> concat(value, args, context);
                case "split" -> split(value, args);
                case "parse_iso_date" -> parseIsoDate(value);
                case "boolean_from_string" -> booleanFromString(value);
                case "extract_domain" -> extractDomain(value);
                case "array_first" -> arrayFirst(value);
                case "array_join" -> arrayJoin(value, args);
                case "default_if_null" -> defaultIfNull(value, args);
                case "conditional" -> conditional(value, args, context);
                case "nested_lookup" -> nestedLookup(value, args);
                case "uuid_from_bytes" -> uuidFromBytes(args, context);
                case "uuid_from_string" -> uuidFromString(value, args, context);
                case "extract_secondary_mail" -> extractSecondaryMail(context);
                case "extract_email_secondary" -> extractEmailSecondary(context);
                case "preferred_business_phone" -> preferredBusinessPhone(value);
                case "build_account_id" -> buildAccountId(args, context);
                case "extension_deployment_status" -> extensionDeploymentStatus(value, args, context);
                case "build_extra_info" -> buildExtraInfo(value, args, context);
                case "build_group_extra_info" -> buildGroupExtraInfo(value);
                case "build_role_extra_info" -> buildRoleExtraInfo(value);
                default -> {
                    log.warn("Unknown transform function: {}", transformName);
                    yield value;
                }
            };
        } catch (Exception e) {
            log.error("Error applying transform '{}': {}", transformName, e.getMessage(), e);
            return value; // Return original value on error
        }
    }
    
    // ========== STRING TRANSFORMS ==========
    
    private Object lowercase(Object value) {
        if (value == null) return null;
        return value.toString().toLowerCase();
    }
    
    private Object uppercase(Object value) {
        if (value == null) return null;
        return value.toString().toUpperCase();
    }
    
    private Object trim(Object value) {
        if (value == null) return null;
        return value.toString().trim();
    }
    
    private Object extractDomain(Object value) {
        if (value == null) return null;
        String email = value.toString();
        if (email.contains("@")) {
            return email.split("@")[1];
        }
        return null;
    }
    
    // ========== UTILITY TRANSFORMS ==========
    
    private Object coalesce(Object value, JsonNode args, JsonNode context) {
        if (value != null && !isNullOrEmpty(value)) {
            return value;
        }
        
        // Try fallback fields from args
        if (args != null && args.has("fields")) {
            ArrayNode fields = (ArrayNode) args.get("fields");
            for (JsonNode fieldNode : fields) {
                String fieldPath = fieldNode.asText();
                Object fieldValue = extractFromContext(context, fieldPath);
                if (fieldValue != null && !isNullOrEmpty(fieldValue)) {
                    return fieldValue;
                }
            }
        }
        
        return null;
    }
    
    private Object concat(Object value, JsonNode args, JsonNode context) {
        if (args == null || !args.has("fields")) {
            return value;
        }
        
        String separator = args.has("separator") ? args.get("separator").asText() : "";
        boolean lowercase = args.has("lowercase") && args.get("lowercase").asBoolean();
        
        List<String> parts = new ArrayList<>();
        ArrayNode fields = (ArrayNode) args.get("fields");
        
        for (JsonNode fieldNode : fields) {
            String fieldPath = fieldNode.asText();
            Object fieldValue = extractFromContext(context, fieldPath);
            if (fieldValue != null) {
                parts.add(fieldValue.toString());
            }
        }
        
        String result = String.join(separator, parts);
        return lowercase ? result.toLowerCase() : result;
    }
    
    private Object split(Object value, JsonNode args) {
        if (value == null || args == null) return null;
        
        String delimiter = args.has("delimiter") ? args.get("delimiter").asText() : ",";
        int index = args.has("index") ? args.get("index").asInt() : 0;
        
        String[] parts = value.toString().split(delimiter);
        if (index >= 0 && index < parts.length) {
            return parts[index];
        }
        
        return null;
    }
    
    private Object defaultIfNull(Object value, JsonNode args) {
        if (value != null && !isNullOrEmpty(value)) {
            return value;
        }
        
        if (args != null && args.has("default")) {
            return args.get("default").asText();
        }
        
        return null;
    }
    
    // ========== DATE TRANSFORMS ==========
    
    private Object parseIsoDate(Object value) {
        if (value == null) return null;
        
        try {
            String dateStr = value.toString();
            if (dateStr.isBlank()) return null;
            
            // Try parsing as Instant (timestamp)
            return Instant.parse(dateStr);
        } catch (Exception e) {
            try {
                // Try parsing as LocalDate
                String dateStr = value.toString();
                if (dateStr.length() >= 10) {
                    return LocalDate.parse(dateStr.substring(0, 10));
                }
            } catch (Exception ex) {
                log.warn("Failed to parse date: {}", value);
            }
        }
        
        return null;
    }
    
    // ========== CONVERSION TRANSFORMS ==========
    
    private Object booleanFromString(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return value;
        
        String str = value.toString().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }
    
    // ========== ARRAY TRANSFORMS ==========
    
    private Object arrayFirst(Object value) {
        if (value == null) return null;
        
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.isEmpty() ? null : list.get(0);
        }
        
        if (value instanceof JsonNode && ((JsonNode) value).isArray()) {
            JsonNode array = (JsonNode) value;
            return array.isEmpty() ? null : array.get(0);
        }
        
        return value;
    }
    
    private Object arrayJoin(Object value, JsonNode args) {
        if (value == null) return null;
        
        String separator = args != null && args.has("separator") ? args.get("separator").asText() : ",";
        
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining(separator));
        }
        
        if (value instanceof JsonNode && ((JsonNode) value).isArray()) {
            JsonNode array = (JsonNode) value;
            return StreamSupport.stream(array.spliterator(), false)
                .filter(node -> !node.isNull())
                .map(JsonNode::asText)
                .collect(Collectors.joining(separator));
        }
        
        return value.toString();
    }
    
    // ========== LOGIC TRANSFORMS ==========
    
    private Object conditional(Object value, JsonNode args, JsonNode context) {
        if (args == null) return value;
        
        // Simple conditional: if value matches condition, return thenValue, else elseValue
        if (args.has("equals")) {
            String expected = args.get("equals").asText();
            boolean matches = value != null && value.toString().equals(expected);
            
            if (matches && args.has("then")) {
                return args.get("then").asText();
            } else if (!matches && args.has("else")) {
                return args.get("else").asText();
            }
        }
        
        return value;
    }
    
    // ========== JSON TRANSFORMS ==========
    
    private Object nestedLookup(Object value, JsonNode args) {
        if (value == null || args == null || !args.has("path")) {
            return null;
        }
        
        if (!(value instanceof JsonNode)) {
            return null;
        }
        
        JsonNode node = (JsonNode) value;
        String path = args.get("path").asText();
        
        // Simple dot-notation navigation
        String[] parts = path.split("\\.");
        for (String part : parts) {
            if (node == null || node.isNull()) {
                return null;
            }
            node = node.get(part);
        }
        
        return node != null && !node.isNull() ? node : null;
    }
    
    // ========== UUID TRANSFORMS ==========
    
    private Object uuidFromBytes(JsonNode args, JsonNode context) {
        if (args == null || !args.has("fields")) {
            return UUID.randomUUID();
        }
        
        String prefix = args.has("prefix") ? args.get("prefix").asText() + ":" : "";
        ArrayNode fields = (ArrayNode) args.get("fields");
        
        List<String> parts = new ArrayList<>();
        if (!prefix.isEmpty()) {
            parts.add(prefix);
        }
        
        // Check if we're in an array element context (has _element)
        JsonNode elementNode = context.has("_element") ? context.get("_element") : null;
        
        for (JsonNode fieldNode : fields) {
            String fieldPath = fieldNode.asText();
            Object fieldValue = null;
            
            // For simple field names (like "id"), check _element first if we're in array context
            if (elementNode != null && !fieldPath.contains(".")) {
                JsonNode elementField = elementNode.get(fieldPath);
                if (elementField != null && !elementField.isNull()) {
                    fieldValue = elementField.asText();
                }
            }
            
            // Fall back to extracting from full context
            if (fieldValue == null) {
                fieldValue = extractFromContext(context, fieldPath);
            }
            
            if (fieldValue != null) {
                parts.add(fieldValue.toString());
            }
        }
        
        String combined = String.join(":", parts);
        return UUID.nameUUIDFromBytes(combined.getBytes());
    }
    
    private Object uuidFromString(Object value, JsonNode args, JsonNode context) {
        // If value is provided, parse it
        if (value != null && !value.toString().isBlank()) {
            try {
                return UUID.fromString(value.toString());
            } catch (Exception e) {
                log.warn("Failed to parse UUID from: {}", value);
            }
        }
        
        // Otherwise, extract from context using field path
        if (args != null && args.has("field")) {
            String fieldPath = args.get("field").asText();
            Object fieldValue = extractFromContext(context, fieldPath);
            if (fieldValue != null) {
                try {
                    return UUID.fromString(fieldValue.toString());
                } catch (Exception e) {
                    log.warn("Failed to parse UUID from field {}: {}", fieldPath, fieldValue);
                }
            }
        }
        
        return null;
    }
    
    // ========== CUSTOM TRANSFORMS ==========
    
    private Object extractSecondaryMail(JsonNode context) {
        // Extract domain-matching secondary email from otherMails
        String primaryEmail = extractStringFromContext(context, "data.mail");
        if (primaryEmail == null || !primaryEmail.contains("@")) {
            return null;
        }
        
        String primaryDomain = primaryEmail.split("@")[1];
        JsonNode otherMails = extractNodeFromContext(context, "data.otherMails");
        
        if (otherMails != null && otherMails.isArray()) {
            for (JsonNode mailNode : otherMails) {
                String otherMail = mailNode.asText();
                if (otherMail.contains("@")) {
                    String otherDomain = otherMail.split("@")[1];
                    if (otherDomain.equals(primaryDomain)) {
                        return otherMail.toLowerCase();
                    }
                }
            }
        }
        
        return null;
    }
    
    private Object extractEmailSecondary(JsonNode context) {
        // Extract non-domain-matching email from otherMails
        String primaryEmail = extractStringFromContext(context, "data.mail");
        String primaryDomain = null;
        if (primaryEmail != null && primaryEmail.contains("@")) {
            primaryDomain = primaryEmail.split("@")[1];
        }
        
        JsonNode otherMails = extractNodeFromContext(context, "data.otherMails");
        
        if (otherMails != null && otherMails.isArray()) {
            for (JsonNode mailNode : otherMails) {
                String otherMail = mailNode.asText();
                if (otherMail.contains("@")) {
                    String otherDomain = otherMail.split("@")[1];
                    if (primaryDomain == null || !otherDomain.equals(primaryDomain)) {
                        return otherMail.toLowerCase();
                    }
                }
            }
        }
        
        return null;
    }
    
    private Object preferredBusinessPhone(Object value) {
        if (value == null) return null;
        
        if (value instanceof List) {
            List<?> phones = (List<?>) value;
            for (Object phone : phones) {
                if (phone != null && !phone.toString().isBlank()) {
                    return phone.toString();
                }
            }
        }
        
        if (value instanceof JsonNode && ((JsonNode) value).isArray()) {
            JsonNode phones = (JsonNode) value;
            for (JsonNode phone : phones) {
                if (!phone.isNull() && !phone.asText().isBlank()) {
                    return phone.asText();
                }
            }
        }
        
        return null;
    }
    
    private Object buildAccountId(JsonNode args, JsonNode context) {
        // Build account ID: lower(email)_appId with fallbacks
        String email = extractStringFromContext(context, "data.mail");
        String upn = extractStringFromContext(context, "data.userPrincipalName");
        String userId = extractStringFromContext(context, "data.id");
        
        String identifier = email != null ? email : (upn != null ? upn : userId);
        if (identifier == null) {
            identifier = "unknown";
        }
        
        String appId = args != null && args.has("appId") ? args.get("appId").asText() : "default";
        
        return identifier.toLowerCase() + "_" + appId;
    }
    
    private Object extensionDeploymentStatus(Object value, JsonNode args, JsonNode context) {
        // Determine extension deployment status based on accountEnabled and userType
        Boolean accountEnabled = null;
        
        if (value instanceof Boolean) {
            accountEnabled = (Boolean) value;
        } else if (value != null) {
            // Handle string "true"/"false" or other types
            String strValue = value.toString().toLowerCase();
            if ("true".equals(strValue) || "1".equals(strValue)) {
                accountEnabled = true;
            } else if ("false".equals(strValue) || "0".equals(strValue)) {
                accountEnabled = false;
            }
        }
        
        if (accountEnabled == null) {
            return "Excluded";
        }
        
        String userType = extractStringFromContext(context, "data.userType");
        boolean isGuest = "guest".equalsIgnoreCase(userType);
        
        return (accountEnabled && !isGuest) ? "Ready to Deploy" : "Excluded";
    }
    
    /**
     * Build extra info map from data node - used for user entity
     * This is a pass-through since DynamicEntityTransformer handles the actual building
     */
    private Object buildExtraInfo(Object value, JsonNode args, JsonNode context) {
        // The actual extraInfo building is done in DynamicEntityTransformer.buildUserExtraInfo
        // This transform is a placeholder that returns the value as-is
        // The DynamicEntityTransformer will override this with the properly built extraInfo
        return value;
    }
    
    /**
     * Build extra info for group entity - placeholder
     */
    private Object buildGroupExtraInfo(Object value) {
        // Placeholder - actual building done in DynamicEntityTransformer
        return value;
    }
    
    /**
     * Build extra info for role entity - placeholder
     */
    private Object buildRoleExtraInfo(Object value) {
        // Placeholder - actual building done in DynamicEntityTransformer
        return value;
    }
    
    // ========== HELPER METHODS ==========
    
    private boolean isNullOrEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String && ((String) value).isBlank()) return true;
        if (value instanceof Collection && ((Collection<?>) value).isEmpty()) return true;
        return false;
    }
    
    private Object extractFromContext(JsonNode context, String path) {
        if (context == null || path == null) return null;
        
        // Handle root-level fields (tenant, subscriber, domain, instance_id)
        if (!path.contains(".")) {
            JsonNode node = context.get(path);
            return node != null && !node.isNull() ? node.asText() : null;
        }
        
        // Handle nested paths with $ prefix (JsonPath style)
        String cleanPath = path.startsWith("$.") ? path.substring(2) : path;
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
        
        // Return appropriate type
        if (current.isTextual()) return current.asText();
        if (current.isBoolean()) return current.asBoolean();
        if (current.isNumber()) return current.asLong();
        if (current.isArray() || current.isObject()) return current;
        
        return current.asText();
    }
    
    private String extractStringFromContext(JsonNode context, String path) {
        Object value = extractFromContext(context, path);
        return value != null ? value.toString() : null;
    }
    
    private JsonNode extractNodeFromContext(JsonNode context, String path) {
        if (context == null || path == null) return null;
        
        String cleanPath = path.startsWith("$.") ? path.substring(2) : path;
        String[] parts = cleanPath.split("\\.");
        
        JsonNode current = context;
        for (String part : parts) {
            if (current == null || current.isNull()) {
                return null;
            }
            current = current.get(part);
        }
        
        return current;
    }
}
