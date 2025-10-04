package ca.xef5000.playerinformation.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for message handling and formatting
 */
public class MessageUtils {
    
    /**
     * Convert color codes to Minecraft colors
     * @param text The text to colorize
     * @return Colorized text
     */
    public static String colorize(String text) {
        if (text == null) return null;
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * Convert a list of strings to colorized strings
     * @param texts The list of texts to colorize
     * @return List of colorized texts
     */
    public static List<String> colorize(List<String> texts) {
        if (texts == null) return null;
        return texts.stream()
                .map(MessageUtils::colorize)
                .collect(Collectors.toList());
    }
    
    /**
     * Strip color codes from text
     * @param text The text to strip colors from
     * @return Text without color codes
     */
    public static String stripColors(String text) {
        if (text == null) return null;
        return ChatColor.stripColor(text);
    }
    
    /**
     * Send a message to a command sender with color support
     * @param sender The command sender
     * @param message The message to send
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null) return;
        sender.sendMessage(colorize(message));
    }
    
    /**
     * Send multiple messages to a command sender
     * @param sender The command sender
     * @param messages The messages to send
     */
    public static void sendMessages(CommandSender sender, List<String> messages) {
        if (sender == null || messages == null) return;
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }
    
    /**
     * Send a message to a command sender with placeholder replacement
     * @param sender The command sender
     * @param message The message template
     * @param placeholders Placeholder replacements (key, value pairs)
     */
    public static void sendMessage(CommandSender sender, String message, String... placeholders) {
        if (sender == null || message == null) return;
        
        String processedMessage = replacePlaceholders(message, placeholders);
        sendMessage(sender, processedMessage);
    }
    
    /**
     * Replace placeholders in a message
     * @param message The message template
     * @param placeholders Placeholder replacements (key, value pairs)
     * @return Message with placeholders replaced
     */
    public static String replacePlaceholders(String message, String... placeholders) {
        if (message == null) return null;
        
        String result = message;
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            result = result.replace("{" + placeholder + "}", value != null ? value : "null");
        }
        
        return result;
    }
    
    /**
     * Format a number with commas
     * @param number The number to format
     * @return Formatted number string
     */
    public static String formatNumber(long number) {
        return String.format("%,d", number);
    }
    
    /**
     * Format a decimal number with commas
     * @param number The number to format
     * @param decimals Number of decimal places
     * @return Formatted number string
     */
    public static String formatNumber(double number, int decimals) {
        return String.format("%,." + decimals + "f", number);
    }
    
    /**
     * Create a centered message
     * @param message The message to center
     * @param length The total length to center within
     * @return Centered message
     */
    public static String centerMessage(String message, int length) {
        if (message == null) return null;
        
        String stripped = stripColors(message);
        if (stripped.length() >= length) {
            return message;
        }
        
        int padding = (length - stripped.length()) / 2;
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < padding; i++) {
            result.append(" ");
        }
        
        result.append(message);
        return result.toString();
    }
    
    /**
     * Create a progress bar
     * @param current Current value
     * @param max Maximum value
     * @param length Length of the progress bar
     * @param filledChar Character for filled portion
     * @param emptyChar Character for empty portion
     * @param filledColor Color for filled portion
     * @param emptyColor Color for empty portion
     * @return Formatted progress bar
     */
    public static String createProgressBar(double current, double max, int length, 
                                         char filledChar, char emptyChar, 
                                         ChatColor filledColor, ChatColor emptyColor) {
        
        double percentage = Math.max(0, Math.min(1, current / max));
        int filledLength = (int) (length * percentage);
        
        StringBuilder bar = new StringBuilder();
        bar.append(filledColor);
        
        for (int i = 0; i < filledLength; i++) {
            bar.append(filledChar);
        }
        
        bar.append(emptyColor);
        for (int i = filledLength; i < length; i++) {
            bar.append(emptyChar);
        }
        
        return bar.toString();
    }
    
    /**
     * Create a simple progress bar with default settings
     * @param current Current value
     * @param max Maximum value
     * @param length Length of the progress bar
     * @return Formatted progress bar
     */
    public static String createProgressBar(double current, double max, int length) {
        return createProgressBar(current, max, length, '█', '░', ChatColor.GREEN, ChatColor.GRAY);
    }
    
    /**
     * Check if a player has permission and send no permission message if not
     * @param player The player to check
     * @param permission The permission to check
     * @param noPermissionMessage The message to send if no permission
     * @return true if player has permission, false otherwise
     */
    public static boolean checkPermission(Player player, String permission, String noPermissionMessage) {
        if (player.hasPermission(permission)) {
            return true;
        }
        
        if (noPermissionMessage != null) {
            sendMessage(player, noPermissionMessage);
        }
        
        return false;
    }
    
    /**
     * Truncate text to a maximum length
     * @param text The text to truncate
     * @param maxLength Maximum length
     * @param suffix Suffix to add if truncated (e.g., "...")
     * @return Truncated text
     */
    public static String truncate(String text, int maxLength, String suffix) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        
        String truncated = text.substring(0, maxLength - (suffix != null ? suffix.length() : 0));
        return suffix != null ? truncated + suffix : truncated;
    }
    
    /**
     * Truncate text to a maximum length with default "..." suffix
     * @param text The text to truncate
     * @param maxLength Maximum length
     * @return Truncated text
     */
    public static String truncate(String text, int maxLength) {
        return truncate(text, maxLength, "...");
    }
    
    /**
     * Convert milliseconds to a human-readable time format
     * @param milliseconds The time in milliseconds
     * @return Human-readable time string
     */
    public static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Create a header/footer line
     * @param character The character to repeat
     * @param length The length of the line
     * @param color The color of the line
     * @return Formatted line
     */
    public static String createLine(char character, int length, ChatColor color) {
        StringBuilder line = new StringBuilder();
        if (color != null) {
            line.append(color);
        }
        
        for (int i = 0; i < length; i++) {
            line.append(character);
        }
        
        return line.toString();
    }
    
    /**
     * Create a default header/footer line
     * @param length The length of the line
     * @return Formatted line
     */
    public static String createLine(int length) {
        return createLine('-', length, ChatColor.GRAY);
    }
}
