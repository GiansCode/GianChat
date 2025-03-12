package gg.gianluca.gianchat.format;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class ChatFormat {
    private final String name;
    private final int priority;
    private final ComponentSection prefix;
    private final ComponentSection playerName;
    private final ComponentSection separator;
    private final ComponentSection message;

    public ChatFormat(String name, ConfigurationSection config) {
        this.name = name;
        this.priority = config.getInt("priority", 1);
        this.prefix = new ComponentSection(config.getConfigurationSection("prefix"));
        this.playerName = new ComponentSection(config.getConfigurationSection("name"));
        this.separator = new ComponentSection(config.getConfigurationSection("separator"));
        this.message = new ComponentSection(config.getConfigurationSection("message"));
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public Component getPrefix() {
        return prefix.build();
    }

    public Component getNameFormat() {
        return playerName.build();
    }

    public Component getSeparator() {
        return separator.build();
    }

    public ClickEvent getNameClickEvent() {
        return playerName.clickEvent != null ? 
            ClickEvent.clickEvent(ClickEvent.Action.valueOf(playerName.clickEvent.type()), playerName.clickEvent.command()) : 
            null;
    }

    public ClickEvent getMessageClickEvent() {
        return message.clickEvent != null ? 
            ClickEvent.clickEvent(ClickEvent.Action.valueOf(message.clickEvent.type()), message.clickEvent.command()) : 
            null;
    }

    public Component buildComponent(TagResolver... placeholders) {
        return Component.empty()
                .append(prefix.build(placeholders))
                .append(playerName.build(placeholders))
                .append(separator.build(placeholders))
                .append(message.build(placeholders));
    }

    private static class ComponentSection {
        private final String value;
        private final List<String> tooltip;
        private final ClickEventRecord clickEvent;

        public ComponentSection(ConfigurationSection section) {
            if (section == null) {
                this.value = "";
                this.tooltip = List.of();
                this.clickEvent = null;
                return;
            }

            this.value = section.getString("value", "");
            this.tooltip = section.getStringList("tooltip");
            
            ConfigurationSection clickSection = section.getConfigurationSection("click_event");
            if (clickSection != null) {
                this.clickEvent = new ClickEventRecord(
                    clickSection.getString("type", "SUGGEST_COMMAND"),
                    clickSection.getString("command", "")
                );
            } else {
                this.clickEvent = null;
            }
        }

        public Component build(TagResolver... placeholders) {
            Component component = MiniMessage.miniMessage().deserialize(value, placeholders);
            
            if (!tooltip.isEmpty()) {
                Component tooltipComponent = Component.empty();
                for (int i = 0; i < tooltip.size(); i++) {
                    tooltipComponent = tooltipComponent.append(
                        MiniMessage.miniMessage().deserialize(tooltip.get(i), placeholders)
                    );
                    if (i < tooltip.size() - 1) {
                        tooltipComponent = tooltipComponent.append(Component.newline());
                    }
                }
                component = component.hoverEvent(tooltipComponent);
            }

            if (clickEvent != null) {
                component = component.clickEvent(ClickEvent.clickEvent(
                    ClickEvent.Action.valueOf(clickEvent.type()),
                    clickEvent.command()
                ));
            }

            return component;
        }
    }

    private record ClickEventRecord(String type, String command) {}
} 