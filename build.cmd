@echo on

set PKG_AID="0xd2:0x76:0x00:0x01:0x24:0x01:0x01:0x01:0xff:0xff:0x00:0x00:0x00:0x01:0x00"
set AID=%PKG_AID%:0x00

javac -target 1.1 -source 1.3 -cp %JC22_HOME%\lib\api.jar;43.019 Exercise1\*.java

set JC_HOME=%JC22_HOME%
%JC22_HOME%\bin\converter -exportpath %JC22_HOME%\api_export_files;43.019 -applet %AID% Exercise1 -out CAP EXP Exercise1 %PKG_AID% 0.1
                           