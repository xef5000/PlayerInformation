// ...new file...
package ca.xef5000.playerinformation.listeners;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.database.PlayerDataRepository;
import ca.xef5000.playerinformation.utils.MessageUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Listener that handles one-time string inputs from players via chat.
 */
public class ChatInputListener implements Listener {
    private final PlayerInformation plugin;
    private final PlayerDataRepository dataRepository;

    private static class PendingInput {
        final UUID targetUuid;
        final String targetName;
        final InformationDefinition definition;
        final Consumer<String> onComplete;
        final Runnable onCancel;

        PendingInput(UUID targetUuid, String targetName, InformationDefinition definition,
                     Consumer<String> onComplete, Runnable onCancel) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.definition = definition;
            this.onComplete = onComplete;
            this.onCancel = onCancel;
        }
    }

    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputListener(PlayerInformation plugin, PlayerDataRepository dataRepository) {
        this.plugin = plugin;
        this.dataRepository = dataRepository;
    }

    /**
     * Prompt a player to provide a value via chat.
     * The onComplete consumer will be called (synchronously on the server thread) after the value has been saved.
     */
    public void promptForInput(Player viewer, UUID targetUuid, String targetName,
                               InformationDefinition definition, Consumer<String> onComplete, Runnable onCancel) {
        UUID viewerUuid = viewer.getUniqueId();
        pendingInputs.put(viewerUuid, new PendingInput(targetUuid, targetName, definition, onComplete, onCancel));

        // Close inventory to allow chat, and send instructions
        viewer.closeInventory();
        MessageUtils.sendMessage(viewer, "&eType the new value for &f" + definition.getName() + "&e in chat. Type &ccancel &eto cancel.");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PendingInput pending = pendingInputs.get(uuid);
        if (pending == null) return;

        // We are handling this input - cancel the chat message
        event.setCancelled(true);

        String message = event.getMessage();

        // Handle cancel
        if (message.equalsIgnoreCase("cancel")) {
            pendingInputs.remove(uuid);
            MessageUtils.sendMessage(player, "&cInput cancelled.");
            if (pending.onCancel != null) {
                // run on server thread
                plugin.getServer().getScheduler().runTask(plugin, pending.onCancel);
            }
            return;
        }

        // Validate value
        if (!pending.definition.isValidValue(message)) {
            MessageUtils.sendMessage(player, "&cInvalid value for &f" + pending.definition.getName() + "&c. Please try again or type &ccancel&c.");
            return; // keep pending so the player can try again
        }

        // Save value asynchronously via repository
        dataRepository.setPlayerInformation(pending.targetUuid, pending.targetName, pending.definition.getName(), message)
            .thenAccept(success -> {
                pendingInputs.remove(uuid);

                if (success) {
                    MessageUtils.sendMessage(player, "&aUpdated " + pending.definition.getName() + " to &f" + message);
                    // Re-open the edit GUI on the main thread via the onComplete consumer
                    if (pending.onComplete != null) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> pending.onComplete.accept(message));
                    }
                } else {
                    MessageUtils.sendMessage(player, "&cFailed to update " + pending.definition.getName());
                    // call cancel handler to reopen previous GUI if provided
                    if (pending.onCancel != null) {
                        plugin.getServer().getScheduler().runTask(plugin, pending.onCancel);
                    }
                }
            });
    }
}

