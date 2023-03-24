#!/bin/bash
SCRIPTNAME="runEurekaServers.sh"

case $1 in
start)
echo starting
sudo systemctl daemon-reload
sudo systemctl start eurekaserver1.service
sudo systemctl start eurekaserver2.service
sudo systemctl status eurekaserver1.service
sudo systemctl status eurekaserver2.service
;;
stop)
echo stopping
sudo systemctl stop eurekaserver1.service
sudo systemctl stop eurekaserver2.service
;;
restart)
echo restarting
sudo systemctl daemon-reload
sudo systemctl stop eurekaserver1.service
sudo systemctl stop eurekaserver2.service
sudo systemctl start eurekaserver1.service
sudo systemctl start eurekaserver2.service
sudo systemctl status eurekaserver1.service
sudo systemctl status eurekaserver2.service
;;
*)
echo "Usage is: $SCRIPTNAME [start/stop]"
;;
esac