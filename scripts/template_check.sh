#!/bin/bash

source ~/.bash_profile

type ruby >/dev/null 2>&1 || { echo >&2 "I require ruby but it's not installed.  Aborting."; exit 1;}

cd /etc/puppetlabs/code/environments/CSESRE2087/pips_modules/pips_web/templates || exit
#cd /etc/puppetlabs/code/environments/ddv || exit
#cd /etc/puppetlabs/code/environments/production || exit

find . -name "*.erb" | while read -r val; do ERROR=$(exec  erb -x -T "-" "$val" | ruby -c  2>&1 >/dev/null); [[ -n $ERROR ]] && echo  "$ERROR" "for this file $val";done
