package com.Zrips.CMI.events;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CMIPlayerBanEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public CommandSender getBannedBy() { return null; }
    public Player getBannedPlayer() { return null; }
    public String getReason() { return ""; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
