@echo off

@rem ***
@rem *** Configuration params for the service are below. You must reinstall the service after editing these.
@rem *** For more information on these parameters run ViewConfigurationHelp.bat (or java -jar dcmrcv.jar)
@rem ***

set     CF= STORESCP:104 
set CF=%CF% -dest D://UltrasoundData
set CF=%CF% -zipsend=on
set CF=%CF% -emailfrom=ultrasound-dicom@mydomain.com
set CF=%CF% -emailto=ultrasound-dicom@mydomain.com
set CF=%CF% -emailsmtpserver=mail.mydomain.com
set CF=%CF% -emailuser=ultrasound-dicom+mydomain.com
set CF=%CF% -emailpassword=mypassword
set CF=%CF% -ziptimeout=30000

set SERVICE_DESCRIPTION=A service to receive DICOM images from the ultrasound scanner and send them to Bluespier



@rem ***
@rem *** File locations
@rem ***

setlocal
@rem note that if JVM not found, service 'does not report an error' when startup fails, although event logged
if "%JAVA_HOME%" == "" set JAVA_HOME=C:\Program Files\Java\jre6
set JVMDIR=%JAVA_HOME%\bin\client
set JSBINDIR=%CD%
set JSEXE=%JSBINDIR%\JavaService.exe
set SSBINDIR=%JSBINDIR%
set LOGDIR=D:\UltrasoundData



@echo.
@echo  ******************************************************************************
@echo  * ReinstallAndConfigureService.bat - for the UltrasoundScannerBridge service *
@echo  *                                                                            *
@echo  * Note that to change the configuration of the UltrasoundScannerBridge       *
@echo  * please edit this batch file and run it to reinstall the service.           *
@echo  *                                                                            *
@echo  *         Created by Nick Evans on 10/09/2011 - www.nickevans.me.uk          *
@echo  ******************************************************************************

@echo.
@echo.
@echo  Using following version of JavaService executable:
%JSEXE% -version
@echo.


@echo  *****************  Stop the UltrasoundScannerBridge service  *****************
@echo.
@echo Press Control-C to abort, or
@pause
@echo.
net stop UltrasoundScannerBridge 
@echo.
@echo.
@echo.


@echo  **************  Uninstall the UltrasoundScannerBridge service  ***************
@echo.
@echo Press Control-C to abort, or
@pause
@echo.
%JSEXE% -uninstall UltrasoundScannerBridge 
@echo.
@echo.
@echo.


@echo  ***************  Install the UltrasoundScannerBridge service  ****************
@echo.
@echo Press Control-C to abort, or
@pause
@echo.
@echo on
%JSEXE% -install UltrasoundScannerBridge "%JVMDIR%\jvm.dll" -Djava.class.path=%SSBINDIR%\dcmrcv.jar -start org.dcm4che2.tool.dcmrcv.DcmRcv -params %CF% -out %LOGDIR%\stdout.log -err %LOGDIR%\stderr.log -current %JSBINDIR% -auto -description "%SERVICE_DESCRIPTION%"
@echo off
@echo.
@echo.
@echo.


@echo  ****************  Start the UltrasoundScannerBridge service  *****************
@echo.
@echo Press Control-C to abort, or
@pause
@echo.
net start UltrasoundScannerBridge
@echo.
@echo If the service fails to start, error messages may be found in the following:
@echo  - Event viewer
@echo  - Log files in D:\UltrasoundData\
@echo.


@echo  *****************************  Script Complete  ******************************
@echo.
@pause