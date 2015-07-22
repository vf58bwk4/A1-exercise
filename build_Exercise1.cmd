@echo on

set PKG_AID=0x1:0x2:0x3:0x4:0x5:0x6:0x7:0x8:0x9:0x0
set AID=%PKG_AID%:0x1:0x2:0x3:0x4:0x5:0x6

javac -target 1.1 -source 1.3 -cp jars\SUN_JavaCard_2-2-1\jc_SUN_JavaCard_2-2-1.jar;jars\ETSI_43019_6-0-0\jc_ETSI_43019_6-0-0.jar Exercise1\*.java

set JC_HOME=%JC22_HOME%
%JC22_HOME%\bin\converter -exportpath exports\SUN_JavaCard_2-2-1;exports\ETSI_43019_6-0-0 -applet %AID% Exercise1 -out CAP EXP JCA Exercise1 %PKG_AID% 0.1

