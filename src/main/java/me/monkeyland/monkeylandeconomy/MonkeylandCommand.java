package me.monkeyland.monkeylandeconomy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public class MonkeylandCommand implements CommandExecutor {
    private final MonkeylandEconomy plugin;

    public MonkeylandCommand(MonkeylandEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                handleInfoCommand(player);
                break;
            case "balance":
            case "bal":
                handleBalanceCommand(player, args);
                break;
            case "add":
                handleAddCommand(player, args);
                break;
            case "exchange":
                handleExchangeCommand(player, args);
                break;
            case "give":
                handleGiveCommand(player, args);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Invalid command. Use /monkeyland for help.");
        }
        return true;
    }

    // Method for displaying the help message
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "Monkeyland Economy Commands:");
        player.sendMessage(ChatColor.YELLOW + "/monkeyland balance [currency]" + ChatColor.WHITE + " - Check your balances.");
        player.sendMessage(ChatColor.YELLOW + "/monkeyland exchange <fromCurrency> <toCurrency> <amount>" + ChatColor.WHITE + " - Exchange currencies.");
        player.sendMessage(ChatColor.YELLOW + "/monkeyland give <player> <currency> <amount>" + ChatColor.WHITE + " - Give currency to another player.");

        if (player.hasPermission("monkeylandeconomy.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/monkeyland info" + ChatColor.WHITE + " - View economy info.");
            player.sendMessage(ChatColor.YELLOW + "/monkeyland add <player> <currency> <amount>" + ChatColor.WHITE + " - Add to a player's balance.");
        }
    }

    // Method for handling the /monkeyland info command
    private void handleInfoCommand(Player player) {
        if (player.hasPermission("monkeylandeconomy.admin")) {
            player.sendMessage(ChatColor.GOLD + "Monkeyland Economy Information:");
            player.sendMessage(ChatColor.YELLOW + "Gold Max Supply: " + ChatColor.WHITE + String.format("%.2f", plugin.getMaxGoldSupply()));
            player.sendMessage(ChatColor.YELLOW + "Gold Circulating Supply: " + ChatColor.WHITE + String.format("%.2f", plugin.getCirculatingGoldSupply()));
            player.sendMessage(ChatColor.YELLOW + "Gold Inflation Rate: " + ChatColor.WHITE + String.format("%.2f%%", plugin.getGoldInflationRate() * 100));

            for (MonkeylandEconomy.Currency currency : MonkeylandEconomy.Currency.values()) {
                if (currency != MonkeylandEconomy.Currency.GOLD) { // Only display non-gold currencies
                    player.sendMessage(ChatColor.YELLOW + currency.name() + " exchange rate to Gold: " + ChatColor.WHITE + String.format("%.4f", plugin.getExchangeRate(currency)));
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
        }
    }

    // Method for handling the /monkeyland balance command
    private void handleBalanceCommand(Player player, String[] args) {
        if (args.length > 1) {
            MonkeylandEconomy.Currency currency;
            try {
                currency = MonkeylandEconomy.Currency.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid currency.");
                return;
            }
            double balance = plugin.getBalance(player.getUniqueId(), currency);
            player.sendMessage(ChatColor.GOLD + "Your " + currency + " Balance: " + ChatColor.WHITE + String.format("%.2f", balance));
        } else { // No currency specified, show all balances
            player.sendMessage(ChatColor.GOLD + "Your Balances:");
            for (MonkeylandEconomy.Currency currency : MonkeylandEconomy.Currency.values()) {
                double balance = plugin.getBalance(player.getUniqueId(), currency);
                if (balance > 0) {
                    player.sendMessage(ChatColor.YELLOW + currency.name() + ": " + ChatColor.WHITE + String.format("%.2f", balance));
                }
            }
        }
    }

    /**
     * @deprecated Use the `/monkeyland add` command instead to modify player balances.
     */
    // Method for handling the /monkeyland set command
    @Deprecated
    private void handleSetCommand(Player player, String[] args) {
        if (!player.hasPermission("monkeylandeconomy.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }

        if (args.length != 4) {
            player.sendMessage(ChatColor.RED + "Usage: /monkeyland set <player> <currency> <amount>");
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        MonkeylandEconomy.Currency currency = MonkeylandEconomy.Currency.GOLD; // Default to gold

        if (args.length > 1) {
            try {
                currency = MonkeylandEconomy.Currency.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid currency.");
                return;
            }
        }


        double balance = plugin.getBalance(player.getUniqueId(), currency);
        player.sendMessage(ChatColor.GOLD + "Your " + currency + " Balance: " + ChatColor.WHITE + String.format("%.2f", balance));
    }

    private void handleAddCommand(Player player, String[] args) {
        if (!player.hasPermission("monkeylandeconomy.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }

        if (args.length != 4) {
            player.sendMessage(ChatColor.RED + "Usage: /monkeyland add <player> <currency> <amount>");
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        MonkeylandEconomy.Currency currency;
        try {
            currency = MonkeylandEconomy.Currency.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid currency.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[3]);
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "Amount must be positive.");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return;
        }

        plugin.addBalance(targetPlayer.getUniqueId(), currency, amount);
        player.sendMessage(ChatColor.GREEN + "Added " + amount + " " + currency + " to " + targetPlayer.getName() + "'s balance.");

    }

    private void handleExchangeCommand(Player player, String[] args) {
        if (args.length != 4) {
            player.sendMessage(ChatColor.RED + "Usage: /monkeyland exchange <fromCurrency> <toCurrency> <amount>");
            return;
        }

        MonkeylandEconomy.Currency fromCurrency, toCurrency;
        try {
            fromCurrency = MonkeylandEconomy.Currency.valueOf(args[1].toUpperCase());
            toCurrency = MonkeylandEconomy.Currency.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid currency.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return;
        }

        plugin.exchangeCurrencyForGold(player, fromCurrency, toCurrency, amount);
    }

    private void handleGiveCommand(Player player, String[] args) {
        if (args.length != 4) {
            player.sendMessage(ChatColor.RED + "Usage: /monkeyland give <player> <currency> <amount>");
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        MonkeylandEconomy.Currency currency;
        try {
            currency = MonkeylandEconomy.Currency.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid currency.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[3]);
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "Amount must be positive.");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return;
        }

        // Security Check 1: Ensure the player has enough balance
        if (plugin.getBalance(player.getUniqueId(), currency) >= amount) {

            // Security Check 2: Prevent self-payment
            if (player.getUniqueId().equals(targetPlayer.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You cannot give money to yourself.");
                return;
            }

            // Transaction using addBalance
            plugin.addBalance(player.getUniqueId(), currency, -amount);
            plugin.addBalance(targetPlayer.getUniqueId(), currency, amount);

            player.sendMessage(ChatColor.GREEN + "You gave " + amount + " " + currency + " to " + targetPlayer.getName() + ".");
            targetPlayer.sendMessage(ChatColor.GREEN + "You received " + amount + " " + currency + " from " + player.getName() + ".");
        } else {
            player.sendMessage(ChatColor.RED + "You don't have enough " + currency + ".");
        }
    }

}
