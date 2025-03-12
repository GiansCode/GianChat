package gg.gianluca.gianchat.events;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GianChatPlayerMentionEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final Player mentioner;
    private final Player mentioned;
    private Component mentionComponent;
    private boolean playSound;
    private boolean showTitle;
    private boolean showActionBar;

    public GianChatPlayerMentionEvent(Player mentioner, Player mentioned, Component mentionComponent,
                                    boolean playSound, boolean showTitle, boolean showActionBar) {
        this.mentioner = mentioner;
        this.mentioned = mentioned;
        this.mentionComponent = mentionComponent;
        this.playSound = playSound;
        this.showTitle = showTitle;
        this.showActionBar = showActionBar;
    }

    public Player getMentioner() {
        return mentioner;
    }

    public Player getMentioned() {
        return mentioned;
    }

    public Component getMentionComponent() {
        return mentionComponent;
    }

    public void setMentionComponent(Component mentionComponent) {
        this.mentionComponent = mentionComponent;
    }

    public boolean isPlaySound() {
        return playSound;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    public boolean isShowActionBar() {
        return showActionBar;
    }

    public void setShowActionBar(boolean showActionBar) {
        this.showActionBar = showActionBar;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
} 