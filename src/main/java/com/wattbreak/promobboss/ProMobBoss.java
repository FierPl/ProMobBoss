package com.wattbreak.promobboss;

import com.wattbreak.promobboss.boss.BossManager;
import com.wattbreak.promobboss.boss.BossScheduler;
import com.wattbreak.promobboss.commands.CommandManager;
import com.wattbreak.promobboss.config.ConfigManager;
import com.wattbreak.promobboss.config.LanguageManager;
import com.wattbreak.promobboss.data.IDataSource;
import com.wattbreak.promobboss.data.LeaderboardCache;
import com.wattbreak.promobboss.data.MySQLDataSource;
import com.wattbreak.promobboss.data.SQLiteDataSource;
import com.wattbreak.promobboss.gui.MenuListener;
import com.wattbreak.promobboss.integrations.DiscordWebhookHandler;
import com.wattbreak.promobboss.integrations.MythicMobsHandler;
import com.wattbreak.promobboss.integrations.ProMobBossExpansion;
import com.wattbreak.promobboss.listeners.BossDeathListener;
import com.wattbreak.promobboss.listeners.EggListener;
import com.wattbreak.promobboss.listeners.PlayerListener;
import com.wattbreak.promobboss.mob.MobManager;
import com.wattbreak.promobboss.mob.MobSpawner;
import com.wattbreak.promobboss.reward.RewardListener;
import com.wattbreak.promobboss.utility.ChatInputHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.wattbreak.promobboss.utility.UpdateChecker;

public final class ProMobBoss extends JavaPlugin {

    private static ProMobBoss instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private BossManager bossManager;
    private MobManager mobManager;
    private BossScheduler bossScheduler;
    private MobSpawner mobSpawner;
    private CommandManager commandManager;
    private ChatInputHandler chatInputHandler;
    private RewardListener rewardListener;
    private MythicMobsHandler mythicMobsHandler;
    private DiscordWebhookHandler discordWebhookHandler;

    private boolean mythicMobsEnabled = false;
    private boolean placeholderApiEnabled = false;

    private IDataSource dataSource;
    private LeaderboardCache leaderboardCache;

    private UpdateChecker updateChecker; // Referans

    private ProMobBossExpansion proMobBossExpansion;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);
        initializeDataSource();
        this.leaderboardCache = new LeaderboardCache(this, this.dataSource);

        getLogger().info("=============================================");
        getLogger().info("Enabling ProMobBoss v" + getDescription().getVersion());

        checkDependencies();

        this.bossManager = new BossManager(this);
        this.mobManager = new MobManager(this);
        this.bossScheduler = new BossScheduler(this, this.bossManager);
        this.mobSpawner = new MobSpawner(this, this.mobManager);

        this.commandManager = new CommandManager(this);

        this.updateChecker = new UpdateChecker(this, "FierPl/ProMythicBosses");

        registerListeners();

        getLogger().info("ProMobBoss has been enabled successfully!");
        getLogger().info("=============================================");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);

        if (dataSource != null) {
            dataSource.shutdown();
        }

        if (proMobBossExpansion != null) {
            proMobBossExpansion.onDisable();
        }

        getLogger().info("=============================================");
        getLogger().info("ProMobBoss has been disabled.");
        getLogger().info("=============================================");
    }

    private void initializeDataSource() {
        String storageType = configManager.getMainConfig().getString("storage.type", "SQLITE").toUpperCase();

        if ("MYSQL".equals(storageType)) {
            this.dataSource = new MySQLDataSource(this);
            getLogger().info("Storage type set to MYSQL.");
        } else {
            this.dataSource = new SQLiteDataSource(this);
            getLogger().info("Storage type set to SQLITE.");
        }

        this.dataSource.init();
    }

    public void checkDependencies() {
        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            try {
                this.mythicMobsHandler = new MythicMobsHandler(this);
                if (this.mythicMobsHandler.isReady()) {
                    this.mythicMobsEnabled = true;
                    getLogger().info("Successfully hooked into MythicMobs.");
                } else {
                    throw new Exception("Handler could not be initialized.");
                }
            } catch (Throwable t) {
                this.mythicMobsEnabled = false;
                getLogger().warning("Found MythicMobs, but failed to hook into its API. Features disabled.");
            }
        } else {
            this.mythicMobsEnabled = false;
            getLogger().warning("MythicMobs not found! MythicMobs-related features will be disabled.");
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderApiEnabled = true;
            this.proMobBossExpansion = new ProMobBossExpansion(this);
            this.proMobBossExpansion.register();
            getLogger().info("Successfully hooked into PlaceholderAPI and registered placeholders.");
        } else {
            this.placeholderApiEnabled = false;
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }

        this.discordWebhookHandler = new DiscordWebhookHandler(this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new BossDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new EggListener(this), this);

        this.chatInputHandler = new ChatInputHandler(this);
        this.rewardListener = new RewardListener(this);

        getLogger().info("Listeners have been registered.");
    }

    public static ProMobBoss getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public IDataSource getDataSource() { return dataSource; }
    public LeaderboardCache getLeaderboardCache() { return leaderboardCache; }
    public BossManager getBossManager() { return bossManager; }
    public MobManager getMobManager() { return mobManager; }
    public BossScheduler getBossScheduler() { return bossScheduler; }
    public MobSpawner getMobSpawner() { return mobSpawner; }
    public ChatInputHandler getChatInputHandler() { return chatInputHandler; }
    public RewardListener getRewardListener() { return rewardListener; }
    public MythicMobsHandler getMythicMobsHandler() { return this.mythicMobsHandler; }
    public DiscordWebhookHandler getDiscordWebhookHandler() { return discordWebhookHandler; }

    public String getMessage(String path) { return languageManager.getMessage(path); }
    public String getMessage(String path, String... replacements) { return languageManager.getMessage(path, replacements); }

    public boolean isMythicMobsEnabled() { return mythicMobsEnabled; }
    public boolean isPlaceholderApiEnabled() { return placeholderApiEnabled; }

    public UpdateChecker getUpdateChecker() { return updateChecker; }
}