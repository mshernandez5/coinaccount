# <img src="logo.svg" alt="CoinAccount Logo" height="75px">

## Early Development Notice
CoinAccount is still in relatively early development and is subject to large changes which may include database schema changes.

## What is CoinAccount?
CoinAccount forms a layer of abstraction over a core wallet that allows you to easily manage accounts with the ability to deposit, withdraw, and transfer funds internally. This allows you to quickly create services such as tip bots without worrying about managing the underlying wallet.

This project grew out of an attempt to bring Vertcoin to Minecraft through a server plugin named Vertconomy (reflected in shared git history of these projects). The plugin quickly took on more responsibility than one should, leading to a separation of responsibilities. The responsibilities once held by the plugin alone were split between CoinAccount and a new [Vertconomy-MC](https://github.com/mshernandez5/vertconomy-mc) plugin which relies on CoinAccount for wallet and account management.

## How can programs interact with it?
CoinAccount defines its API in language-neutral [protocol buffers](https://developers.google.com/protocol-buffers). Since the project uses [gRPC](https://grpc.io/) for communication, you can compile client stubs to interact with CoinAccount in the language of your choice.

The protobuf API is located in the `coinaccount-api` module, while the service implementations are in the `coinaccount-service` module.

## How can I run CoinAccount?
CoinAccount requires both a core wallet and MariaDB database to be running at all times, preferably configured as startup system services.

For more details on setting up vertcoind as a systemd service, refer to [this guide](https://gist.github.com/mshernandez5/325c192b449b27174641b2d3c2142f14#file-vertcoin-node-service-md).

## Wallet RPC Configuration
First, you will need a dedicated wallet configured to listen for RPC connections. Do not use an existing wallet.

These steps will use Vertcoin as an example but they are applicable to practically all Bitcoin derivatives by replacing "vertcoin" with "bitcoin", etc. The configuration file for Vertcoin core can be found at one of the following locations if the configuration directory was not manually set:

Operating System | Location
-----------------|---------
Windows | `%appdata%\vertcoin\vertcoin.conf`
Linux: | `~/.vertcoin/vertcoin.conf`

If the configuration file does not exist - which is likely if you never needed to edit it before - create it.

Here is an example configuration that will allow the wallet to accept RPC connections at `http://127.0.0.1:5888` using the specified username and password:

> ***Warning:*** *Do not allow outside connections to the selected port as anyone that can send RPC requests to the wallet has full control of its funds. The username and password don't have much value since the wallet works over an unencrypted HTTP connection.*

```properties
server=1
rpcuser=vtcuser
rpcpassword=vtcpass
rpcport=5888
```

Please change the username and password to non-default values. After saving the new configuration, the changes will take effect after starting `vertcoind` (recommended, headless) or `vertcoin-qt` (graphical interface).

Make sure to let the node fully sync before using CoinAccount. Be aware that a withdraw request may fail if the node has not been running long enough regardless of whether or not it has caught up with the blockchain. Without running long enough the node will not be able to provide fee estimates to CoinAccount which are essential to withdrawals. This is especially relevant to smaller coins which do not have a large volume of transactions to use as a basis for fees.

## MariaDB Configuration
You will need to install MariaDB to run CoinAccount. For Linux users, it is available in the default repositories for most distributions.

Once installed, you must take a few steps to make the database accessible to CoinAccount:

1. Create a new user named `coinaccount`.
2. Create a new database named `coinaccountdb`.
3. Grant all permissions for `coinaccountdb` to the new `coinaccount` user.

These steps can be completed by running the following SQL statements, replacing `password` with a real password:
```sql
CREATE USER coinaccount IDENTIFIED BY 'password';
CREATE DATABASE coinaccountdb;
GRANT ALL PRIVILEGES ON coinaccountdb.* TO coinaccount;
FLUSH PRIVILEGES;
```
If you installed MariaDB from the Ubuntu repositories, note that logging-in to the root user initially requires starting the console with root privileges rather than a predefined password:
```bash
sudo mysql -u root
```

## CoinAccount Configuration
CoinAccount will need to know how to connect to the wallet and database, so you must create a configuration file with this information. In the same directory as the application JAR, there should be a directory `config` containing a file `application.properties` with the following contents:

```properties
############################################################################
#
#                ##                                                      ##
#                ##                                                      ##
#                                                                        ##
#  ####   ###   ###    # ##    ####   ####   ####   ###   ## ##  # ##   ####
# ##     ## ##   ##    ## ##  ## ##  ##     ##     ## ##  ## ##  ## ##   ##
# ##     ## ##   ##    ## ##  ## ##  ##     ##     ## ##  ## ##  ## ##   ##
# ##     ## ##   ##    ## ##  ## ##  ##     ##     ## ##  ## ##  ## ##   ##
#  ####   ###   ####   ## ##   ## #   ####   ####   ###    ## #  ## ##    ##
#
# Configuration File
############################################################################

############################################################################
# gRPC Service Configuration
############################################################################

# CoinAccount Service Hosting
quarkus.grpc.server.host: 127.0.0.1
quarkus.grpc.server.port: 3030

############################################################################
# Database Configuration - MariaDB Connection
############################################################################

# Database Connection URL
quarkus.datasource.jdbc.url: jdbc:mariadb://localhost:3306/coinaccountdb

# Database Credentials
quarkus.datasource.username: coinaccount
quarkus.datasource.password: dbpassword

############################################################################
# Wallet RPC Configuration - Match to vertcoin.conf or equivalent settings.
############################################################################

# Wallet RPC Address, Including Port
coinaccount.wallet.address: http://127.0.0.1:5888

# Wallet RPC Credentials
coinaccount.wallet.user: vtcuser
coinaccount.wallet.pass: vtcpass

############################################################################
# Web Interface - Shows CoinAccount status information.
############################################################################

# HTTP Server Port
quarkus.http.port: 5050

############################################################################
# Currency Representation - Customize to match the connected wallet.
############################################################################

# Coin Symbol, ex. VTC
coinaccount.coin.symbol: VTC

# Base Unit Symbol, ex. sat
coinaccount.coin.base.symbol: vertoshi

############################################################################
# UTXO Management - Recommended to leave as-is.
############################################################################

# Minimum Amount For Deposit Not To Be Ignored
coinaccount.deposit.minimum: 50000000

# Minimum Number Of Confirmations To Consider User Deposit UTXOs Valid
coinaccount.deposit.confirmations: 6

# Minimum Number Of Confirmations To Consider Change UTXOs Valid
coinaccount.change.confirmations: 1

# The Default Address Type (P2WPKH, P2SH_P2WPKH, P2PKH)
coinaccount.address.type: P2WPKH

# Whether Change Addresses Should Be Reused
coinaccount.address.change.reuse: false

# Whether User Deposit Addresses Should Be Reused
coinaccount.address.user.reuse: false

############################################################################
# Withdrawals - Recommended to leave as-is.
############################################################################

# Block Confirmation Target For Withdrawals, Affects Fees
coinaccount.withdraw.target: 2

# Time In Milliseconds For A Withdraw Request To Expire
coinaccount.withdraw.expire: 60000

############################################################################
# CoinAccount Internal Settings - Recommended to leave as-is.
############################################################################

# How Often To Check For New Account Deposits
coinaccount.deposit.check: 10s

# How Often To Check For Expired Withdraw Requests
coinaccount.withdraw.expire.check: 5s
```

If you built CoinAccount from source then this configuration will not exist and must be created.

Make sure to change the following properties:

Property | Value
-----------------|---------
`quarkus.datasource.password` | The password given to the `coinaccount` database user.
`coinaccount.wallet.address` | The wallet RPC URI including the port specified in the wallet configuration, ex. `http://127.0.0.1:5888`
`coinaccount.wallet.user` | The username defined in the wallet RPC configuration. 
`coinaccount.wallet.pass` | The password defined in the wallet RPC configuration.
`coinaccount.coin.symbol` | The symbol representing the coin in use, ex. `BTC`
`coinaccount.coin.base.symbol` | A name for the most basic unit of currency, ex. `sat`

## Packaging & Running CoinAccount

The application can be packaged and installed to the local repository using:
```shell script
mvn clean install
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the required dependencies are stored in the `target/quarkus-app/lib/` directory.

If you want to build an _über-jar_, execute the following command:
```shell script
mvn clean install -Dquarkus.package.type=uber-jar
```

The application may be run using `java -jar quarkus-run.jar` assuming a valid configuration file has been created.

## Donations
This software took some time and effort, please consider donating if you like the results!

Coin | Donation Address
-----|-----------------
VTC | vtc1q46jdvsmvwmxmmya47639pxnwz807rdqcnjv82h