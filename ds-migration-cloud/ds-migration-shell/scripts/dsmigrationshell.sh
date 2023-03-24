#!/bin/bash

# script parameters
set jardir=
set springApplicationName="dsmigrationcoredata"
set jarFile="$jardir/ds-migration-coredata-0.0.1-SNAPSHOT.jar"
set proxyHost="[proxyHost]"
set proxyPort="[proxyPort]"
set serverPort="[serverPort]"
set springBootProfileName="dev"
set logFileMaxSize="300MB"
set logfilePath="$jardir\\$serverPort.log"
set commandFilePath="$jardir\\shellcommand.txt"

# print parameters
echo Starting the $springApplicationName microservice

# execute
java -jar -Dserver.port=$serverPort -Dlogging.file=$logfilePath -Dlogging.file.max-size=$logFileMaxSize -Dspring-boot.run.profiles=$springBootProfileName -Dhttp.proxyHost=$proxyHost -Dhttp.proxyPort=$proxyPort -Dhttps.proxyHost=$proxyHost -Dhttps.proxyPort=$proxyPort $jarFile @$commandFilePath