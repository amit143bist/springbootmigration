@echo off
set _JAVA_OPTIONS=-Xms256m -Xmx512m -XX:NewSize=64m -XX:MaxNewSize=64m -XX:MaxMetaspaceSize=128m -XX:CompressedClassSpaceSize=64m -Xss256k -XX:InitialCodeCacheSize=16m -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=32m -XX:-TieredCompilation -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=C:\textura\dumps -XX:OnOutOfMemoryError="shutdown -r" -XX:+UseGCOverheadLimit
set logFileMaxSize="300MB"

start "peer1 for ds-migration-eureka-server" cmd.exe /k java -jar -Dspring.profiles.active=peer1 -Dmig.peer1Port=8190 -Dmig.peer2Port=8191 -Dlogging.file=C:\textura\applogs\eurekaserver\8190.log -Dlogging.file.max-size=%logFileMaxSize% C:\textura\ds-migration-cloud\ds-migration-eureka-server\target\ds-migration-eureka-server-0.0.1-SNAPSHOT.jar

start "peer2 for ds-migration-eureka-server" cmd.exe /k java -jar -Dspring.profiles.active=peer2 -Dmig.peer1Port=8190 -Dmig.peer2Port=8191 -Dlogging.file=C:\textura\applogs\eurekaserver\8191.log -Dlogging.file.max-size=%logFileMaxSize% C:\textura\ds-migration-cloud\ds-migration-eureka-server\target\ds-migration-eureka-server-0.0.1-SNAPSHOT.jar

