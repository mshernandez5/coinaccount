# <img src="logo.svg" alt="Vertconomy Logo" height="75px">

Vertconomy brings Vertcoin into your server's economy with support for in-game deposits, withdrawals, and transfers.\
Vertconomy is configured to work with Vertcoin by default but should be compatible with most coins supporting standard bitcoind RPC calls.
## Why Vertcoin?
[Vertcoin](https://vertcoin.org/) is committed to maintaining an ASIC-resistant mining algorithm, forking whenever necessary to continue doing so. Keeping dedicated mining hardware off the table makes it possible to mine with consumer-grade GPUs in normal gaming computers with as little as 2GB VRAM. With the [Vertcoin One Click Miner](https://github.com/vertcoin-project/one-click-miner-vnext/releases), players can easily get involved and start mining cryptocurrency without having to purchase any directly. You can even mine while playing Minecraft! Though this plugin is not exclusively compatible with Vertcoin, there are few other coins that are as easily accessible without asking players to buy from an exchange.

## How does it work?
Vertconomy interfaces with a user-provided wallet via standard [bitcoind RPC methods](https://developer.bitcoin.org/reference/rpc/), allowing a player to deposit and withdraw Vertcoin through the Minecraft plugin.

All player balances are held as part of the same wallet with an embedded database keeping track of specific player balances for zero-fee transfers between players within the server.

## Does it work with existing plugins?
Vertconomy aims to provide at least limited functionality with existing plugins through the common Minecraft [Vault API](https://github.com/MilkBowl/VaultAPI).
Unfortunately, by nature of the API it is impossible to create a proper implementation due to the (fair) assumption that in-game currency can be created out of thin air and sent back into the void at will.

*So does it support Vault API plugins?* Well, I'd say it's more like Vertconomy "has the potential to work with a limited selection of Vault-compatible plugins through forbidden dark magic that comes with the risk of lost funds and may easily break with plugin updates" =)

On top of the non-standard Vault implementation provided by this plugin, other plugins tend to ignore the success/failure responses provided by Vault API calls leading to situations where actions may be taken despite the server or player not having the necessary funds to do so. Please test any external plugin features thoroughly before making them available to players.

Due to the complexities of working with Vault plugins and relatively high risk of unintended behavior Vault support is disabled by default, but this can be changed in the plugin configuration.

## How can I use this plugin?
You will need a dedicated Vertcoin wallet configured to listen for RPC connections. Do not use an existing Vertcoin wallet. The configuration file for Vertcoin wallet can be found at one of the following locations if the wallet configuration directory was not manually changed:

Operating System | Location
-----------------|---------
Windows | `%appdata%\vertcoin\vertcoin.conf`
Linux: | `~/.vertcoin/vertcoin.conf`

\
If the configuration file does not exist - which is likely if you never needed to edit it before - create it.
Here is an example configuration that will allow the wallet to accept RPC connections at `http://127.0.0.1:5888` using the specified username and password:

```properties
server=1
rpcuser=vtcuser
rpcpassword=vtcpass
rpcport=5888
```

Please change the username and password to non-default values. After saving the new configuration, the changes will take effect after starting `vertcoind` (recommended, headless) or `vertcoin-qt` (graphical interface).

The plugin will need to know how to connect to the wallet, so you will also need a plugin configuration to match in `plugins/Vertconomy/config.yml` relative to the Minecraft server directory. If no configuration exists on startup, a default configuration will be created which should be modified to work with your RPC settings.

The default configuration is as follows:

```yml
###################################
# Wallet API & Transactions
###################################

# RPC Wallet API Authentication, Please Change Defaults
user: vtcuser
pass: vtcpass
uri: http://127.0.0.1:5888

# Minimum Confirmations To Consider Deposits Valid
min-deposit-confirmations: 6

# Minimum Confirmations To Use Change Outputs
min-change-confirmations: 1

# Target Number Of Blocks To Confirm Withdrawals
target-block-time: 2

###################################
# Coin Information & Representation
###################################

# Coin Symbol
symbol: VTC
base-unit: sat

# Supported: base, micro, milli, full
scale: base

###################################
# General Plugin Behavior
###################################

# Experimental Vault Integration - Risk Of Lost Funds
vault-integration: false

# Automatically Configure Essentials Economy Commands If Found
configure-essentials: false

###################################
# Development Settings: CAREFUL
###################################

# SECURITY RISK: Enable H2 Web Console For DB Insight
enable-h2-console: false
```

The authentication parameters should be changed to match the wallet configuration.

With these configurations in place, the server should be ready to start with the Vault and Vertconomy JAR files placed in the server's `plugins` folder. Make sure the Vertcoin wallet is fully synced with the network and ready to use before starting the Minecraft server, it cannot return valid responses while loading.

The process for setting up cryptocurrencies other than Vertcoin is virtually identical, just customize the currency representation to match the crypto you choose and be aware the default port for the daemon varies between coins.

By default, Vertcoin balances will be represented in satoshi units; this can be changed in the plugin configuration. If your player base is new to cryptocurrency you may want to work on a smaller scale to make it easier for players to get involved.

## Warning
This project is experimental and does not guarantee the safety of any funds or wallets it interacts with.

As the project does not rely on any centralized server, please understand that any coins deposited through the plugin are inherently in the hands of the Minecraft server owners.\
From a user perspective, if you would not trust the server owners to hold your coins in their own personal wallets then you should not trust them to hold your coins through this plugin.

For server owners, be aware that the world of cryptocurrency is no stranger to attacks and scams.\
Take great care in your plugin selection and server management as malicious plugins and players with the right permissions can steal funds.\
By nature, any plugin that communicates with Vertconomy to transfer balances for useful functionality can also do so maliciously.\
Likewise, any player that can manage server funds, transfer player balances, or force other players to execute commands can do so maliciously.\
Once funds are withdrawn, there is no going back.\
*Be careful.*

## Contributions
Plugin written by [@mshernandez5](https://github.com/mshernandez5/) with special thanks to the projects making this one possible:
* The Vertcoin, Bitcoin, and Litecoin Projects
* Bukkit/Spigot, Vault API
* Dependencies... H2, Hibernate, Guice, Gson, Junit

## Donations
This plugin took time and effort, consider donating if you like the results!

Coin | Donation Address
-----|-----------------
VTC | vtc1qmzj8s3ss8f2uvjd0rdejju5t0n7wke4f5m8wrl