# Vertconomy
A plugin integrating Vertcoin into the Minecraft economy through the Vault API.

## Why Vertcoin?
[Vertcoin](https://vertcoin.org/) is committed to maintaining an ASIC-resistant algorithm, hard forking whenever necessary to continue doing so. Keeping dedicated mining hardware off the table makes it possible to mine with consumer-grade GPUs in normal gaming computers. With the [Vertcoin One Click Miner](https://github.com/vertcoin-project/one-click-miner-vnext/releases), people can easily get involved and start mining cryptocurrency without having to purchase any directly. You can even mine Vertcoin while playing Minecraft! Though this plugin is not exclusively compatible with Vertcoin, most other coins would require players to purchase coins to participate in the server economy.

## How does it work?
Vertconomy interfaces with a user-provided wallet via standard [bitcoind RPC methods](https://developer.bitcoin.org/reference/rpc/), allowing a player to deposit and withdraw Vertcoin through the Minecraft plugin.

All player balances are held as part of the same wallet with an embedded database keeping track of specific player balances for zero-fee transfers between players within the server.

Minecraft [Vault API](https://github.com/MilkBowl/VaultAPI) integration enables Vertcoin support across a wide variety of existing plugins.

## Does it work with existing plugins?
Vertconomy aims to provide at least limited compatibility with existing plugins designed to work with the Vault API. Unfortunately, Minecraft plugins are built on the (fair) assumption that in-game currency can be created out of thin air and sent back into the void at will. Vertconomy works around these assumptions by transforming the money void into a server-owned account and rejecting the creation of currency beyond that available as excess funds within the account. This general idea ensures that the existing currency owned by players must be bound to real wallet funds. Of course, a few more tricks go into the implementation as some external plugins internally interact with player balances in complicated ways.

## How can I use this plugin?
You will need a dedicated Vertcoin wallet configured to listen for RPC connections. Do not use an existing Vertcoin wallet. The configuration file for Vertcoin wallet can be found at one of the following locations if the wallet configuration directory was not manually changed:

Operating System | Location
-----------------|---------
Windows | `%appdata%\vertcoin\vertcoin.conf`
Linux: | `~/.vertcoin/vertcoin.conf`

\
If the configuration file does not exist - which is likely if you never needed to edit it before - create it.
Here is an example configuration that will allow the wallet to accept RPC connections at `http://127.0.0.1:5888` using the specified username and password:

```
server=1
rpcuser=vtcuser
rpcpassword=vtcpass
rpcport=5888
```

Please change the username and password to non-default values. After saving the new configuration, the changes will take effect after starting `vertcoind` (recommended, headless) or `vertcoin-qt` (graphical interface).

The plugin will need to know how to connect to the wallet, so you will also need a plugin configuration to match in `plugins/Vertconomy/config.yml` relative to the Minecraft server directory.

The default configuration is as follows:

```yml
###################################
# Wallet API & Transactions
###################################

# RPC Wallet API Authentication, Please Change Defaults
user: vtcuser
pass: vtcpass
uri: http://127.0.0.1:5888

# Minimum Confirmations To Consider Transaction Valid
min-confirmations: 10

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

By default, Vertcoin balances will be represented in satoshi units; this can be changed in the plugin configuration. I would not recommend using full VTC units as this encourages larger in-game transactions and may make the game less enjoyable for those without large amounts of Vertcoin to experiment with.

## Warning
This project is experimental and does not make any guarantees to the safety of the funds or wallets it interacts with. As the project does not rely on any centralized server, please understand that any coins deposited though the plugin are inherently in the hands of the Minecraft server owners. If you would not trust the server owners to hold your coins directly, you should not trust them to hold your coins through this plugin.

## Contributions
Plugin written by [@mshernandez5](https://github.com/mshernandez5/) with special thanks to the projects making this one possible:
* The Vertcoin, Bitcoin, and Litecoin Projects
* Bukkit/Spigot, Vault API
* Libraries... H2, Hibernate, Gson, Junit

## Donations
This plugin took time and effort, consider donating if you like the results!

Coin | Donation Address
-----|-----------------
VTC | vtc1qmzj8s3ss8f2uvjd0rdejju5t0n7wke4f5m8wrl