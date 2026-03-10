package me.confuser.banmanager.prebukkitevents;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerBannedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public CommandSender getActor() { return null; }
    public Player getTarget() { return null; }
    public String getReason() { return ""; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
