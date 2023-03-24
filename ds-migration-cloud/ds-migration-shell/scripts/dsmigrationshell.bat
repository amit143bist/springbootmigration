@echo off

rem script parameters
set jardir=
set springApplicationName="dsmigrationshell"
set jarFile="%jardir%/ds-migration-shell-0.0.1-SNAPSHOT.jar"
set proxyHost="[proxyHost]"
set proxyPort="[proxyPort]"
set serverPort="[serverPort]"
set springBootProfileName="dev"
set logFileMaxSize="300MB"
set logfilePath="%jardir%\\%serverPort%.log"
set commandFilePath="%jardir%\\shellcommand.txt"

echo Starting the %springApplicationName% microservice

rem execute
java -jar -Dlogging.file=%logfilePath% -Dlogging.file.max-size=%logFileMaxSize% -Dspring-boot.run.profiles=%springBootProfileName% -Dhttp.proxyHost=%proxyHost% -Dhttp.proxyPort=%proxyPort% -Dhttps.proxyHost=%proxyHost% -Dhttps.proxyPort=%proxyPort% %jarFile @%commandFilePath%