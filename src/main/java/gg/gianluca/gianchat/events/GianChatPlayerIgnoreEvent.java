package gg.gianluca.gianchat.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GianChatPlayerIgnoreEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Player target;
    private final boolean ignore;
    private boolean cancelled;

    public GianChatPlayerIgnoreEvent(Player player, Player target, boolean ignore) {
        this.player = player;
        this.target = target;
        this.ignore = ignore;
        this.cancelled = false;
    }

    /**
     * Gets the player who is ignoring/unignoring someone.
     *
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the player being ignored/unignored.
     *
     * @return The target player
     */
    public Player getTarget() {
        return target;
    }

    /**
     * Gets whether this is an ignore (true) or unignore (false) action.
     *
     * @return true if ignoring, false if unignoring
     */
    public boolean isIgnore() {
        return ignore;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
} 