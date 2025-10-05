package ca.xef5000.playerinformation.gui;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.config.ConfigManager;
import ca.xef5000.playerinformation.config.InformationConfig;
import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.InformationType;
import ca.xef5000.playerinformation.database.PlayerDataRepository;
import ca.xef5000.playerinformation.listeners.ChatInputListener;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI for editing specific information values
 */
public class DataEditGUI {
    private final PlayerInformation plugin;
    private final ConfigManager configManager;
    private final InformationConfig informationConfig;
    private final PlayerDataRepository dataRepository;
    private final ChatInputListener chatInputListener;

    public DataEditGUI(PlayerInformation plugin, ConfigManager configManager, 
                      InformationConfig informationConfig, PlayerDataRepository dataRepository) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.informationConfig = informationConfig;
        this.dataRepository = dataRepository;
        this.chatInputListener = plugin.getChatInputListener();
    }
    
    /**
     * Open the edit GUI for a specific information
     */
    public void openEditGUI(Player viewer, UUID targetPlayerUuid, String targetPlayerName, 
                           InformationDefinition definition, String currentValue, Runnable backCallback) {
        
        String title = configManager.getGuiEditTitle()
                .replace("{data}", definition.getName())
                .replace("{player}", targetPlayerName);
        title = colorize(title);
        
        ChestGui gui = new ChestGui(3, title);
        OutlinePane pane = new OutlinePane(0, 0, 9, 2, Pane.Priority.NORMAL);
        
        // Create items based on information type
        switch (definition.getType()) {
            case INT:
                createIntegerEditItems(pane, viewer, targetPlayerUuid, targetPlayerName, definition, currentValue, backCallback);
                break;
            case STRING:
            case UUID:
                createStringEditItems(pane, viewer, targetPlayerUuid, targetPlayerName, definition, currentValue, backCallback);
                break;
            case ENUM:
                createEnumEditItems(gui, pane, viewer, targetPlayerUuid, targetPlayerName, definition, currentValue, backCallback);
                break;
            case MULTIENUM:
                createMultiEnumEditItems(gui, pane, viewer, targetPlayerUuid, targetPlayerName, definition, currentValue, backCallback);
                break;
            case LADDER:
                createLadderEditItems(pane, viewer, targetPlayerUuid, targetPlayerName, definition, currentValue, backCallback);
                break;
            case PERMISSION:
                createPermissionEditItems(pane, viewer, targetPlayerUuid, targetPlayerName, definition, currentValue, backCallback);
                break;
        }
        
        // Add back button
        addBackButton(pane, backCallback);
        
        gui.addPane(pane);
        gui.show(viewer);
    }
    
    /**
     * Create edit items for integer type
     */
    private void createIntegerEditItems(OutlinePane pane, Player viewer, UUID targetPlayerUuid, String targetPlayerName,
                                      InformationDefinition definition, String currentValue, Runnable backCallback) {
        
        int current = Integer.parseInt(currentValue);
        
        // -10 button
        pane.addItem(createActionItem(Material.RED_CONCRETE, "&c-10", 
            "Click to subtract 10", () -> {
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, String.valueOf(current - 10), backCallback);
            }));
        
        // -1 button
        pane.addItem(createActionItem(Material.ORANGE_CONCRETE, "&c-1", 
            "Click to subtract 1", () -> {
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, String.valueOf(current - 1), backCallback);
            }));
        
        // Current value display
        pane.addItem(createDisplayItem(Material.GOLD_BLOCK, "&e" + definition.getName(), 
            "&7Current value: &f" + currentValue));
        
        // +1 button
        pane.addItem(createActionItem(Material.LIME_CONCRETE, "&a+1", 
            "Click to add 1", () -> {
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, String.valueOf(current + 1), backCallback);
            }));
        
        // +10 button
        pane.addItem(createActionItem(Material.GREEN_CONCRETE, "&a+10", 
            "Click to add 10", () -> {
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, String.valueOf(current + 10), backCallback);
            }));
        
        // Reset button
        pane.addItem(createActionItem(Material.BARRIER, "&cReset", 
            "Click to reset to default", () -> {
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, definition.getDefaultValue(), backCallback);
            }));
    }
    
    /**
     * Create edit items for string/UUID type
     */
    private void createStringEditItems(OutlinePane pane, Player viewer, UUID targetPlayerUuid, String targetPlayerName,
                                     InformationDefinition definition, String currentValue, Runnable backCallback) {
        
        // Current value display
        pane.addItem(createDisplayItem(Material.PAPER, "&e" + definition.getName(),
            "&7Current value: &f" + currentValue,
            "&7Type: &f" + definition.getType().name(),
            "&eUse chat to set new value"));
        
        // Set value button (opens chat input)
        pane.addItem(createActionItem(Material.WRITABLE_BOOK, "&eSet Value",
            "Click and type in chat", () -> {
                // Use chat input listener to prompt the viewer. Provide callbacks to reopen GUIs.
                chatInputListener.promptForInput(viewer, targetPlayerUuid, targetPlayerName, definition,
                    // onComplete -> reopen edit GUI with new value
                    newValue -> openEditGUI(viewer, targetPlayerUuid, targetPlayerName, definition, newValue, backCallback),
                    // onCancel -> reopen edit GUI with old value
                    () -> openEditGUI(viewer, targetPlayerUuid, targetPlayerName, definition, currentValue, backCallback)
                );
            }));
        
        // Reset button
        pane.addItem(createActionItem(Material.BARRIER, "&cReset",
            "Click to reset to default", () -> {
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, definition.getDefaultValue(), backCallback);
            }));
    }
    
    /**
     * Create edit items for enum type
     */
    private void createEnumEditItems(ChestGui gui, OutlinePane topPane, Player viewer,
                                     UUID targetPlayerUuid, String targetPlayerName,
                                     InformationDefinition definition, String currentValue,
                                     Runnable backCallback) {

        List<String> enumValues = definition.getEnumValues();
        if (enumValues == null || enumValues.isEmpty()) {
            return;
        }

        // --- TOP SECTION (info display) ---
        int currentIndex = enumValues.indexOf(currentValue);
        topPane.addItem(createDisplayItem(Material.COMPASS, "&e" + definition.getName(),
                "&7Current: &f" + currentValue,
                "&7Index: &f" + (currentIndex + 1) + "/" + enumValues.size(),
                "&7Values: &f" + String.join(", ", enumValues)));

        // Reset button
        topPane.addItem(createActionItem(Material.BARRIER, "&cReset",
                "Click to reset to default", () -> {
                    updateValue(viewer, targetPlayerUuid, targetPlayerName,
                            definition, definition.getDefaultValue(), backCallback);
                }));

        // --- BOTTOM SECTION (paginated list) ---
        PaginatedPane enumPane = new PaginatedPane(0, 2, 9, 1); // row 3 (0-indexed)
        int itemsPerPage = 7;

        int totalPages = (int) Math.ceil(enumValues.size() / (double) itemsPerPage);

        for (int page = 0; page < totalPages; page++) {
            OutlinePane pagePane = new OutlinePane(1, 0, 7, 1); // middle 7 slots

            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, enumValues.size());

            for (int i = start; i < end; i++) {
                String value = enumValues.get(i);
                boolean isCurrent = value.equalsIgnoreCase(currentValue);

                Material mat = isCurrent ? Material.EMERALD_BLOCK : Material.PAPER;
                String display = (isCurrent ? "&a" : "&f") + value;

                pagePane.addItem(createActionItem(mat, display,
                        isCurrent ? "Current value" : "Click to set this value", () -> {
                            updateValue(viewer, targetPlayerUuid, targetPlayerName,
                                    definition, value, backCallback);
                        }));
            }

            enumPane.addPane(page, pagePane);
        }

        // --- Add navigation buttons ---
        GuiItem previousPage = createActionItem(Material.ARROW, "&cPrevious Page", "Go to previous page", () -> {
            if (enumPane.getPage() > 0) {
                enumPane.setPage(enumPane.getPage() - 1);
                gui.update();
            }
        });

        GuiItem nextPage = createActionItem(Material.ARROW, "&aNext Page", "Go to next page", () -> {
            if (enumPane.getPage() < totalPages - 1) {
                enumPane.setPage(enumPane.getPage() + 1);
                gui.update();
            }
        });

        OutlinePane navPane = new OutlinePane(0, 2, 9, 1);
        navPane.addItem(previousPage);
        navPane.addItem(nextPage);

        gui.addPane(enumPane);
        gui.addPane(navPane);
    }

    /**
     * Create edit items for multi-enum type
     */
    private void createMultiEnumEditItems(ChestGui gui, OutlinePane topPane, Player viewer,
                                          UUID targetPlayerUuid, String targetPlayerName,
                                          InformationDefinition definition, String currentValue,
                                          Runnable backCallback) {

        List<String> multiEnumValues = definition.getMultiEnumValues();
        if (multiEnumValues == null || multiEnumValues.isEmpty()) {
            return;
        }

        List<String> selectedValues = definition.parseMultiEnumValue(currentValue);

        // --- TOP SECTION (info display) ---
        topPane.addItem(createDisplayItem(Material.ITEM_FRAME, "&e" + definition.getName(),
                "&7Selected: &f" + (selectedValues.isEmpty() ? "None" : String.join(", ", selectedValues)),
                "&7Count: &f" + selectedValues.size() + "/" + multiEnumValues.size(),
                "&7Options: &f" + String.join(", ", multiEnumValues)));

        // Clear all button
        topPane.addItem(createActionItem(Material.BARRIER, "&cClear All",
                "Click to deselect all", () -> {
                    updateValue(viewer, targetPlayerUuid, targetPlayerName,
                            definition, "", backCallback);
                }));

        // Select all button
        topPane.addItem(createActionItem(Material.EMERALD, "&aSelect All",
                "Click to select all", () -> {
                    String allSelected = definition.formatMultiEnumValue(multiEnumValues);
                    updateValue(viewer, targetPlayerUuid, targetPlayerName,
                            definition, allSelected, backCallback);
                }));

        // --- BOTTOM SECTION (paginated list) ---
        PaginatedPane enumPane = new PaginatedPane(0, 2, 9, 1);
        int itemsPerPage = 7;

        int totalPages = (int) Math.ceil(multiEnumValues.size() / (double) itemsPerPage);

        for (int page = 0; page < totalPages; page++) {
            OutlinePane pagePane = new OutlinePane(1, 0, 7, 1);

            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, multiEnumValues.size());

            for (int i = start; i < end; i++) {
                String value = multiEnumValues.get(i);
                boolean isSelected = selectedValues.contains(value);

                Material mat = isSelected ? Material.LIME_DYE : Material.GRAY_DYE;
                String display = (isSelected ? "&a✓ " : "&7") + value;

                pagePane.addItem(createActionItem(mat, display,
                        isSelected ? "Click to deselect" : "Click to select", () -> {
                            String newValue = definition.toggleMultiEnumValue(currentValue, value);
                            updateValue(viewer, targetPlayerUuid, targetPlayerName,
                                    definition, newValue, backCallback);
                        }));
            }

            enumPane.addPane(page, pagePane);
        }

        // --- Add navigation buttons ---
        GuiItem previousPage = createActionItem(Material.ARROW, "&cPrevious Page", "Go to previous page", () -> {
            if (enumPane.getPage() > 0) {
                enumPane.setPage(enumPane.getPage() - 1);
                gui.update();
            }
        });

        GuiItem nextPage = createActionItem(Material.ARROW, "&aNext Page", "Go to next page", () -> {
            if (enumPane.getPage() < totalPages - 1) {
                enumPane.setPage(enumPane.getPage() + 1);
                gui.update();
            }
        });

        OutlinePane navPane = new OutlinePane(0, 2, 9, 1);
        navPane.addItem(previousPage);
        navPane.addItem(nextPage);

        gui.addPane(enumPane);
        gui.addPane(navPane);
    }

    /**
     * Create edit items for ladder type
     */
    private void createLadderEditItems(OutlinePane pane, Player viewer, UUID targetPlayerUuid, String targetPlayerName,
                                     InformationDefinition definition, String currentValue, Runnable backCallback) {
        
        List<String> ladderValues = definition.getLadderValues();
        if (ladderValues == null || ladderValues.isEmpty()) {
            return;
        }
        
        int currentIndex = ladderValues.indexOf(currentValue);
        
        // Demote button
        pane.addItem(createActionItem(Material.RED_STAINED_GLASS, "&cDemote", 
            "Click to demote", () -> {
                String newValue = definition.getPreviousValue(currentValue);
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, newValue, backCallback);
            }));
        
        // Current value display
        pane.addItem(createDisplayItem(Material.LADDER, "&e" + definition.getName(), 
            "&7Current: &f" + currentValue,
            "&7Rank: &f" + (currentIndex + 1) + "/" + ladderValues.size(),
            "&7Ladder: &f" + String.join(" → ", ladderValues)));
        
        // Promote button
        pane.addItem(createActionItem(Material.GREEN_STAINED_GLASS, "&aPromote", 
            "Click to promote", () -> {
                String newValue = definition.getNextValue(currentValue);
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, newValue, backCallback);
            }));
        
        // Reset button
        pane.addItem(createActionItem(Material.BARRIER, "&cReset", 
            "Click to reset to default", () -> {
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, definition.getDefaultValue(), backCallback);
            }));
    }
    
    /**
     * Create edit items for permission type
     */
    private void createPermissionEditItems(OutlinePane pane, Player viewer, UUID targetPlayerUuid, String targetPlayerName,
                                         InformationDefinition definition, String currentValue, Runnable backCallback) {
        
        boolean isTrue = Boolean.parseBoolean(currentValue);
        
        // False button
        pane.addItem(createActionItem(Material.RED_WOOL, "&cFalse", 
            "Click to set to false", () -> {
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, "false", backCallback);
            }));
        
        // Current value display
        pane.addItem(createDisplayItem(Material.REDSTONE_TORCH, "&e" + definition.getName(), 
            "&7Current: &f" + currentValue,
            "&7Permission: &f" + definition.getPermissionNode()));
        
        // True button
        pane.addItem(createActionItem(Material.GREEN_WOOL, "&aTrue", 
            "Click to set to true", () -> {
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, "true", backCallback);
            }));
        
        // Reset button
        pane.addItem(createActionItem(Material.BARRIER, "&cReset", 
            "Click to reset to default", () -> {
                updateValue(viewer, targetPlayerUuid, targetPlayerName, definition, definition.getDefaultValue(), backCallback);
            }));
    }
    
    /**
     * Add back button to the GUI
     */
    private void addBackButton(OutlinePane pane, Runnable backCallback) {
        pane.addItem(createActionItem(Material.ARROW, "&7← Back", 
            "Click to go back", () -> {
                backCallback.run();
            }));
    }
    
    /**
     * Create an action item
     */
    private GuiItem createActionItem(Material material, String name, String description, Runnable action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7" + description));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return new GuiItem(item, event -> {
            event.setCancelled(true);
            action.run();
        });
    }
    
    /**
     * Create a display item
     */
    private GuiItem createDisplayItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(colorize(line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return new GuiItem(item, event -> event.setCancelled(true));
    }
    
    /**
     * Update a value and refresh the GUI
     */
    private void updateValue(Player viewer, UUID targetPlayerUuid, String targetPlayerName, 
                           InformationDefinition definition, String newValue, Runnable backCallback) {
        
        dataRepository.setPlayerInformation(targetPlayerUuid, targetPlayerName, definition.getName(), newValue)
            .thenAccept(success -> {
                if (success) {
                    viewer.sendMessage(colorize("&aUpdated " + definition.getName() + " to " + newValue));
                    // Refresh the edit GUI
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        openEditGUI(viewer, targetPlayerUuid, targetPlayerName, definition, newValue, backCallback);
                    });
                } else {
                    viewer.sendMessage(colorize("&cFailed to update " + definition.getName()));
                }
            });
    }
    
    /**
     * Convert color codes to Minecraft colors
     */
    private String colorize(String text) {
        return text.replace('&', '§');
    }
}
