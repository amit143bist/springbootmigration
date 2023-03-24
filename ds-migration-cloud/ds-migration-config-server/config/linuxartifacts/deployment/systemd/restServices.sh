#!/bin/bash
SCRIPTNAME="restServices.sh"

case $1 in
start)
echo starting coredata services ...
sudo systemctl daemon-reload
sudo systemctl start coredata@8290.service
sudo systemctl start coredata@8291.service
sudo systemctl start coredata@8292.service
sudo systemctl status |grep -i coredata

echo starting auditdata services ...
sudo systemctl start auditdata@8390.service
sudo systemctl start auditdata@8391.service
sudo systemctl start auditdata@8392.service
sudo systemctl start auditdata@8393.service
#sudo systemctl start auditdata@8394.service
sudo systemctl status |grep -i auditdata

echo starting prontodata services ...
sudo systemctl start prontodata@8490.service
sudo systemctl start prontodata@8491.service
sudo systemctl start prontodata@8492.service
sudo systemctl start prontodata@8493.service
sudo systemctl status |grep -i prontodata

echo starting authentication services ...
sudo systemctl start authentication@9290.service
sudo systemctl start authentication@9291.service
sudo systemctl start authentication@9292.service
sudo systemctl status |grep -i authentication

;;
stop)
echo stopping coredata services ...
sudo systemctl stop  coredata@8290.service
sudo systemctl stop coredata@8291.service
sudo systemctl stop coredata@8292.service

echo stopping auditdata services ...
sudo systemctl stop auditdata@8390.service
sudo systemctl stop auditdata@8391.service
sudo systemctl stop auditdata@8392.service
sudo systemctl stop auditdata@8393.service
#sudo systemctl stop auditdata@8394.service

echo stopping prontodata services ...
sudo systemctl stop prontodata@8490.service
sudo systemctl stop prontodata@8491.service
sudo systemctl stop prontodata@8492.service
sudo systemctl stop prontodata@8493.service

echo stopping authentication services ...
sudo systemctl stop authentication@9290.service
sudo systemctl stop authentication@9291.service
sudo systemctl stop authentication@9292.service

;;
restart)
echo restarting coredata services ...
sudo systemctl daemon-reload
sudo systemctl stop coredata@8290.service
sudo systemctl start coredata@8290.service
sudo systemctl stop coredata@8291.service
sudo systemctl start coredata@8291.service
sudo systemctl stop coredata@8292.service
sudo systemctl start coredata@8292.service
sudo systemctl status |grep -i coredata

echo restarting auditdata services ...
sudo systemctl stop auditdata@8390.service
sudo systemctl start auditdata@8390.service
sudo systemctl stop auditdata@8391.service
sudo systemctl start auditdata@8391.service
sudo systemctl stop auditdata@8392.service
sudo systemctl start auditdata@8392.service
sudo systemctl stop auditdata@8393.service
sudo systemctl start auditdata@8393.service
#sudo systemctl stop auditdata@8394.service
#sudo systemctl start auditdata@8394.service
sudo systemctl status |grep -i auditdata

echo restarting prontodata services ...
sudo systemctl stop prontodata@8490.service
sudo systemctl start prontodata@8490.service
sudo systemctl stop prontodata@8491.service
sudo systemctl start prontodata@8491.service
sudo systemctl stop prontodata@8492.service
sudo systemctl start prontodata@8492.service
sudo systemctl stop prontodata@8493.service
sudo systemctl start prontodata@8493.service
sudo systemctl status |grep -i prontodata

echo restarting authentication services ...
sudo systemctl stop authentication@9290.service
sudo systemctl start authentication@9290.service
sudo systemctl stop authentication@9291.service
sudo systemctl start authentication@9291.service
sudo systemctl stop authentication@9292.service
sudo systemctl start authentication@9292.service
sudo systemctl status |grep -i authentication

;;
*)
echo "Usage is: $SCRIPTNAME [start/stop]"
;;
esac