# <img src="logo.svg" alt="CoinAccount Logo" height="75px">

## What is CoinAccount?
CoinAccount forms a layer over the standard core wallet allowing you to create accounts, each holding shares of unspent outputs.

This project grew out of an attempt to bring Vertcoin to Minecraft through a server plugin named Vertconomy (reflected in shared git history of these projects) which quickly resulted in a plugin that was more complicated and took on more responsibility than one should. The responsibilities once held by the plugin alone were split between CoinAccount and a new [Vertconomy-MC](https://github.com/mshernandez5/vertconomy-mc) plugin which relies on CoinAccount for wallet and account management.

## How does it work?
When an account receives a deposit to their address they will initially own the entire portion of the newly created UTXO. Rather than treating account balances as a promise for withdrawals of equal value, CoinAccount binds balances to specific outputs with defined owners. In this case, the deposited UTXO is accessible only by the depositing account and will not be spent or otherwise used by CoinAccount unless the account takes additional actions.

If the account chooses to transfer a portion of their balance to another account - without fees since all accounts use the same underlying wallet - then a portion of their deposited UTXO will be allocated to the receiving account. Both accounts now have shares in the deposit and either can now transfer or withdraw their shares.

In cases where an unspent output is distributed among multiple accounts and one of the accounts wishes to withdraw a portion of their share, CoinAccount will receive the change output and internally distribute it according to the remaining shares of the now spent output. It takes one confirmation before the change is accepted and other accounts are allowed to withdraw their remaining shares. Accounts may transfer balances internally between each other at all times, including during pending withdrawals.

## How can programs interact with it?
CoinAccount defines its API in language-neutral [protocol buffers](https://developers.google.com/protocol-buffers). Since the project uses [gRPC](https://grpc.io/) for communication, you can compile client stubs to interact with CoinAccount in the language of your choice.

The protobuf API is located in the `coinaccount-api` module, while the service implementations are in the `coinaccount-service` module.

## How can I run CoinAccount?
CoinAccount requires both a core wallet and MariaDB database to be running at all times, preferably configured as system services to run at startup.

## Wallet RPC Configuration
First, you will need a dedicated wallet configured to listen for RPC connections. Do not use an existing wallet.

These steps will use Vertcoin as an example but they are applicable to practically all Bitcoin derivatives replacing "vertcoin" with "bitcoin", etc. The configuration file for Vertcoin core can be found at one of the following locations if the configuration directory was not manually changed:

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

If you are not sure how to complete these steps then the [MariaDB quickstart script](mariadb-quickstart/mariadb-quickstart.md) included in this project should help you take care of them.

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