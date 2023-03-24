@echo off
set _JAVA_OPTIONS=-Xms512m -Xmx2048m -XX:NewSize=128m -XX:MaxNewSize=256m -XX:MaxMetaspaceSize=256m -XX:CompressedClassSpaceSize=256m -Xss512k -XX:InitialCodeCacheSize=256m -XX:ReservedCodeCacheSize=256m -XX:MaxDirectMemorySize=256m -XX:-TieredCompilation -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=C:\textura\dumps -XX:OnOutOfMemoryError="shutdown -r" -XX:+UseGCOverheadLimit
set logFileMaxSize="300MB"


start "ds-migration-website_9490" cmd.exe /k java -jar -Dserver.port=9490 -Dlogging.file=C:\textura\applogs\migrationwebsite\9490.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-website\target\ds-migration-website-0.0.1-SNAPSHOT.jar

start "ds-migration-website_9491" cmd.exe /k java -jar -Dserver.port=9491 -Dlogging.file=C:\textura\applogs\migrationwebsite\9491.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-website\target\ds-migration-website-0.0.1-SNAPSHOT.jar