#!/bin/sh
# Deploy in local containers:
# ./run.sh
#
# Deploy a specific playbook on a specific env:
# INVENTORY=env/inventory PLAYBOOK=playbook.yml ./run.sh
set -eux

#if [ -z "${VIRTUAL_ENV-}" ]; then
#    Please activate a virtualenv
#    exit 1
#fi

ansible-galaxy install --role-file requirements.yml --roles-path roles

#[ $USER = jenkins ] && user="--user jenkins-back" || user=""

ansible-playbook -v --inventory ${INVENTORY-local/inventory} ${PLAYBOOK-local.yml} # $user
