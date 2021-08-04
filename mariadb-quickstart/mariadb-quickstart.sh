#!/bin/bash

# Prompt For New Account Password
printf "Creating a new database account 'coinaccount', please give it a password:\n\n"
read -s -p "Password: " password
printf "\n"
read -s -p "Enter Again: " password_again
printf "\n\n"

# Make Sure Passwords Match
if [ "$password" != "$password_again" ]; then
    printf "Error: Passwords don't match!\n"
    exit 1
fi

# Replace Password Placeholder In SQL Script & Stream Commands 
sed "s/{PASSWORD}/$password/" MariaDBQuickstartTemplate.sql | sudo mysql -u root

# Completed
printf "\nCompleted"