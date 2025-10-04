package ca.xef5000.playerinformation.commands;

import ca.xef5000.playerinformation.config.InformationConfig;
import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.InformationType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completer for the /information command
 */
public class InformationTabCompleter implements TabCompleter {
    private final InformationConfig informationConfig;
    
    public InformationTabCompleter(InformationConfig informationConfig) {
        this.informationConfig = informationConfig;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument: subcommands
            List<String> subCommands = Arrays.asList("set", "get", "add", "remove", "reset", "manage", "reload");
            return subCommands.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            // Second argument: player names (for all commands except reload)
            if (!args[0].equalsIgnoreCase("reload")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3) {
            // Third argument: information names
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("set", "get", "add", "remove", "reset").contains(subCommand)) {
                return informationConfig.getInformationNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 4) {
            // Fourth argument: values (for set, add, remove commands)
            String subCommand = args[0].toLowerCase();
            String informationName = args[2];
            
            if (Arrays.asList("set", "add", "remove").contains(subCommand)) {
                InformationDefinition definition = informationConfig.getDefinition(informationName);
                if (definition != null) {
                    return getValueCompletions(definition, subCommand);
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Get value completions based on information type and command
     */
    private List<String> getValueCompletions(InformationDefinition definition, String subCommand) {
        List<String> completions = new ArrayList<>();
        
        switch (definition.getType()) {
            case INT:
                if ("set".equals(subCommand)) {
                    completions.addAll(Arrays.asList("0", "1", "10", "100"));
                } else if ("add".equals(subCommand) || "remove".equals(subCommand)) {
                    completions.addAll(Arrays.asList("1", "5", "10", "50", "100"));
                }
                break;
                
            case STRING:
                if ("set".equals(subCommand)) {
                    completions.add("<text>");
                }
                break;
                
            case UUID:
                if ("set".equals(subCommand)) {
                    completions.add("<uuid>");
                }
                break;
                
            case ENUM:
                if ("set".equals(subCommand) && definition.getEnumValues() != null) {
                    completions.addAll(definition.getEnumValues());
                }
                break;
                
            case LADDER:
                if ("set".equals(subCommand) && definition.getLadderValues() != null) {
                    completions.addAll(definition.getLadderValues());
                }
                break;
                
            case PERMISSION:
                if ("set".equals(subCommand)) {
                    completions.addAll(Arrays.asList("true", "false"));
                }
                break;
        }
        
        return completions;
    }
}
