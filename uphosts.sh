#!/bin/bash
cd ../builds/"${BUILD_NUMBER}" || exit
grep "unreachable=0" log  | awk '{ print $1 }' > "${WORKSPACE}"/QA_Inventory_UP
