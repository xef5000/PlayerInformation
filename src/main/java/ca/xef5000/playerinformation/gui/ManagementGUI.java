package ca.xef5000.playerinformation.gui;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.config.ConfigManager;
import ca.xef5000.playerinformation.config.InformationConfig;
import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.InformationType;
import ca.xef5000.playerinformation.data.PlayerInformationData;
import ca.xef5000.playerinformation.database.PlayerDataRepository;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Management GUI for viewing and editing player information
 */
public class ManagementGUI {
    private final PlayerInformation plugin;
    private final ConfigManager configManager;
    private final InformationConfig informationConfig;
    private final PlayerDataRepository dataRepository;
    
    public ManagementGUI(PlayerInformation plugin, ConfigManager configManager, 
                        InformationConfig informationConfig, PlayerDataRepository dataRepository) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.informationConfig = informationConfig;
        this.dataRepository = dataRepository;
    }
    
    /**
     * Open the management GUI for a specific player
     */
    public void openManagementGUI(Player viewer, UUID targetPlayerUuid, String targetPlayerName) {
        // Load player data
        dataRepository.loadPlayerData(targetPlayerUuid, targetPlayerName).thenAccept(playerData -> {
            if (playerData == null) {
                viewer.sendMessage(configManager.getMessage("error.gui-error", "error", "Player data not found"));
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                openManagementGUIInternal(viewer, targetPlayerUuid, targetPlayerName, playerData);
            });
        });
    }

    private void openManagementGUIInternal(Player viewer, UUID targetPlayerUuid, String targetPlayerName, PlayerInformationData playerData) {
        // Create GUI
        String title = configManager.getGuiManagementTitle().replace("{player}", targetPlayerName);
        title = colorize(title);

        int size = Math.min(54, Math.max(9, (int) Math.ceil(informationConfig.getAllDefinitions().size() / 9.0) * 9));
        ChestGui gui = new ChestGui(size / 9, title);

        // Create pane
        OutlinePane pane = new OutlinePane(0, 0, 9, size / 9, Pane.Priority.NORMAL);

        // Add information items
        List<InformationDefinition> definitions = new ArrayList<>(informationConfig.getAllDefinitions().values());
        definitions.sort(Comparator.comparing(InformationDefinition::getName));

        for (int i = 0; i < definitions.size() && i < size; i++) {
            InformationDefinition definition = definitions.get(i);
            String currentValue = playerData.getValue(definition.getName(), definition.getDefaultValue());

            GuiItem item = createInformationItem(definition, currentValue, viewer, targetPlayerUuid, targetPlayerName);
            pane.addItem(item);
        }

        gui.addPane(pane);
        viewer.closeInventory();
        gui.show(viewer);
    }

    /**
     * Create an item representing an information definition
     */
    private GuiItem createInformationItem(InformationDefinition definition, String currentValue, 
                                        Player viewer, UUID targetPlayerUuid, String targetPlayerName) {
        
        // Use configured display item if available
        ItemStack item = configManager.getDisplayItem(definition.getType());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Override the display name to show the information name
            meta.setDisplayName(colorize("&e" + definition.getName()));

            // Start with any lore provided by the configured item
            List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            // Append type/current/default info
            lore.add(colorize("&7Type: &f" + definition.getType().name()));
            lore.add(colorize("&7Current: &f" + currentValue));
            lore.add(colorize("&7Default: &f" + definition.getDefaultValue()));
            lore.add("");

             // Add type-specific information
             switch (definition.getType()) {
                 case ENUM:
                     if (definition.getEnumValues() != null) {
                         lore.add(colorize("&7Values: &f" + String.join(", ", definition.getEnumValues())));
                     }
                     break;
                 case LADDER:
                     if (definition.getLadderValues() != null) {
                         lore.add(colorize("&7Ladder: &f" + String.join(" → ", definition.getLadderValues())));
                     }
                     break;
                 case PERMISSION:
                     if (definition.getPermissionNode() != null) {
                         lore.add(colorize("&7Permission: &f" + definition.getPermissionNode()));
                     }
                     break;
                 case MULTIENUM:
                     if (definition.getMultiEnumValues() != null) {
                         List<String> selected = definition.parseMultiEnumValue(currentValue);
                         lore.add(colorize("&7Options: &f" + String.join(", ", definition.getMultiEnumValues())));
                         lore.add(colorize("&7Selected: &f" + (selected.isEmpty() ? "None" : String.join(", ", selected))));
                     }
                     break;

             }

             lore.add("");
             lore.add(colorize("&eClick to edit"));

             meta.setLore(lore);
             item.setItemMeta(meta);
         }

         return new GuiItem(item, event -> {
             event.setCancelled(true);

             // Open edit GUI
             DataEditGUI editGUI = new DataEditGUI(plugin, configManager, informationConfig, dataRepository);
             editGUI.openEditGUI(viewer, targetPlayerUuid, targetPlayerName, definition, currentValue, () -> {
                 // Reopen management GUI safely one tick later
                 plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                         openManagementGUI(viewer, targetPlayerUuid, targetPlayerName), 1L);
             });
         });
     }

    /**
     * Convert color codes to Minecraft colors
     */
    private String colorize(String text) {
        return text.replace('&', '§');
    }
}
