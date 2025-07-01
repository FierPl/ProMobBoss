package com.wattbreak.promobboss.mob;

import com.wattbreak.promobboss.ProMobBoss;
import com.wattbreak.promobboss.common.VanillaMobSettings;
import com.wattbreak.promobboss.gui.menus.mob.EquipmentSelectorMenu;
import com.wattbreak.promobboss.utility.ChatHelper;
import com.wattbreak.promobboss.utility.NBTUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MobSpawner {

    private final ProMobBoss plugin;
    private final MobManager mobManager;

    private final Map<String, Set<UUID>> activeCustomMobs = new ConcurrentHashMap<>();
    private BukkitRunnable spawnerTask;

    public MobSpawner(ProMobBoss plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        startSpawnerTask();
    }

    /**
     * Starts or restarts the automatic spawn mission.
     */
    public void startSpawnerTask() {
        if (spawnerTask != null) {
            spawnerTask.cancel();
        }

        long interval = plugin.getConfigManager().getSettingsConfig().getLong("mob-spawner.spawn-interval-seconds", 60) * 20L;

        spawnerTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    processSpawning();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "An error occurred in the MobSpawner task.", e);
                }
            }
        };
        spawnerTask.runTaskTimerAsynchronously(plugin, 200L, interval);
        plugin.getLogger().info("MobSpawner task started with an interval of " + (interval / 20) + " seconds.");
    }

    /**
     * It goes through all mob configurations and tries to spawn.
     * This method should be executed in the main thread (because it contains Entity API calls).
     */
    private void processSpawning() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Mob mob : mobManager.getAllMobs()) {
                    if (!mob.isEnabled() || mob.getSpawnWorld() == null) continue;

                    World world = Bukkit.getWorld(mob.getSpawnWorld());
                    if (world == null || world.getPlayers().isEmpty()) {
                        continue;
                    }

                    int currentAmount = getActiveMobCount(mob.getConfigName());
                    int neededAmount = mob.getMaxAlive() - currentAmount;

                    if (neededAmount <= 0) {
                        continue;
                    }

                    for (int i = 0; i < neededAmount; i++) {
                        spawnMobInWorld(mob, world);
                    }
                }
            }
        }.runTask(plugin);
    }

    /**
     * Starts the process of spawning a mob near a random player in the specified world.
     * @param mob Mob object to be spawned.
     * @param world The world where Mob will spawn.
     */
    private void spawnMobInWorld(Mob mob, World world) {
        List<Player> playersInWorld = world.getPlayers();
        if (playersInWorld.isEmpty()) return;

        Player randomPlayer = playersInWorld.get(new Random().nextInt(playersInWorld.size()));

        findSafeLocation(randomPlayer.getLocation(), 20, 64).ifPresent(safeLocation -> {
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnSingleMob(mob, safeLocation);
                }
            }.runTask(plugin);
        });
    }

    /**
     * Instantly spawns a single custom mob at a specified location.
     * This method is used by command, egg or automatic spawner.
     * All NBT tagging and saving as active mob is done here.
     * @param mob Mob object to be spawned.
     * @param location Spawn location.
     */
    public void spawnSingleMob(Mob mob, Location location) {
        Entity spawnedEntity = null;

        if ("MYTHIC_MOBS".equalsIgnoreCase(mob.getMobTypeSource())) {
            if (plugin.getMythicMobsHandler() != null && plugin.getMythicMobsHandler().isReady()) {
                spawnedEntity = plugin.getMythicMobsHandler().spawnMythicMob(mob.getMythicMobName(), location);
                if (spawnedEntity == null) {
                    plugin.getLogger().warning("Failed to spawn MythicMob: " + mob.getMythicMobName() + " for mob " + mob.getConfigName());
                }
            } else {
                plugin.getLogger().warning("Mob " + mob.getConfigName() + " is set to MythicMobs, but the integration is not ready.");
            }
        } else { // VANILLA_CUSTOM
            spawnedEntity = spawnVanillaMobEntity(mob, location);
        }

        if (spawnedEntity != null) {
            NBTUtil.setString(spawnedEntity, "promb-mob-id", mob.getConfigName());
            registerActiveMob(mob, spawnedEntity);
            plugin.getLogger().info("Successfully spawned mob '" + mob.getConfigName() + "' at " +
                    location.getWorld().getName() + " X:" + (int)location.getX() + " Y:" + (int)location.getY() + " Z:" + (int)location.getZ());
        } else {
            plugin.getLogger().warning("Failed to spawn custom mob: " + mob.getConfigName());
        }
    }

    /**
     * Spawns a special vanilla creature according to the settings in the mob object.
     * @param mob Mob object containing its settings.
     * @param loc Location where to spawn.
     * @return Spawned LivingEntity or null.
     */
    private LivingEntity spawnVanillaMobEntity(Mob mob, Location loc) {
        if (loc.getWorld() == null) return null;

        VanillaMobSettings settings = mob.getVanillaMobSettings();
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(settings.getEntityType().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid entity type '" + settings.getEntityType() + "' for mob " + mob.getConfigName());
            return null;
        }

        Entity entity = loc.getWorld().spawnEntity(loc, entityType);
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.setCustomName(ChatHelper.colorize(mob.getDisplayName()));
            livingEntity.setCustomNameVisible(true);

            if (livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(settings.getHealth());
            }
            livingEntity.setHealth(settings.getHealth());
            if (livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(settings.getDamage());
            }

            EntityEquipment equipment = livingEntity.getEquipment();
            if (equipment != null) {
                equipment.setHelmet(createEnchantedItem(settings, EquipmentSelectorMenu.EquipmentType.HELMET));
                equipment.setChestplate(createEnchantedItem(settings, EquipmentSelectorMenu.EquipmentType.CHESTPLATE));
                equipment.setLeggings(createEnchantedItem(settings, EquipmentSelectorMenu.EquipmentType.LEGGINGS));
                equipment.setBoots(createEnchantedItem(settings, EquipmentSelectorMenu.EquipmentType.BOOTS));
                equipment.setItemInMainHand(createEnchantedItem(settings, EquipmentSelectorMenu.EquipmentType.WEAPON));

                equipment.setHelmetDropChance(0f);
                equipment.setChestplateDropChance(0f);
                equipment.setLeggingsDropChance(0f);
                equipment.setBootsDropChance(0f);
                equipment.setItemInMainHandDropChance(0f);
            }

            livingEntity.setPersistent(true);
            livingEntity.setRemoveWhenFarAway(false);
            return livingEntity;
        }
        return null;
    }

    private ItemStack createEnchantedItem(VanillaMobSettings settings, EquipmentSelectorMenu.EquipmentType type) {
        String materialName = settings.getEquipment(type);
        if (materialName == null || materialName.equalsIgnoreCase("AIR") || materialName.isEmpty()) {
            return new ItemStack(Material.AIR);
        }

        ItemStack item;
        try {
            item = new ItemStack(Material.valueOf(materialName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material name '" + materialName + "' for equipment slot " + type.name());
            return new ItemStack(Material.AIR);
        }

        Map<String, Integer> enchantments = settings.getEnchantMapFor(type);
        if (enchantments != null && !enchantments.isEmpty()) {
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(entry.getKey().toLowerCase()));
                if (enchantment != null) {
                    item.addUnsafeEnchantment(enchantment, entry.getValue());
                }
            }
        }
        return item;
    }

    private Optional<Location> findSafeLocation(Location center, int minRadius, int maxRadius) {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minRadius + (maxRadius - minRadius) * random.nextDouble();
            double x = center.getX() + distance * Math.cos(angle);
            double z = center.getZ() + distance * Math.sin(angle);

            Location loc = new Location(center.getWorld(), x, 0, z);
            loc.setY(center.getWorld().getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ()) + 1);

            if (loc.getBlock().getType().isSolid() && loc.clone().add(0, 1, 0).getBlock().isEmpty()) {
                return Optional.of(loc);
            }
        }
        return Optional.empty();
    }

    public void registerActiveMob(Mob mob, Entity entity) {
        activeCustomMobs.computeIfAbsent(mob.getConfigName(), k -> new HashSet<>()).add(entity.getUniqueId());
    }

    public void unregisterActiveMob(Mob mob, Entity entity) {
        if (mob != null) {
            Set<UUID> mobSet = activeCustomMobs.get(mob.getConfigName());
            if (mobSet != null) {
                mobSet.remove(entity.getUniqueId());
            }
        }
    }

    private int getActiveMobCount(String mobConfigName) {
        Set<UUID> uuids = activeCustomMobs.get(mobConfigName);
        if (uuids == null) return 0;

        uuids.removeIf(uuid -> Bukkit.getEntity(uuid) == null || Bukkit.getEntity(uuid).isDead());

        return uuids.size();
    }

    public void updateSpawningForMob(Mob mob) {
        startSpawnerTask();
    }

    public void removeMobFromSpawning(Mob mob) {
        activeCustomMobs.remove(mob.getConfigName());
    }
}