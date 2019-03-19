#!/bin/bash

source ~/.bash_profile
type ruby >/dev/null 2>&1 || { echo >&2 "I require ruby but it's not installed.  Aborting."; exit 1;}
#cd /etc/puppetlabs/code/hieradata
cd /etc/puppetlabs/code/hieradata/infra || exit
#for i in `find . -name '*.yaml'`; do echo $i; ruby -e "require 'yaml'; YAML.parse(File.open('$i'))"; done
for i in $(find . -name '*.yaml'); do ruby -e "require 'yaml'; YAML.parse(File.open('$i'))"; done
