# dcm4che-tool-dcmrcv-zipsend

Adapted dcm4che2 dcmrcv tool to zip and e-mail DICOM files received via C-STORE requests.

The tool accepts C-STORE requests from DICOM compatible devices such as an Ultrasound Scanner. It can be configured to compress each image received and add it to a dmz (zip) file - producing one dmz file per image series. These dmz files can be viewed using http://www.radiantviewer.com/ (recommended) or DicomWorks.
After a configurable timeout the dmz files are sent to via e-mail to the patient management system, with a subject line containing patient demographics relating to the series.

This project is based on the [dcm4che2 toolkit](http://www.dcm4che.org), and uses the [JavaService](http://javaservice.ow2.org/) wrapper in production.
