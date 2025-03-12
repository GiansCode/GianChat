package gg.gianluca.gianchat.events;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GianChatPrivateMessageEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final Player sender;
    private final Player recipient;
    private Component senderMessage;
    private Component recipientMessage;
    private Component socialSpyMessage;
    private boolean playSoundToRecipient;

    public GianChatPrivateMessageEvent(Player sender, Player recipient, Component senderMessage, 
                                     Component recipientMessage, Component socialSpyMessage, 
                                     boolean playSoundToRecipient) {
        this.sender = sender;
        this.recipient = recipient;
        this.senderMessage = senderMessage;
        this.recipientMessage = recipientMessage;
        this.socialSpyMessage = socialSpyMessage;
        this.playSoundToRecipient = playSoundToRecipient;
    }

    public Player getSender() {
        return sender;
    }

    public Player getRecipient() {
        return recipient;
    }

    public Component getSenderMessage() {
        return senderMessage;
    }

    public void setSenderMessage(Component senderMessage) {
        this.senderMessage = senderMessage;
    }

    public Component getRecipientMessage() {
        return recipientMessage;
    }

    public void setRecipientMessage(Component recipientMessage) {
        this.recipientMessage = recipientMessage;
    }

    public Component getSocialSpyMessage() {
        return socialSpyMessage;
    }

    public void setSocialSpyMessage(Component socialSpyMessage) {
        this.socialSpyMessage = socialSpyMessage;
    }

    public boolean shouldPlaySoundToRecipient() {
        return playSoundToRecipient;
    }

    public void setPlaySoundToRecipient(boolean playSoundToRecipient) {
        this.playSoundToRecipient = playSoundToRecipient;
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