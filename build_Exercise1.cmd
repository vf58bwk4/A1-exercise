@echo on

set JC_HOME=C:\Java\java_card_kit-2_2_1
set ETSI_TS=C:\Java\ETSI_TS

set lib=%JC_HOME%\lib\api.jar
set lib=%ETSI_TS%\ts_143019v050600p0.jar;%lib%

set exp=%JC_HOME%\api_export_files\
set exp=%ETSI_TS%\exports\ts_143019v050600p0\;%exp%

set PKG=Exercise1
set PKG_AID=0x1:0x2:0x3:0x4:0x5:0x6:0x7:0x8:0x9:0x0
set AID=%PKG_AID%:0x1:0x2:0x3:0x4:0x5:0x6

javac -target 1.1 -source 1.3 -cp %lib% -d bin src\%PKG%\*.java

%JC_HOME%\bin\converter -classdir bin -d bin -exportpath %exp% -applet %AID% %PKG% -out CAP EXP JCA %PKG% %PKG_AID% 0.1
