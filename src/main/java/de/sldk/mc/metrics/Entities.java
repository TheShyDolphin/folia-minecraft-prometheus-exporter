package de.sldk.mc.metrics;

import io.prometheus.client.Gauge;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;

/**
 * Get current count of all entities.
 *
 * Entities are labelled by
 * <ol>
 *     <li> world,
 *     <li> type ({@link EntityType}),
 *     <li> alive ({@link EntityType#isAlive()}),
 *     <li> and spawnable ({@link EntityType#isSpawnable()})
 * </ol>
 */
public class Entities extends WorldMetric {

    private static final Gauge ENTITIES = Gauge.build()
            .name(prefix("entities_total"))
            .help("Entities loaded per world")
            .labelNames("world", "type", "alive", "spawnable")
            .create();

    /**
     * Override the value returned by {@link EntityType#isAlive()}.
     */
    private static final Map<EntityType, Boolean> ALIVE_OVERRIDE = singletonMap(EntityType.ARMOR_STAND, false);

    public Entities(Plugin plugin) {
        super(plugin, ENTITIES);
    }

    @Override
    protected void clear() {
        ENTITIES.clear();
    }

    @Override
    public void collect(World world) {
        CompletableFuture<List<EntityType>> worldEntities = new CompletableFuture<>();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> worldEntities.complete(world.getEntities()
                .stream().map(Entity::getType).collect(Collectors.toList())));
        Map<EntityType, Long> mapEntityTypesToCounts = worldEntities.join().stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        mapEntityTypesToCounts
                .forEach((entityType, count) ->
                        ENTITIES
                                .labels(world.getName(),
                                        getEntityName(entityType),
                                        Boolean.toString(isEntityTypeAlive(entityType)),
                                        Boolean.toString(entityType.isSpawnable()))
                                .set(count)
                );
    }

    private boolean isEntityTypeAlive(EntityType type) {
        return ALIVE_OVERRIDE.containsKey(type) ? ALIVE_OVERRIDE.get(type) : type.isAlive();
    }
}
