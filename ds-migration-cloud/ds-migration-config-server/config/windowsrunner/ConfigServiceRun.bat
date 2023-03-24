@echo off
set _JAVA_OPTIONS=-Xms128m -Xmx256m -XX:NewSize=64m -XX:MaxNewSize=64m -XX:MaxMetaspaceSize=128m -XX:CompressedClassSpaceSize=64m -Xss256k -XX:InitialCodeCacheSize=16m -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=32m -XX:-TieredCompilation -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=C:\textura\dumps -XX:OnOutOfMemoryError="shutdown -r" -XX:+UseGCOverheadLimit
set logFileMaxSize="300MB"

start "ds-migration-config-server" cmd.exe /k java -jar -Dserver.port=8090 -Dlogging.file=C:\textura\applogs\configserver\8090.log -Dlogging.file.max-size=%logFileMaxSize% -Dcloud.config.searchLocations=/C:/textura/config/ C:\textura\ds-migration-cloud\ds-migration-config-server\target\ds-migration-config-server-0.0.1-SNAPSHOT.jar