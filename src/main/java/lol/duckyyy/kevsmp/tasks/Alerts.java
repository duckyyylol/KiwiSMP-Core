package lol.duckyyy.kevsmp.tasks;

import lol.duckyyy.kevsmp.KevSmp;

public class Alerts implements Runnable {
    private static KevSmp plugin;
    public Alerts(KevSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.performTPSCheck();
    }
}
