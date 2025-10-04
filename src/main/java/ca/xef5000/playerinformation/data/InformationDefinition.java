package ca.xef5000.playerinformation.data;

import java.util.List;
import java.util.Objects;

/**
 * Represents a definition of an information type that can be stored for players
 */
public class InformationDefinition {
    private final String name;
    private final InformationType type;
    private final String defaultValue;
    private final List<String> enumValues;
    private final List<String> ladderValues;
    private final String permissionNode;
    
    public InformationDefinition(String name, InformationType type, String defaultValue) {
        this(name, type, defaultValue, null, null, null);
    }
    
    public InformationDefinition(String name, InformationType type, String defaultValue, 
                               List<String> enumValues, List<String> ladderValues, String permissionNode) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue != null ? defaultValue : type.getDefaultValue();
        this.enumValues = enumValues;
        this.ladderValues = ladderValues;
        this.permissionNode = permissionNode;
    }
    
    /**
     * Get the name of this information definition
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the type of this information
     * @return The information type
     */
    public InformationType getType() {
        return type;
    }
    
    /**
     * Get the default value for this information
     * @return The default value as string
     */
    public String getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * Get the enum values (only applicable for ENUM type)
     * @return List of enum values, or null if not an enum type
     */
    public List<String> getEnumValues() {
        return enumValues;
    }
    
    /**
     * Get the ladder values (only applicable for LADDER type)
     * @return List of ladder values, or null if not a ladder type
     */
    public List<String> getLadderValues() {
        return ladderValues;
    }
    
    /**
     * Get the permission node (only applicable for PERMISSION type)
     * @return The permission node, or null if not a permission type
     */
    public String getPermissionNode() {
        return permissionNode;
    }
    
    /**
     * Validate if a value is valid for this information definition
     * @param value The value to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidValue(String value) {
        return type.isValidValue(value, this);
    }
    
    /**
     * Get the next value in an enum or ladder sequence
     * @param currentValue The current value
     * @return The next value, or the current value if at the end or not applicable
     */
    public String getNextValue(String currentValue) {
        if (type == InformationType.ENUM && enumValues != null) {
            int currentIndex = enumValues.indexOf(currentValue);
            if (currentIndex >= 0 && currentIndex < enumValues.size() - 1) {
                return enumValues.get(currentIndex + 1);
            }
            return currentValue;
        }
        
        if (type == InformationType.LADDER && ladderValues != null) {
            int currentIndex = ladderValues.indexOf(currentValue);
            if (currentIndex >= 0 && currentIndex < ladderValues.size() - 1) {
                return ladderValues.get(currentIndex + 1);
            }
            return currentValue;
        }
        
        return currentValue;
    }
    
    /**
     * Get the previous value in an enum or ladder sequence
     * @param currentValue The current value
     * @return The previous value, or the current value if at the beginning or not applicable
     */
    public String getPreviousValue(String currentValue) {
        if (type == InformationType.ENUM && enumValues != null) {
            int currentIndex = enumValues.indexOf(currentValue);
            if (currentIndex > 0) {
                return enumValues.get(currentIndex - 1);
            }
            return currentValue;
        }
        
        if (type == InformationType.LADDER && ladderValues != null) {
            int currentIndex = ladderValues.indexOf(currentValue);
            if (currentIndex > 0) {
                return ladderValues.get(currentIndex - 1);
            }
            return currentValue;
        }
        
        return currentValue;
    }
    
    /**
     * Get the SQL column name for this information (sanitized)
     * @return SQL-safe column name
     */
    public String getColumnName() {
        return "info_" + name.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InformationDefinition that = (InformationDefinition) o;
        return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return "InformationDefinition{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", defaultValue='" + defaultValue + '\'' +
                '}';
    }
}
