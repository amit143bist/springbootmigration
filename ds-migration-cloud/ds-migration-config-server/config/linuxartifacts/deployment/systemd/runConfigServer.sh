#!/bin/bash
SCRIPTNAME="runConfigServer.sh"

case $1 in
start)
echo starting configserver@8090 ...
sudo systemctl daemon-reload
sudo systemctl start configserver@8090.service
sudo systemctl -l status configserver@8090
;;
stop)
echo stopping configserver@8090 ...
sudo systemctl stop configserver@8090.service
sudo systemctl -l stop configserver@8090
;;
restart)
echo restarting configserver@8090 ...
sudo systemctl daemon-reload
sudo systemctl stop configserver@8090.service
sudo systemctl start configserver@8090.service
sudo systemctl -l status configserver@8090
;;
*)
echo "Usage is: $SCRIPTNAME [start/stop]"
;;
esac