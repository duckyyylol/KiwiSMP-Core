package lol.duckyyy.kevsmp.tasks;

import lol.duckyyy.kevsmp.KevSmp;

public class AFKTimer implements Runnable {
    private final KevSmp plugin;
    public AFKTimer(KevSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getServer().getOnlinePlayers().forEach(plugin::updateAfkTime);
    }
}
