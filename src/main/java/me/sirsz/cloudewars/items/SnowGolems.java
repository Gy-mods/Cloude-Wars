package me.sirsz.cloudewars.items;

import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.GamePlayerView;
import com.l299l.newbedwars.api.view.TeamView;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.sirsz.cloudewars.CloudeWars;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

/**
 * The shop's snow golem, like the snowman of Hypixel SkyWars' Snowman kit: it walks faster than a normal snow
 * golem, throws snowballs at enemy players several times faster (each one pushes them back), and when
 * it falls into the void it comes back next to its owner with the same health. It never hits its own team, its
 * team cannot hurt it, it leaves no snow trail and lives until it is killed or the game ends.
 */
public final class SnowGolems implements Listener {

    /** How far it looks for enemies, and how close they must be for a snowball (a normal snow golem's range). */
    private static final double TARGET_RANGE = 16;
    private static final double SHOOT_RANGE = 10;
    private static final int RETARGET_TICKS = 10;
    /** Without a quick void height: how far it may fall over nothing (no block below it) before it comes back. */
    private static final double VOID_FALL = 3;

    private final JavaPlugin plugin;
    private final NamespacedKey ownerKey;
    private final NamespacedKey teamKey;
    private final double speedMultiplier;
    private final double knockback;
    /** Ticks between the extra snowballs on top of the normal golem's one per second (0 = no extra ones). */
    private final int extraShotTicks;

    public SnowGolems(JavaPlugin plugin, YamlConfiguration config) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "snow_golem_owner");
        this.teamKey = new NamespacedKey(plugin, "snow_golem_team");
        this.speedMultiplier = config.getDouble("snow-golem.speed");
        this.knockback = config.getDouble("snow-golem.knockback");
        int extraPerSecond = config.getInt("snow-golem.snowballs-per-second") - 1;
        this.extraShotTicks = extraPerSecond > 0 ? Math.max(1, Math.round(20f / extraPerSecond)) : 0;
    }

    /** Spawns the player's snow golem at their feet; false if they are in no team. */
    public boolean spawn(Player owner, ArenaView arena) {
        TeamView team = team(arena, owner);
        if (team == null) {
            return false;
        }
        Snowman golem = owner.getWorld().spawn(owner.getLocation(), Snowman.class, spawned -> {
            spawned.customName(LegacyComponentSerializer.legacySection()
                    .deserialize(team.getColor() + displayName(team) + " Snow Golem"));
            spawned.setCustomNameVisible(true);
            spawned.setRemoveWhenFarAway(false);
            spawned.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            spawned.getPersistentDataContainer().set(teamKey, PersistentDataType.STRING, arena.getName() + ":" + team.getName());
            AttributeInstance speed = spawned.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * speedMultiplier);
            }
        });
        new GolemTask(golem, arena.getName(), team.getName(), owner.getUniqueId()).runTaskTimer(plugin, 1, 1);
        return true;
    }

    /** Runs every tick for one golem: void rescue, picking enemies and the extra snowballs. */
    private final class GolemTask extends BukkitRunnable {
        private final Snowman golem;
        private final String arenaName;
        private final String teamName;
        private final UUID ownerId;
        /** The height it last stood at. */
        private double groundY;
        private int ticks;
        /** This tick's arena (New Bedwars hands out fresh views; old ones die with a /bw reload). */
        private @Nullable ArenaView arena;

        GolemTask(Snowman golem, String arenaName, String teamName, UUID ownerId) {
            this.golem = golem;
            this.arenaName = arenaName;
            this.teamName = teamName;
            this.ownerId = ownerId;
            this.groundY = golem.getY();
        }

        @Override
        public void run() {
            arena = CloudeWars.api().getArena(arenaName).orElse(null);
            if (!golem.isValid() || arena == null || arena.getState() != ArenaState.PLAYING) {
                golem.remove();
                cancel();
                return;
            }
            if (golem.isOnGround()) {
                groundY = golem.getY();
            } else if (fellIntoVoid()) {
                bringBack();
                return;
            }
            ticks++;
            if (ticks % RETARGET_TICKS == 0) {
                golem.setTarget(nearestEnemy());
            }
            if (extraShotTicks > 0 && ticks % extraShotTicks == 0) {
                shoot();
            }
        }

        /**
         * Whether it fell below the arena's quick void height, or (for arenas without one) is falling with no block
         * below it.
         */
        private boolean fellIntoVoid() {
            Integer quickVoidY = arena.getQuickVoidY();
            if (quickVoidY != null) {
                return golem.getY() < quickVoidY;
            }
            if (golem.getY() < golem.getWorld().getMinHeight()) {
                return true;
            }
            int highest = golem.getWorld().getHighestBlockYAt(golem.getLocation().getBlockX(), golem.getLocation().getBlockZ(),
                    HeightMap.WORLD_SURFACE);
            return golem.getY() < groundY - VOID_FALL && highest < golem.getWorld().getMinHeight();
        }

        /** Back next to the owner (or to the team spawn while the owner is dead), keeping its health. */
        private void bringBack() {
            Player owner = Bukkit.getPlayer(ownerId);
            TeamView team = arena.getTeam(teamName).orElse(null);
            boolean ownerPlaying = owner != null && team(arena, owner) != null && owner.getGameMode() == GameMode.SURVIVAL
                    && owner.getWorld().equals(golem.getWorld());
            Location to = ownerPlaying ? owner.getLocation() : team == null ? null : team.getSpawn();
            if (to == null) {
                golem.remove();
                return;
            }
            golem.setVelocity(new Vector());
            golem.setFallDistance(0);
            golem.teleport(to);
            groundY = to.getY();
        }

        private @Nullable Player nearestEnemy() {
            Player nearest = null;
            double nearestDistance = TARGET_RANGE * TARGET_RANGE;
            for (TeamView other : arena.getTeams()) {
                if (other.getName().equals(teamName)) {
                    continue;
                }
                for (Player enemy : other.getPlayers()) {
                    if (!canTarget(enemy)) {
                        continue;
                    }
                    double distance = enemy.getLocation().distanceSquared(golem.getLocation());
                    if (distance < nearestDistance && golem.hasLineOfSight(enemy)) {
                        nearest = enemy;
                        nearestDistance = distance;
                    }
                }
            }
            return nearest;
        }

        private boolean canTarget(Player enemy) {
            return enemy.isOnline() && enemy.getWorld().equals(golem.getWorld()) && enemy.getGameMode() == GameMode.SURVIVAL
                    && !arena.getSpectators().contains(enemy);
        }

        /** A snowball at the target, aimed like a normal snow golem's. */
        private void shoot() {
            if (!(golem.getTarget() instanceof Player target) || !canTarget(target)
                    || target.getLocation().distanceSquared(golem.getLocation()) > SHOOT_RANGE * SHOOT_RANGE
                    || !golem.hasLineOfSight(target)) {
                return;
            }
            Snowball snowball = golem.launchProjectile(Snowball.class);
            double dx = target.getX() - golem.getX();
            double dz = target.getZ() - golem.getZ();
            double dy = target.getEyeLocation().getY() - 1.1 - snowball.getY();
            double arc = Math.sqrt(dx * dx + dz * dz) * 0.2;
            snowball.setVelocity(new Vector(dx, dy + arc, dz).normalize().multiply(1.6));
            golem.getWorld().playSound(golem.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, 1f,
                    0.4f / (ThreadLocalRandom.current().nextFloat() * 0.4f + 0.8f));
        }
    }

    /**
     * Its snowballs fly through its own team and push enemies back like a hit (today's Minecraft snowballs do not move
     * players, the 1.8 ones of Hypixel did).
     */
    @EventHandler(ignoreCancelled = true)
    public void onSnowballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball) || !(snowball.getShooter() instanceof Snowman golem)
                || !isOurs(golem) || !(event.getHitEntity() instanceof Player victim)) {
            return;
        }
        if (sameTeam(golem, victim)) {
            event.setCancelled(true);
            return;
        }
        if (knockback <= 0 || victim.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        Vector flight = snowball.getVelocity();
        victim.knockback(knockback, -flight.getX(), -flight.getZ());
        victim.setVelocity(victim.getVelocity()); // knockback() alone does not send the push to the player's client
        victim.playHurtAnimation((float) Math.toDegrees(Math.atan2(-flight.getZ(), -flight.getX())) - victim.getYaw() - 90);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1f);
    }

    /** Its owner and team cannot hurt it. */
    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Snowman golem && isOurs(golem)) {
            Player attacker = attacker(event.getDamager());
            if (attacker != null && sameTeam(golem, attacker)) {
                event.setCancelled(true);
            }
        }
    }

    /** No void damage (the golem task brings it back), and no melting in rain, water or warm places. */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Snowman golem) || !isOurs(golem)) {
            return;
        }
        switch (event.getCause()) {
            case VOID, MELTING, DROWNING -> event.setCancelled(true);
            default -> { }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Snowman golem && isOurs(golem)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    /** No snow trail on the map. */
    @EventHandler(ignoreCancelled = true)
    public void onSnowTrail(EntityBlockFormEvent event) {
        if (event.getEntity() instanceof Snowman golem && isOurs(golem)) {
            event.setCancelled(true);
        }
    }

    /** It only goes after enemy players (a normal snow golem would hunt monsters, e.g. silverfish). */
    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Snowman golem) || !isOurs(golem) || event.getTarget() == null) {
            return;
        }
        if (!(event.getTarget() instanceof Player target) || sameTeam(golem, target)) {
            event.setCancelled(true);
        }
    }

    private boolean isOurs(Entity entity) {
        return entity.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING);
    }

    /** Whether the player is in the golem's team (an unknown team counts as the same, so nothing gets hurt). */
    private boolean sameTeam(Snowman golem, Player player) {
        String key = golem.getPersistentDataContainer().get(teamKey, PersistentDataType.STRING);
        int colon = key == null ? -1 : key.indexOf(':');
        ArenaView arena = colon < 0 ? null : CloudeWars.api().getArena(key.substring(0, colon)).orElse(null);
        if (arena == null || arena.getTeam(key.substring(colon + 1)).isEmpty()) {
            return true;
        }
        TeamView playerTeam = team(arena, player);
        return playerTeam != null && playerTeam.getName().equals(key.substring(colon + 1));
    }

    /** The team name players see: New Bedwars 2.8 has display names (random team colors set them). */
    private static String displayName(TeamView team) {
        return CloudeWars.newBedwarsApiVersion() >= 1 ? team.getDisplayName() : team.getName();
    }

    private static @Nullable TeamView team(ArenaView arena, Player player) {
        return arena.getGamePlayer(player.getUniqueId()).flatMap(GamePlayerView::getTeam).orElse(null);
    }

    private static @Nullable Player attacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
