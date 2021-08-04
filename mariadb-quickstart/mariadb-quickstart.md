# MariaDB Quickstart
You will need to configure an installation of MariaDB before running CoinAccount.

This includes:
* Creating a user named `coinaccount`
* Creating a database named `coinaccountdb`
* Granting all permissions for `coinaccountdb` to `coinaccount`

Of course, the user and database names can be customized if the CoinAccount configuration is updated respectively.

Not familiar with MariaDB and need to configure a brand new installation?

This directory contains a script to setup an account named `coinaccount` and a database named `coinaccountdb` the user has full permissions for.

Just make sure you have installed MariaDB and the `mysql` command is available before running the script.

# Linux
Navigate to this directory then run `./mariadb-quickstart.sh` and follow the prompts.

Note the password that you give to the `coinaccount` user as you will need to provide this in the CoinAccount database configuration.

## Why does the script ask to run a command with sudo?
New MariaDB installations initially only have a root account which must be accessed as a superuser. Once the script uses this account to create a `coinaccount` database user with less elevated permissions then CoinAccount can access the database normally.