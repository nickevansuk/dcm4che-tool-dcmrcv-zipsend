  ******************************************************************************
  *                                                                            *
  *                    UltrasoundScannerBridge Service                         *
  *                                                                            *
  *         Created by Nick Evans on 10/09/2011 - www.nickevans.me.uk          *
  ******************************************************************************

The UltrasoundScannerBridge is written in Java, compiled into the self-contained
dcmrcv.jar with all the necessary libraries, and run as a service using JavaService.

UltrasoundScannerBridge is configured to accept C-STORE requests from the Ultrasound Scanner
on port 104. It compresses each image received into a dmz (zip) file (one dmz file per image
series), and after a configurable timeout sends the dmz file to Bluespier via e-mail.

All logs are configured to be written to D:\UltrasoundData\

D:\UltrasoundData\ is used as a location to store temporary DICOM and ZIP files while they 
are being processed.

During normal operation, when the scanner is not in use, the D:\UltrasoundData\ should only 
contain log files. If any *.dmz or *.dcm files persist the log files should be investigated to
determine the related error.

The UltrasoundScannerBridge is configured via command line arguments that are set in
ReinstallAndConfigureService.bat. To change the configuration, edit this script and run it.



The contents of this directory are as follows:

- dcmrcv.jar
  Compiled self-contained UltrasoundScannerBridge Java application (includes all libraries)

- log4j.properties
  Settings for logging level of the UltrasoundScannerBridge application, options are:
	- WARN
	- INFO
	- DEBUG

- JavaService.exe
  Tool used to run any Java application as a service (see http://javaservice.ow2.org)

- ReinstallAndConfigureService.bat
  Interactively Stops, Uninstalls, Reinstalls and then Starts the UltrasoundScannerBridge service.

- RunStandaloneTest.bat
  Runs the UltrasoundScannerBridge Java application as a stand-alone java application,
  useful to identify issues with JavaService that are not caused by the application.

- ViewConfigurationHelp.bat
  Runs the UltrasoundScannerBridge application without any arguments, displaying an
  explanation of the available arguments.

- /LoadTest/
  Self-contained load-test scripts which can be used when testing the configuration of the service