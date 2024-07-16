# MonkeylandEconomy - A Dynamic Minecraft Economy Plugin

**Project Description:**

MonkeylandEconomy is a Bukkit/Spigot plugin that creates a dynamic and engaging in-game economy for your Minecraft server. It features a gold-based currency system with multiple additional currencies (bronze, copper, silver) that are pegged to gold. The plugin simulates realistic economic principles like inflation and deflation, with exchange rates that fluctuate based on the amount of gold in circulation. 

**Features:**

- **Gold-Based Economy:** Gold is the primary currency, with a configurable maximum supply.
- **Multiple Currencies:** Includes bronze, copper, and silver currencies, each with its own exchange rate to gold.
- **Dynamic Exchange Rates:** Exchange rates change based on the supply and demand of gold, simulating inflation and deflation.
- **Player Balances:** Tracks player balances for all currencies.
- **Currency Exchange:** Allows players to exchange different currencies with each other and with the server (through commands or NPC shops).
- **Inflation and Deflation:** Simulates the effects of inflation and deflation on the value of gold and exchange rates.
- **PlaceholderAPI Integration:**  Provides placeholders to display balances and exchange rates in chat, signs, scoreboards, and other plugins like DeluxeMenus.

**Installation:**

1. **Download:** Download the latest release of MonkeylandEconomy.
2. **Install:**  Place the `MonkeylandEconomy.jar` file into your server's `plugins` directory.
3. **Dependencies:** Ensure that you have PlaceholderAPI installed.
4. **Start/Restart Server:** Start or restart your Minecraft server.
5. **Configuration:** Customize the economy by editing the `monkeyland_economy.yml` configuration file in your `plugins/MonkeylandEconomy` directory.

**Configuration:**

- **`maxSupply.GOLD`:**  The maximum amount of gold allowed in the economy.
- **`startingInflationRate`:** The base inflation rate for gold (percentage per time period).
- **`targetInflationRate`:** The desired inflation rate for gold. The economy will adjust to try and reach this rate.
- **`inflationCurveFactor`:** Controls the sensitivity of inflation/deflation to changes in the gold supply.
- **`exchangeRates`:** The initial exchange rates for each currency to gold.

**Usage:**

- **Player Commands:**
    - `/monkeyland balance [currency]`: Check your balance (defaults to gold if no currency is specified).
    - `/monkeyland exchange <fromCurrency> <toCurrency> <amount>`: Exchange currencies.
    - `/monkeyland info`: (Admin only) View detailed information about the economy.
    - `/monkeyland set <player> <currency> <amount>`: (Admin only) Set a player's balance.
    - `/monkeyland add <player> <currency> <amount>`: (Admin only) Add currency to a player's balance.

**Dependencies:**

- **Spigot API:**  This plugin is built for Spigot/Bukkit servers.
- **PlaceholderAPI:** Required for displaying dynamic values in chat, signs, etc.
