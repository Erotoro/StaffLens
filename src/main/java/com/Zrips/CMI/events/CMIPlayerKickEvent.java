package com.Zrips.CMI.events;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CMIPlayerKickEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public CommandSender getKickedBy() { return null; }
    public Player getKickedPlayer() { return null; }
    public String getReason() { return ""; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
