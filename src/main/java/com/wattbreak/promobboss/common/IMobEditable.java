package com.wattbreak.promobboss.common;

import org.bukkit.Location;
import com.wattbreak.promobboss.reward.IRewardable;

/**
 * Hem Boss hem de Mob nesnelerinin ortak düzenlenebilir özelliklerini temsil eden arayüz.
 * IRewardable ve IMobTyped arayüzlerini genişletir.
 */
public interface IMobEditable extends IRewardable, IMobTyped {

    @Override
    String getDisplayName();

    void setDisplayName(String displayName);

    boolean isEnabled();
    void setEnabled(boolean enabled);

    Location getSpawnLocation();
    void setSpawnLocation(Location location);

    String getSpawnWorld();
    void setSpawnWorld(String world);
    int getSpawnInterval();
    void setSpawnInterval(int interval);
    int getMaxAlive();
    void setMaxAlive(int maxAlive);
}