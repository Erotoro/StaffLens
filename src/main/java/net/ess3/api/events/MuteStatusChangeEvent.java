package net.ess3.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MuteStatusChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public boolean getValue() { return true; }
    public Player getController() { return null; }
    public Player getAffected() { return null; }
    public String getReason() { return ""; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
