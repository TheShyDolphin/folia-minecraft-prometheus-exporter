package de.sldk.mc.metrics;

import de.sldk.mc.tps.TpsCollector;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.prometheus.client.Gauge;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class Tps extends Metric {

    private static final Gauge TPS = Gauge.build()
            .name(prefix("tps"))
            .help("Server TPS (ticks per second)")
            .create();

    private ScheduledTask taskId;

    private final TpsCollector tpsCollector = new TpsCollector();

    public Tps(Plugin plugin) {
        super(plugin, TPS);
    }

    @Override
    public void enable() {
        super.enable();
        this.taskId = startTask(getPlugin());
    }

    @Override
    public void disable() {
        super.disable();
        taskId.cancel();
    }

    private ScheduledTask startTask(Plugin plugin) {
        return Bukkit.getServer()
                .getGlobalRegionScheduler()
                .runAtFixedRate(plugin, tpsCollector, 1, TpsCollector.POLL_INTERVAL);
    }

    @Override
    public void doCollect() {
        TPS.set(tpsCollector.getAverageTPS());
    }
}
