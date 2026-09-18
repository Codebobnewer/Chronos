package me.goga221.chronos.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads player-facing message templates from {@code messages.yml} and
 * renders them as MiniMessage components.
 * <p>
 * No player-facing text lives anywhere else in this plugin; commands and
 * GUIs only reference {@link MessageKey} values.
 */
public final class MessageService {

    private final MiniMessage miniMessage;
    private final Map<String, String> messages;
    private final String prefix;

    public MessageService(final JavaPlugin plugin) {
        this.miniMessage = MiniMessage.miniMessage();
        final FileConfiguration raw = loadMessagesFile(plugin);
        this.prefix = raw.getString("prefix", "");
        this.messages = new HashMap<>();
        for (final MessageKey key : MessageKey.values()) {
            messages.put(key.configKey(), raw.getString(key.configKey(), key.configKey()));
        }
    }

    /**
     * Sends the rendered message for {@code key} to {@code target}, with the configured prefix applied.
     */
    public void send(final CommandSender target, final MessageKey key, final TagResolver... placeholders) {
        target.sendMessage(render(key, placeholders));
    }

    /**
     * Renders the message for {@code key}, with the configured prefix applied.
     */
    public Component render(final MessageKey key, final TagResolver... placeholders) {
        return renderTemplate(prefix + messages.get(key.configKey()), placeholders);
    }

    /**
     * Renders the message for {@code key} with no prefix — for UI elements
     * such as GUI titles and item names, where a chat prefix wouldn't fit.
     */
    public Component renderPlain(final MessageKey key, final TagResolver... placeholders) {
        return renderTemplate(messages.get(key.configKey()), placeholders);
    }

    private Component renderTemplate(final String template, final TagResolver... placeholders) {
        return miniMessage.deserialize(template, placeholders);
    }

    private static FileConfiguration loadMessagesFile(final JavaPlugin plugin) {
        final File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
