package me.monkeyland.monkeylandeconomy.hooks;

import me.monkeyland.monkeylandeconomy.MonkeylandEconomy;
import me.monkeyland.monkeylandeconomy.MonkeylandEconomy.Currency;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final MonkeylandEconomy plugin;

    public PlaceholderAPIHook(MonkeylandEconomy plugin) {
        this.plugin = plugin;
    }



    @Override
    public String getIdentifier() {
        return "monkeylandeconomy";
    }

    @Override
    public String getAuthor() {
        return "0xSimbaDev";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        // Exchange rate placeholders
        if (identifier.startsWith("exchange_")) {
            String currencyName = identifier.substring(9); // Extract the currency name
            try {
                Currency fromCurrency = Currency.valueOf(currencyName.toUpperCase());
                double exchangeRate = plugin.getExchangeRate(fromCurrency); // Directly get the exchange rate for the currency to gold
                return String.format("%.4f", exchangeRate);
            } catch (IllegalArgumentException e) {
                return "Invalid currency";
            }
        }
        // Balance placeholders
        else if (identifier.startsWith("balance_")) {
            String currencyName = identifier.substring(8);
            try {
                Currency currency = Currency.valueOf(currencyName.toUpperCase());
                if (player != null && player.isOnline()) {
                    double balance = plugin.getBalance(player.getUniqueId(), currency);
                    return String.format("%.2f", balance);
                } else {
                    return "Player not found or offline";
                }
            } catch (IllegalArgumentException e) {
                return "Invalid currency";
            }
        }

        return null;
    }
}
