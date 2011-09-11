/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Nick Evans (http://www.nickevans.me.uk), Cardiff. UK/Europe.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Nick Evans (http://www.nickevans.me.uk)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4che2.tool.dcmrcv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


import org.dcm4che2.util.CloseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nick Evans (http://www.nickevans.me.uk)
 * @version $Revision: 1 $ $Date: 2011-09-11 12:57:21 +0000 (Sun, 11 Sept 2011) $
 * @since Sept 11, 2011
 */
public class ZipFileSender extends Thread {

	// *** Static Thread controller ***
	
	static int threadId = 1;
	
	static Logger LOG = LoggerFactory.getLogger(ZipFileSender.class);
	
	private static Hashtable<String, ZipFileSender> zipFileDictionary = new Hashtable<String, ZipFileSender>();
	
	/*
	 * Writes the dicom file into a zip file. Uses an existing zip file if it exists and is not in the process of being sent.
	 * Synchronised to ensure that two identical zip files are not created simultaneously. 
	 */
	public static synchronized void addFileFromInputStream(String seriesInstanceUID, String iuid, EmailParams zipFileSubjectParams, InputStream stream, File tempDirectory) throws IOException {
		String filename = iuid.replace('.', '-') + ".dcm";
		
		ZipFileSender existingZip = zipFileDictionary.get(seriesInstanceUID);
		if (!(existingZip != null && existingZip.writeFile(filename, stream))) {
			ZipFileSender zip = new ZipFileSender(seriesInstanceUID, zipFileSubjectParams, tempDirectory);
			zipFileDictionary.put(seriesInstanceUID, zip);
			zip.writeFile(filename, stream);
		}
	}
	
	
	// *** Configuration parameters ***
	
    private static boolean sendZip = false;
    private static int zipTimeout = 5000;
    
    private static String mailTo = null;
    private static String mailFrom = null;
    private static String mailHost = null;
    private static String mailUsername = null;
    private static String mailPassword = null; 
    
    public final static void setSend(boolean send) {
    	sendZip = send;
    }
    
    public final static void setZipTimeout(int timeout) {
    	zipTimeout = timeout;
    }
    
    public final static void setEmailTo(String emailToAddress ) {
    	mailTo = emailToAddress;
    }
    
    public final static void setEmailFrom(String emailFromAddress ) {
    	mailFrom = emailFromAddress;
    }

    public final static void setSMTPServer(String smtpServer ) {
    	mailHost = smtpServer;
    }
    
    public final static void setSMTPUsername(String smtpUsername ) {
    	mailUsername = smtpUsername;
    }
    
    public final static void setSMTPPassword(String smtpPassword ) {
    	mailPassword = smtpPassword;
    }
    

    // *** Thread ***
       
	EmailParams zipFileSubjectParams = null;
	File zipFile;
	ZipOutputStream out = null;
	boolean fileWritten = false; 
	boolean fileSent = false;
	String seriesInstanceUID;
	String filename;

	
	/*
	 * Each open zip file has a thread associated that is interrupted
	 * whenever the zip file is accessed.
	 * After a period of inactivity, the thread closes the zip file 
	 * and sends it by e-mail if configured.
	 * 
	 * @see java.lang.Thread#run()
	 */
    public void run() {
    	while (out != null && !fileSent) {
    		if (!fileWritten) {
    			try {
    				synchronized (this) {
    					wait();
    				}
				} catch (InterruptedException e) {
					//Loop and check file is written
					continue;
				}
    		} else {
    			try {   	
    				sleep(zipTimeout);
	    			sendEmail();
				} catch (InterruptedException e) {
					//Sleep again before sending
					continue;
				}
    		}
    	}
    }
        
    /*
     * Create a new zip file and start this thread to monitor its activity
     */
	public ZipFileSender(String seriesInstanceUID, EmailParams subjectParams, File tempDirectory) throws IOException {
		super("ZIPFILESENDER-" + threadId++);
		this.seriesInstanceUID = seriesInstanceUID;

		try {
			zipFile = new File(tempDirectory, this.getName() + "_" + seriesInstanceUID.replace('.', '-') + ".dmz");
			zipFileSubjectParams = subjectParams;
	        out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream( zipFile )));
	        out.setMethod(ZipOutputStream.DEFLATED);
	        this.start();
	        LOG.info("M-ZIP-OPEN {}", zipFile);
		} catch (IOException e) {
			LOG.error("Error creating zip file '" + zipFile + "'", e);
			CloseUtils.safeClose(out);
			throw e;
		}    
	}
	
	
	static final int BUFFER = 2048;
	
	/*
	 * Writes the given stream into the zip file.
	 * Returns true if file writing was successful.
	 * Synchronized to ensure that the zip file is not closed during the write.
	 */
	public synchronized boolean writeFile(String filename, InputStream stream) throws IOException {
		//Return false to notify failure if the file has already been sent (we missed the boat)
		if (fileSent) return false;
		
		BufferedInputStream origin = new BufferedInputStream(stream, BUFFER);
		try {
			byte data[] = new byte[BUFFER];
			ZipEntry entry = new ZipEntry(filename);
			out.putNextEntry(entry);
			
            int count;
            while((count = origin.read(data, 0, 
              BUFFER)) != -1) {
               out.write(data, 0, count);
            }
            origin.close();
			
            fileWritten = true;
            interrupt();
            
			return true;
		} catch (IOException e) {
			LOG.error("Error writing to zip file '" + zipFile + "'", e);
			interrupt();
			throw e;
		} finally {
			CloseUtils.safeClose(origin);
		}
		
	}
	
	/*
	 * Sends an e-mail with the zip attached 
	 * Synchronized to ensure that no writing occurs to the zip file while it is being sent
	 */
    private synchronized void sendEmail() {
		fileSent = true;
	    if (out != null) {
	    	CloseUtils.safeClose(out);
	        out = null;
	    	zipFileDictionary.remove(seriesInstanceUID);
	    	LOG.info("M-ZIP-CLOSE {}", zipFile);
	    	
	    	if (sendZip) {	  
				  String msgText1 = "Sending a file.\n";
				  String subject = zipFileSubjectParams.getParamString();
				  
				  // create some properties and get the default Session
				  Properties props = System.getProperties();
				  props.put("mail.smtp.host", mailHost);
				  props.put("mail.smtp.auth", "true");
				  props.put("mail.smtp.port", "587");
				  
				  Session session = Session.getDefaultInstance(props,
						new javax.mail.Authenticator() {
							protected PasswordAuthentication getPasswordAuthentication() {
								return new PasswordAuthentication(mailUsername,mailPassword);
							}
						});
				
				  try 
				  {
				      // create a message
				      MimeMessage msg = new MimeMessage(session);
				      msg.setFrom(new InternetAddress(mailFrom));
				      InternetAddress[] address = {new InternetAddress(mailTo)};
				      msg.setRecipients(Message.RecipientType.TO, address);
				      msg.setSubject(subject);
				
				      // create and fill the first message part
				      MimeBodyPart mbp1 = new MimeBodyPart();
				      mbp1.setText(msgText1);
				
				      // create the second message part (the attachment)
				      MimeBodyPart mbp2 = new MimeBodyPart();
				
				      // attach the file to the message
				      FileDataSource fds = new FileDataSource( zipFile );
				      mbp2.setDataHandler(new DataHandler(fds));
				      mbp2.setFileName(seriesInstanceUID.replace('.', '-') + ".dmz");
				
				      // create the Multipart and add its parts to it
				      Multipart mp = new MimeMultipart();
				      mp.addBodyPart(mbp1);
				      mp.addBodyPart(mbp2);
				
				      // add the Multipart to the message
				      msg.setContent(mp);
				
				      // set the Date: header
				      msg.setSentDate(zipFileSubjectParams.getSeriesDateTime());
				      
				      // send the message
				      Transport.send(msg);
				      
				      LOG.info("M-EMAIL {}", zipFileSubjectParams.getParamString());
				      
				      if (zipFile.delete()) {
				          LOG.info("M-DELETE {}", zipFile);
				          zipFile = null;
				      }
				  } catch(Exception e) {
					  LOG.error("Error e-mailing file '" + zipFile + "' with subject '" + zipFileSubjectParams.getParamString() + "'", e);
					  System.out.println();
					  System.out.println((new Date()).toString());
					  System.out.println("   E-mail send failed:");
					  System.out.println("      " + e.getMessage().replaceAll("\n", "\r\n      "));
					  if (e.getCause() != null) {
						  System.out.println("      " + e.getCause().getMessage().replaceAll("\n", "\r\n      "));
					  }
					  System.out.println("   Please manually e-mail the following:");
					  System.out.println("    - Attachment:   " + zipFile);
					  System.out.println("    - Subject Line: " + zipFileSubjectParams.getParamString());
					  System.out.println("    - To:           " + mailTo);
				  }
	    	}
	    }
    }
		
}