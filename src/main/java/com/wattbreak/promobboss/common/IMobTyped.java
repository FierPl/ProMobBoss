package com.wattbreak.promobboss.common;

/**
 * Mob tipi (MythicMobs veya Vanilla) ve MythicMobs adı gibi bilgileri
 * taşıyan varlıklar için ortak arayüz.
 * Bu arayüz, mob tipi seçim menülerinin Boss ve Mob nesneleriyle çalışmasını sağlar.
 */
public interface IMobTyped {
    String getMobTypeSource();
    void setMobTypeSource(String source);
    String getMythicMobName();
    void setMythicMobName(String name);
    VanillaMobSettings getVanillaMobSettings(); // Mob'a özel vanilya ayarları
}