package gg.gianluca.gianchat.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    private String format;
    private boolean messagesEnabled;
    private boolean socialSpyEnabled;
    private boolean mentionsEnabled;
    private UUID lastMessager;
    private Set<UUID> ignoredPlayers;

    public PlayerData() {
        this.format = null;
        this.messagesEnabled = true;
        this.socialSpyEnabled = false;
        this.mentionsEnabled = true;
        this.lastMessager = null;
        this.ignoredPlayers = new HashSet<>();
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isMessagesEnabled() {
        return messagesEnabled;
    }

    public void setMessagesEnabled(boolean enabled) {
        this.messagesEnabled = enabled;
    }

    public boolean isSocialSpyEnabled() {
        return socialSpyEnabled;
    }

    public void setSocialSpyEnabled(boolean enabled) {
        this.socialSpyEnabled = enabled;
    }

    public boolean hasMentionsEnabled() {
        return mentionsEnabled;
    }

    public void setMentionsEnabled(boolean enabled) {
        this.mentionsEnabled = enabled;
    }

    public UUID getLastMessager() {
        return lastMessager;
    }

    public void setLastMessager(UUID lastMessager) {
        this.lastMessager = lastMessager;
    }

    public Set<UUID> getIgnoredPlayers() {
        return ignoredPlayers;
    }

    public void setIgnoredPlayers(Set<UUID> ignoredPlayers) {
        this.ignoredPlayers = ignoredPlayers;
    }
} 