package com.Zrips.CMI.events;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CMIPlayerMuteEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public CommandSender getMutedBy() { return null; }
    public Player getMutedPlayer() { return null; }
    public String getReason() { return ""; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
