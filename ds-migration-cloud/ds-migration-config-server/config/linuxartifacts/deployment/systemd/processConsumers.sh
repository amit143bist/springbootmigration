#!/bin/bash
SCRIPTNAME="processConsumer.sh"

case $1 in
start)
echo starting processcompleteconsumer@8890.service ...
sudo systemctl daemon-reload
sudo systemctl start processcompleteconsumer@8890.service
sudo systemctl status processcompleteconsumer@8890

echo starting processstartconsumer@8690.service ...
sudo systemctl start processstartconsumer@8690.service
sudo systemctl status processstartconsumer@8690

echo starting auditdataconsumer@8590.service ...
sudo systemctl start auditdataconsumer@8590.service
sudo systemctl status auditdataconsumer@8590

echo starting prontodataconsumer@8990.service ...
sudo systemctl start prontodataconsumer@8990.service
sudo systemctl status prontodataconsumer@8990

echo starting recorddataconsumer@9090.service ...
sudo systemctl start recorddataconsumer@9090.service
sudo systemctl status recorddataconsumer@9090

echo starting processfailureconsumer@8790.service ...
sudo systemctl start processfailureconsumer@8790.service
sudo systemctl status processfailureconsumer@8790

echo starting batchtriggerconsumer@9190.service...
sudo systemctl start batchtriggerconsumer@9190.service
sudo systemctl status batchtriggerconsumer@9190.service

;;
stop)
echo stopping processcompleteconsumer@8890.service ...
sudo systemctl daemon-reload
sudo systemctl stop processcompleteconsumer@8890.service

echo stopping processstartconsumer@8690.service ...
sudo systemctl stop processstartconsumer@8690.service

echo stopping auditdataconsumer@8590.service ...
sudo systemctl stop auditdataconsumer@8590.service

echo stopping prontodataconsumer@8990.service ...
sudo systemctl stop prontodataconsumer@8990.service

echo stopping recorddataconsumer@9090.service ...
sudo systemctl stop recorddataconsumer@9090.service

echo stopping processfailureconsumer@8790.service ...
sudo systemctl stop processfailureconsumer@8790.service

echo stopping batchtriggerconsumer@9190.service...
sudo systemctl stop batchtriggerconsumer@9190.service
;;
restart)
echo restarting processcompleteconsumer@8890.service ...
sudo systemctl daemon-reload
sudo systemctl stop processcompleteconsumer@8890.service
sudo systemctl start processcompleteconsumer@8890.service
sudo systemctl status processcompleteconsumer@8890

echo restarting processstartconsumer@8690.service ...
sudo systemctl stop processstartconsumer@8690.service
sudo systemctl start processstartconsumer@8690.service
sudo systemctl status processstartconsumer@8690

echo restarting auditdataconsumer@8590.service ...
sudo systemctl stop auditdataconsumer@8590.service
sudo systemctl start auditdataconsumer@8590.service
sudo systemctl status auditdataconsumer@8590

echo restarting prontodataconsumer@8990.service ...
sudo systemctl stop prontodataconsumer@8990.service
sudo systemctl start prontodataconsumer@8990.service
sudo systemctl status prontodataconsumer@8990

echo restarting recorddataconsumer@9090.service ...
sudo systemctl stop recorddataconsumer@9090.service
sudo systemctl start recorddataconsumer@9090.service
sudo systemctl status recorddataconsumer@9090

echo restarting processfailureconsumer@8790.service ...
sudo systemctl stop processfailureconsumer@8790.service
sudo systemctl start processfailureconsumer@8790.service
sudo systemctl status processfailureconsumer@8790

echo restarting batchtriggerconsumer@9190.service...
sudo systemctl stop batchtriggerconsumer@9190.service
sudo systemctl start batchtriggerconsumer@9190.service
sudo systemctl status batchtriggerconsumer@9190
;;
*)
echo "Usage is: $SCRIPTNAME [start/stop]"
;;
esac