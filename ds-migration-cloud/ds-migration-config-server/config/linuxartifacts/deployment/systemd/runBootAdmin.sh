#!/bin/bash
SCRIPTNAME="runBootAdmin.sh"

case $1 in
start)
echo starting bootadmin@8010.service ...
sudo systemctl daemon-reload
sudo systemctl start bootadmin@8010.service
sudo systemctl -l status bootadmin@8010

echo starting migrationadmin@8020.service ...
sudo systemctl start migrationadmin@8020.service
sudo systemctl -l status migrationadmin@8020
;;
stop)
echo stopping bootadmin@8010.service ...
sudo systemctl stop bootadmin@8010.service
sudo systemctl -l status bootadmin@8010

echo stopping migrationadmin@8020.service ...
sudo systemctl stop migrationadmin@8020.service
sudo systemctl -l status migrationadmin@8020
;;
restart)
echo restarting bootadmin@8010.service ...
sudo systemctl daemon-reload
sudo systemctl stop bootadmin@8010
sudo systemctl start bootadmin@8010.service
sudo systemctl -l status bootadmin@8010

echo restarting migrationadmin@8020.service ...
sudo systemctl stop migrationadmin@8020.service
sudo systemctl start migrationadmin@8020.service
sudo systemctl -l status migrationadmin@8020
;;
*)
echo "Usage is: $SCRIPTNAME [start/stop]"
;;
esac