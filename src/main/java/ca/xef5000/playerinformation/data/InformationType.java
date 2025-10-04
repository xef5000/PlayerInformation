package ca.xef5000.playerinformation.data;

import java.util.UUID;

/**
 * Enum representing the different types of information that can be stored for players
 */
public enum InformationType {
    /**
     * Integer values - can be added to, subtracted from
     */
    INT("INTEGER"),
    
    /**
     * String values - text data
     */
    STRING("TEXT"),
    
    /**
     * UUID values - unique identifiers
     */
    UUID("TEXT"),
    
    /**
     * Enum values - predefined set of options
     */
    ENUM("TEXT"),

    /**
     * MultiEnum values - can select multiple from predefined set of options
     */
    MULTIENUM("TEXT"),
    
    /**
     * Ladder values - hierarchical ranking system
     */
    LADDER("TEXT"),
    
    /**
     * Permission values - boolean permission states
     */
    PERMISSION("BOOLEAN");
    
    private final String sqlType;
    
    InformationType(String sqlType) {
        this.sqlType = sqlType;
    }
    
    /**
     * Get the SQL column type for this information type
     * @return SQL column type
     */
    public String getSqlType() {
        return sqlType;
    }
    
    /**
     * Validate if a value is valid for this information type
     * @param value The value to validate
     * @param definition The information definition (for enum/ladder validation)
     * @return true if valid, false otherwise
     */
    public boolean isValidValue(String value, InformationDefinition definition) {
        if (value == null) return false;
        
        switch (this) {
            case INT:
                try {
                    Integer.parseInt(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            
            case STRING:
                return true; // Any string is valid
            
            case UUID:
                try {
                    java.util.UUID.fromString(value);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            
            case ENUM:
                return definition.getEnumValues() != null && 
                       definition.getEnumValues().contains(value);

            case MULTIENUM:
                if (value.isEmpty()) return true; // Empty is valid (no selections)
                if (definition.getMultiEnumValues() == null) return false;

                String[] selectedValues = value.split(",");
                for (String selected : selectedValues) {
                    if (!definition.getMultiEnumValues().contains(selected.trim())) {
                        return false;
                    }
                }
                return true;
            
            case LADDER:
                return definition.getLadderValues() != null && 
                       definition.getLadderValues().contains(value);
            
            case PERMISSION:
                return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
            
            default:
                return false;
        }
    }
    
    /**
     * Convert a string value to the appropriate object type
     * @param value The string value
     * @return The converted object
     */
    public Object convertValue(String value) {
        if (value == null) return null;
        
        switch (this) {
            case INT:
                return Integer.parseInt(value);
            case STRING:
            case ENUM:
            case LADDER:
            case MULTIENUM:
                return value;
            case UUID:
                return java.util.UUID.fromString(value);
            case PERMISSION:
                return Boolean.parseBoolean(value);
            default:
                return value;
        }
    }
    
    /**
     * Get the default value for this type if none is specified
     * @return Default value as string
     */
    public String getDefaultValue() {
        switch (this) {
            case INT:
                return "0";
            case STRING:
            case ENUM:
            case LADDER:
            case MULTIENUM:
                return "";
            case UUID:
                return java.util.UUID.randomUUID().toString();
            case PERMISSION:
                return "false";
            default:
                return "";
        }
    }
}
