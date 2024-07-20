package me.monkeyland.monkeylandeconomy;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.ChatColor;

import org.bukkit.Bukkit;
import me.monkeyland.monkeylandeconomy.hooks.PlaceholderAPIHook;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MonkeylandEconomy extends JavaPlugin implements Listener {

    public enum Currency {
        BRONZE,
        COPPER,
        SILVER,
        GOLD
    }

    private FileConfiguration economyDataConfig;
    private final String ECONOMY_DATA_FILE_NAME = "monkeyland_economy.yml";

    // Economy Data (default values)
    private final double DEFAULT_MAX_GOLD_SUPPLY = 1000000.0;
    private final double DEFAULT_STARTING_INFLATION_RATE = 0.0015; // 0.15% per day
    private final double DEFAULT_TARGET_INFLATION_RATE = 0.005;   // 0.5% per day
    private final double DEFAULT_INFLATION_CURVE_FACTOR = 2.0;
    private final Map<Currency, Double> DEFAULT_EXCHANGE_RATES = new HashMap<>() {{
        put(Currency.BRONZE, 0.001);
        put(Currency.COPPER, 0.01);
        put(Currency.SILVER, 0.1);
        put(Currency.GOLD, 1.0);
    }};

    // Economy Parameters
    private double maxGoldSupply;
    private double startingInflationRate;
    private double currentInflationRate;
    private double inflationFactor = 1.0;

    // Player Data
    private Map<UUID, Map<Currency, Double>> playerBalances = new HashMap<>();

    // Other variables derived from the data
    private double circulatingGoldSupply = 0.0;

    // Exchange Rates
    private Map<Currency, Double> exchangeRates = new HashMap<>(); // Initialized here

    // Scheduler Task
    private BukkitTask inflationTask;

    @Override
    public void onEnable() {
        // Load or create the economy data file
        economyDataConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), ECONOMY_DATA_FILE_NAME));

        // Initialize maxGoldSupply to the default value
        maxGoldSupply = DEFAULT_MAX_GOLD_SUPPLY;
        getLogger().info("Initialized maxGoldSupply with default value: " + maxGoldSupply);

        // Load the value from the config file ONLY if it exists
        if (economyDataConfig.contains("maxSupply.GOLD")) {
            maxGoldSupply = economyDataConfig.getDouble("maxSupply.GOLD");
            getLogger().info("Loaded maxGoldSupply from config file: " + maxGoldSupply);
        } else {
            getLogger().info("Key 'maxSupply.GOLD' not found in config file, using default value.");
        }

        // If the configuration is empty, initialize the rest of the data
        if (economyDataConfig.getKeys(false).isEmpty()) {
            getLogger().info("Configuration file is empty. Initializing economy data.");
            initializeEconomyData();
        } else {
            getLogger().info("Loading economy data from config file.");
            loadDatabaseData();
        }

        // Calculate initial circulating supply
        calculateCirculatingSupply();

        // Register events and commands
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("monkeyland").setExecutor(new MonkeylandCommand(this));

        // PlaceholderAPI Registration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register(); // Register the PlaceholderExpansion
        } else {
            getLogger().severe("Could not find PlaceholderAPI! This plugin is required.");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        // Schedule task to run daily
        // long inflationDelay = 20L * 60 * 60 * 24; // 24 hours in ticks
        long inflationDelay = 20L * 60 * 60;
        inflationTask = getServer().getScheduler().runTaskTimer(this, this::adjustInflationAndExchangeRates, inflationDelay, inflationDelay);

        // Log initial values to console
        getLogger().info("MonkeylandEconomy Initialized:");
        getLogger().info("Gold Max Supply: " + getMaxGoldSupply());
        getLogger().info("Gold Starting Circulating Supply: " + getCirculatingGoldSupply());
        getLogger().info("Inflation Rate (Gold): " + (getGoldInflationRate() * 100) + "% per day");
        for (Currency currency : Currency.values()) {
            getLogger().info(currency + " exchange rate to Gold: " + getExchangeRate(currency));
        }
    }

    @Override
    public void onDisable() {
        saveEconomyData();
        inflationTask.cancel();
    }

    // --- Database Handling ---

    private void initializeEconomyData() {
        getLogger().info("Initializing economy data...");

        exchangeRates.putAll(DEFAULT_EXCHANGE_RATES); // Add this line

        for (Currency currency : Currency.values()) {
            economyDataConfig.set("exchangeRates." + currency.name(), DEFAULT_EXCHANGE_RATES.get(currency));
            getLogger().info("Set exchange rate for " + currency.name() + " to: " + DEFAULT_EXCHANGE_RATES.get(currency));
        }

        economyDataConfig.set("inflationRate", DEFAULT_STARTING_INFLATION_RATE);
        getLogger().info("Set inflationRate to: " + DEFAULT_STARTING_INFLATION_RATE);

        economyDataConfig.set("inflationFactor", 1.0);
        getLogger().info("Set inflationFactor to: 1.0");

        economyDataConfig.set("startingInflationRate", DEFAULT_STARTING_INFLATION_RATE);
        getLogger().info("Set startingInflationRate to: " + DEFAULT_STARTING_INFLATION_RATE);

        economyDataConfig.set("targetInflationRate", DEFAULT_TARGET_INFLATION_RATE);
        getLogger().info("Set targetInflationRate to: " + DEFAULT_TARGET_INFLATION_RATE);

        economyDataConfig.set("inflationCurveFactor", DEFAULT_INFLATION_CURVE_FACTOR);
        getLogger().info("Set inflationCurveFactor to: " + DEFAULT_INFLATION_CURVE_FACTOR);

        economyDataConfig.set("maxSupply.GOLD", DEFAULT_MAX_GOLD_SUPPLY);
        getLogger().info("Set maxSupply.GOLD to: " + DEFAULT_MAX_GOLD_SUPPLY);

        economyDataConfig.createSection("players");
        getLogger().info("Created 'players' section.");

        saveEconomyData();
        getLogger().info("Saved initial economy data.");
    }

    private void loadDatabaseData() {
        getLogger().info("Loading economy data from configuration file...");

        // Load inflation rates, curve factor, and exchange rates with logging
        startingInflationRate = economyDataConfig.getDouble("startingInflationRate", DEFAULT_STARTING_INFLATION_RATE);
        getLogger().info("Loaded startingInflationRate: " + startingInflationRate);

        for (Currency currency : Currency.values()) {
            double exchangeRate = economyDataConfig.getDouble("exchangeRates." + currency.name(), DEFAULT_EXCHANGE_RATES.get(currency));
            exchangeRates.put(currency, exchangeRate);
            getLogger().info("Loaded exchangeRate for " + currency.name() + ": " + exchangeRate);
        }

        // Load inflation rate and factor and log results
        Object inflationRateValue = economyDataConfig.get("inflationRate");
        if (inflationRateValue instanceof Double) {
            currentInflationRate = (double) inflationRateValue;
            getLogger().info("Loaded currentInflationRate from config: " + currentInflationRate);
        } else {
            currentInflationRate = DEFAULT_STARTING_INFLATION_RATE;
            getLogger().info("Using default currentInflationRate: " + currentInflationRate);
        }

        Object inflationFactorValue = economyDataConfig.get("inflationFactor");
        if (inflationFactorValue instanceof Double) {
            inflationFactor = (double) inflationFactorValue;
            getLogger().info("Loaded inflationFactor from config: " + inflationFactor);
        } else {
            inflationFactor = 1.0;
            getLogger().info("Using default inflationFactor: " + inflationFactor);
        }

        // Load player balances
        ConfigurationSection playersSection = economyDataConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidString : playersSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    Map<Currency, Double> balances = new HashMap<>();
                    for (Currency currency : Currency.values()) {
                        double balance = economyDataConfig.getDouble("players." + uuidString + "." + currency, 0.0);
                        balances.put(currency, balance);
                    }
                    playerBalances.put(playerId, balances);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid UUID found in player data: " + uuidString);
                }
            }
        }
    }

    private void saveEconomyData() {
        // Save configuration and economy data
        economyDataConfig.set("maxSupply.GOLD", maxGoldSupply);
        economyDataConfig.set("inflationRate", currentInflationRate);
        economyDataConfig.set("inflationFactor", inflationFactor);
        economyDataConfig.set("startingInflationRate", startingInflationRate);

        for (Currency currency : Currency.values()) {
            double exchangeRate = exchangeRates.get(currency);

            // Round to 4 decimal places
            exchangeRate = Math.round(exchangeRate * 10000.0) / 10000.0;

            economyDataConfig.set("exchangeRates." + currency.name(), exchangeRate);
        }

        // Save player balances
        economyDataConfig.set("players", null); // Clear existing player data
        for (Map.Entry<UUID, Map<Currency, Double>> entry : playerBalances.entrySet()) {
            UUID playerId = entry.getKey();
            Map<Currency, Double> balances = entry.getValue();
            for (Currency currency : Currency.values()) {
                economyDataConfig.set("players." + playerId + "." + currency, balances.get(currency));
            }
        }

        try {
            economyDataConfig.save(new File(getDataFolder(), ECONOMY_DATA_FILE_NAME));
        } catch (IOException e) {
            getLogger().severe("Failed to save economy data: " + e.getMessage());
        }
    }

    private void calculateCirculatingSupply() {
        circulatingGoldSupply = 0.0;
        for (Map<Currency, Double> balances : playerBalances.values()) {
            circulatingGoldSupply += balances.getOrDefault(Currency.GOLD, 0.0); // Sum only gold balances
        }
    }

    private void adjustInflationAndExchangeRates() {
        calculateCirculatingSupply();

        double targetSupplyRatio = 0.25;
        double supplyRatio = circulatingGoldSupply / maxGoldSupply;

        // Calculate inflation/deflation rate directly based on deviation from target
        currentInflationRate = startingInflationRate + (supplyRatio - targetSupplyRatio) * 0.03;

        // Ensure inflation/deflation rate stays within reasonable bounds (-10% to 10%)
        currentInflationRate = Math.max(-0.10, Math.min(0.1, currentInflationRate));

        // Adjust exchange rates based on inflation/deflation
        for (Currency currency : Currency.values()) {
            if (currency != Currency.GOLD) {
                double baseExchangeRate = exchangeRates.get(currency);
                double newRate = baseExchangeRate / (1 + currentInflationRate);
                exchangeRates.put(currency, newRate);
                getLogger().info(currency.name() + " exchange rate to Gold: " + String.format("%.6f", newRate)); // Log with 6 decimal places
            }
        }

        // Logging
        // Logging
        getLogger().info("Gold Circulating Supply: " + circulatingGoldSupply);
        getLogger().info("Current Inflation Rate (Gold): " + String.format("%.2f%%", currentInflationRate * 100));
        for (Currency currency : Currency.values()) {
            if (currency != Currency.GOLD) {
                getLogger().info(currency.name() + " exchange rate to Gold: " + String.format("%.4f", getExchangeRate(currency)));
            }
        }
    }

    public double getMaxGoldSupply() {
        return maxGoldSupply;
    }

    public double getCirculatingGoldSupply() {
        return circulatingGoldSupply;
    }

    public double getGoldInflationRate() {
        return currentInflationRate;
    }

    public double getExchangeRate(Currency currency) {
        return exchangeRates.getOrDefault(currency, 0.0);
    }

    public double getBalance(UUID playerId, Currency currency) {
        Map<Currency, Double> balances = playerBalances.get(playerId);
        if (balances == null) {
            balances = new HashMap<>();
            playerBalances.put(playerId, balances);
        }
        return balances.getOrDefault(currency, 0.0);
    }

/*    public void setBalance(UUID playerId, Currency currency, double amount) {
        Map<Currency, Double> balances = playerBalances.get(playerId);
        if (balances == null) {
            balances = new HashMap<>();
            playerBalances.put(playerId, balances);
        }
        balances.put(currency, amount);
    }*/

    public void addBalance(UUID playerId, Currency currency, double amount) {
        Player player = Bukkit.getPlayer(playerId);

        if (player != null && player.isOnline()) {

            Map<Currency, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new HashMap<>());
            double currentBalance = balances.getOrDefault(currency, 0.0);

            if (currency == Currency.GOLD) {
                double newGoldBalance = currentBalance + amount;
                if (newGoldBalance > maxGoldSupply) {
                    getLogger().warning("Attempt to add gold to player " + playerId + " would exceed max supply. Transaction cancelled.");
                    player.sendMessage(ChatColor.RED + "Error: Adding this amount would exceed the maximum gold supply.");
                    return;
                }
            } else {
                balances.merge(currency, amount, Double::sum);
            }

            // Update the player's balances
            playerBalances.put(playerId, balances);
            saveEconomyData();
        } else {
            getLogger().warning("Cannot add balance to offline player: " + playerId);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material brokenBlock = event.getBlock().getType();
        UUID playerId = player.getUniqueId();

        if (brokenBlock == Material.GOLD_ORE) {
            double goldReward = 1.0;
            addBalance(playerId, Currency.GOLD, goldReward);
            player.sendMessage(ChatColor.YELLOW + "You earned " + goldReward + " Gold!");
        } else if (brokenBlock == Material.COPPER_ORE) {
            double copperReward = 5.0;
            addBalance(playerId, Currency.COPPER, copperReward);
            player.sendMessage(ChatColor.YELLOW + "You earned " + copperReward + " Copper!");
        }
    }

    public void exchangeCurrencyForGold(Player player, Currency fromCurrency, Currency toCurrency, double amount) {
        UUID playerId = player.getUniqueId();
        double playerFromCurrencyBalance = getBalance(playerId, fromCurrency);

        if (fromCurrency == toCurrency) {
            player.sendMessage(ChatColor.RED + "You cannot exchange the same currency.");
            return;
        }

        double exchangeRate = getExchangeRate(fromCurrency) / getExchangeRate(toCurrency);
        double toCurrencyAmount = amount * exchangeRate;

        if (playerFromCurrencyBalance >= amount) {
            // Update player balances
            addBalance(playerId, toCurrency, toCurrencyAmount);
            addBalance(playerId, fromCurrency, -amount);

            String message = "Successfully exchanged " + amount + " " + fromCurrency + " for " + String.format("%.2f", toCurrencyAmount) + " " + toCurrency;
            player.sendMessage(ChatColor.GREEN + message);
        } else {
            player.sendMessage(ChatColor.RED + "You don't have enough " + fromCurrency + " to make this exchange.");
        }
    }
}
