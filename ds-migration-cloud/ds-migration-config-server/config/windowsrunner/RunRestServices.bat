@echo off
set _JAVA_OPTIONS=-Xms512m -Xmx2048m -XX:NewSize=64m -XX:MaxNewSize=128m -XX:MaxMetaspaceSize=128m -XX:CompressedClassSpaceSize=256m -Xss512k -XX:InitialCodeCacheSize=128m -XX:ReservedCodeCacheSize=256m -XX:MaxDirectMemorySize=256m -XX:-TieredCompilation -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=C:\textura\dumps -XX:OnOutOfMemoryError="shutdown -r" -XX:+UseGCOverheadLimit
set logFileMaxSize="500MB"

start "ds-migration-coredata_8290" cmd.exe /k java -jar -Dserver.port=8290 -Dlogging.file=C:\textura\applogs\coredata\8290.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-coredata\target\ds-migration-coredata-0.0.1-SNAPSHOT.jar

start "ds-migration-coredata_8291" cmd.exe /k java -jar -Dserver.port=8291 -Dlogging.file=C:\textura\applogs\coredata\8291.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-coredata\target\ds-migration-coredata-0.0.1-SNAPSHOT.jar

start "ds-migration-coredata_8292" cmd.exe /k java -jar -Dserver.port=8292 -Dlogging.file=C:\textura\applogs\coredata\8292.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-coredata\target\ds-migration-coredata-0.0.1-SNAPSHOT.jar

start "ds-migration-auditdata_8390" cmd.exe /k java -jar -Dserver.port=8390 -Dlogging.file=C:\textura\applogs\auditdata\8390.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-auditdata\target\ds-migration-auditdata-0.0.1-SNAPSHOT.jar

start "ds-migration-auditdata_8391" cmd.exe /k java -jar -Dserver.port=8391 -Dlogging.file=C:\textura\applogs\auditdata\8391.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-auditdata\target\ds-migration-auditdata-0.0.1-SNAPSHOT.jar

start "ds-migration-auditdata_8392" cmd.exe /k java -jar -Dserver.port=8392 -Dlogging.file=C:\textura\applogs\auditdata\8392.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-auditdata\target\ds-migration-auditdata-0.0.1-SNAPSHOT.jar

start "ds-migration-auditdata_8393" cmd.exe /k java -jar -Dserver.port=8393 -Dlogging.file=C:\textura\applogs\auditdata\8393.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-auditdata\target\ds-migration-auditdata-0.0.1-SNAPSHOT.jar

start "ds-migration-auditdata_8394" cmd.exe /k java -jar -Dserver.port=8394 -Dlogging.file=C:\textura\applogs\auditdata\8394.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-auditdata\target\ds-migration-auditdata-0.0.1-SNAPSHOT.jar

start "ds-migration-prontodata_8490" cmd.exe /k java -jar -Dserver.port=8490 -Dlogging.file=C:\textura\applogs\prontodata\8490.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-prontodata\target\ds-migration-prontodata-0.0.1-SNAPSHOT.jar

start "ds-migration-prontodata_8491" cmd.exe /k java -jar -Dserver.port=8491 -Dlogging.file=C:\textura\applogs\prontodata\8491.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-prontodata\target\ds-migration-prontodata-0.0.1-SNAPSHOT.jar

start "ds-migration-prontodata_8492" cmd.exe /k java -jar -Dserver.port=8492 -Dlogging.file=C:\textura\applogs\prontodata\8492.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-prontodata\target\ds-migration-prontodata-0.0.1-SNAPSHOT.jar

start "ds-migration-authentication_9290" cmd.exe /k java -jar -Dserver.port=9290 -Dlogging.file=C:\textura\applogs\authentication\9290.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-authentication\target\ds-migration-authentication-0.0.1-SNAPSHOT.jar

start "ds-migration-authentication_9291" cmd.exe /k java -jar -Dserver.port=9291 -Dlogging.file=C:\textura\applogs\authentication\9291.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-authentication\target\ds-migration-authentication-0.0.1-SNAPSHOT.jar

start "ds-migration-authentication_9292" cmd.exe /k java -jar -Dserver.port=9292 -Dlogging.file=C:\textura\applogs\authentication\9292.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-authentication\target\ds-migration-authentication-0.0.1-SNAPSHOT.jar

start "ds-migration-boot-admin" cmd.exe /k java -jar -Dserver.port=8010 -Dlogging.file=C:\textura\applogs\bootadmin\8010.log -Dlogging.file.max-size=%logFileMaxSize% -Dspring.profiles.active=dev -Dmig.peer1-address=peer1:8190 -Dmig.peer2-address=peer2:8191 C:\textura\ds-migration-cloud\ds-migration-boot-admin\target\ds-migration-boot-admin-0.0.1-SNAPSHOT.jar