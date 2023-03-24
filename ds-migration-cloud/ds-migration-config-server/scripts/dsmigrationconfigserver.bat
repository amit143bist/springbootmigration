@echo off

rem script parameters
set jardir=
set springApplicationName="dsmigrationconfigserver"
set jarFile="%jardir%/ds-migration-config-server-0.0.1-SNAPSHOT.jar"
set proxyHost="[proxyHost]"
set proxyPort="[proxyPort]"
set serverPort="[serverPort]"
set logFileMaxSize="300MB"
set logfilePath="%jardir%\\%serverPort%.log"

echo Starting the %springApplicationName% microservice

rem execute
java -jar -Dserver.port=%serverPort% -Dlogging.file=%logfilePath% -Dlogging.file.max-size=%logFileMaxSize% -Dhttp.proxyHost=%proxyHost% -Dhttp.proxyPort=%proxyPort% -Dhttps.proxyHost=%proxyHost% -Dhttps.proxyPort=%proxyPort% %jarFile