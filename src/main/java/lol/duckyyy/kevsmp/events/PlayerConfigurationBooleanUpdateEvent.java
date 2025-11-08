package lol.duckyyy.kevsmp.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerConfigurationBooleanUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean newValue;
    private String path;

    public PlayerConfigurationBooleanUpdateEvent(String path, boolean currentValue) {
        this.path = path;
        this.newValue = !currentValue;
    }

    public String getPath(){
        return this.path;
    }

    public boolean getNewValue() {
        return this.newValue;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
