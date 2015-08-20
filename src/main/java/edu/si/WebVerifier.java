/**
 * Copyright 2015 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 *
 * @author Charles Stern 1Charlesstern@gmail.com
 * @version 1.0
 * @since 2015-8-21*/
package edu.si;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.probatron.Driver;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class WebVerifier {
    //The words validate/validator and verify/verifier are used interchangeably in this context.

    /**
     * On initialization checks if the file is valid before validation
     * @param place where the user's file is on the server file system
     * @param log xmlVerifierlog.csv, located on the server file system.  Can be downloaded by the user after the program completes.
     * @param out Prints directly to the webpage in a <p></p> format
     * @param isWCS Which organization is the manifest from?
     */
	public WebVerifier(String place, PrintWriter log, PrintWriter out, boolean isWCS){
		final String nl = System.getProperty("line.separator");
		if(place.length()<4) {
            out.append("Invalid Filename on file.").append(nl);
            log.println("N"+","+"Invalid filename"+","+"N/A"+","+"N/A"+","+"N/A");
            return;
        }
        if(!place.substring(place.length()-4, place.length()).equals(".xml")){
			out.append("This file has an incorrect file extension. Skipping file.").append(nl);
            log.println("N"+","+"Invalid file extension"+","+"N/A"+","+"N/A"+","+"N/A");
            return;}
	try {
		Verify(place, log, out, isWCS);
        //this exception should not happen ever
	}
	  catch (SAXException e1){
		  out.append("Error reading file. It may be corrupt or invalid.").append(nl);
          log.println("N" + "," + "Corrupt/Invalid file" + "," + "N/A" + "," + "N/A" + "," + "N/A");
	  }
 /*   catch (Exception e1){                                   //DEBUG PURPOSES ONLY
        StackTraceElement[] e3=e1.getStackTrace();
        for(int x=0;x<e3.length;x++)
            out.append(e3[x].getLineNumber()+e3[x].getMethodName()+" "+e3[x].getClassName());
    }*/
	}

    /**
     * Validation and logging
     * @param place Where the user's file is on the server file system
     * @param log xmlVerifierlog.csv, located on the server file system.  Can be downloaded by the user after the program completes.
     * @param out Prints directly to the webpage in a <p></p> format
     * @param isWCS Which organization is the manifest from?
     * @throws SAXException If the user file somehow is not valid XML
     */
	public void Verify(String place, PrintWriter log, PrintWriter out, boolean isWCS) throws SAXException
	{
		String line, line2;
        boolean errIsNull=false;
        boolean tron=true;
        boolean sch;
        boolean result=true;
        InputStream Schematron;
        InputStream XSD;
        List<String> Error= new LinkedList<>();
        List<String> Row = new LinkedList<>();
        List<String> Column = new LinkedList<>();
        List<String> ErrLoc = new LinkedList<>();
		final String nl = System.getProperty("line.separator");

		ClassLoader cl = this.getClass().getClassLoader();
        //fetches resources
        if(isWCS){
		Schematron = cl.getResourceAsStream("/resources/WCSDeploymentManifestFinal.sch");
		XSD = cl.getResourceAsStream("/resources/WCSDeploymentManifest2015Final.xsd");
        }
        else{
            Schematron = cl.getResourceAsStream("/resources/eMammalDeploymentManifest2015Final.sch");
            XSD = cl.getResourceAsStream("/resources/eMammalDeploymentManifest2015Final.xsd");
        }
		File tronFile = convertToFile(Schematron);
		try{
		//The third-party program that is used for Schematron validation only uses System.out.
		// Hence, this block of code.
            File probOut = new File("webapp/usrFiles/temp.txt");
            File probErr = new File("/webapp/usrFiles/temp2.txt");
            System.setOut(new PrintStream(probOut));
            System.setErr(new PrintStream(probErr));
            Driver.main(new String[]{place, tronFile.getAbsolutePath()});
		    BufferedReader in = new BufferedReader(new FileReader(probOut));
		    BufferedReader err = new BufferedReader(new FileReader(probErr));
            System.setOut(System.out);
            System.setErr(System.err);

            String debug;
            while((debug=err.readLine())!=null)
                System.out.println(debug);
		for(;;){
                line = in.readLine();
                line2 = in.readLine();
            if(!line.contains("xml version=")){
                if(line2.equals(""))
                    break;
                Error.add(tronErr(line2));
                Row.add(tronRow(line));
                Column.add(tronCol(line));
                ErrLoc.add(tronErrLoc(line));
                }
            }
		while(!errIsNull){
            int count=0;
            count++;
            String Eline = err.readLine();
            if(Eline==null)
                errIsNull=true;
            if(count>1) {
                tron = false;
                out.append("Error: Schematron validation could not be completed. The file may be of an invalid type.").append(nl);
            }
		}
		} catch (IOException e){
            log.println("N" + "," + "Invalid File location" + "," + "N/A" + "," + "N/A" + "," + "N/A");
            e.printStackTrace();
		}

		Source xmlFile = new StreamSource(new File(place));
		Source schemaSource = new StreamSource(XSD);
		System.setProperty("java.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
		SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		SchemaFactory schemaFactory = SchemaFactory
		    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(schemaSource);
		Validator validator = schema.newValidator();
		final List<SAXParseException> exceptions = new LinkedList<>();
        //Used to count the errors rather than having the validator hit an error and stop
		  validator.setErrorHandler(new ErrorHandler()
          {
              public void warning(SAXParseException exception) throws SAXException {
                  exceptions.add(exception);
              }

              public void fatalError(SAXParseException exception) throws SAXException {
                  exceptions.add(exception);
              }

              public void error(SAXParseException exception) throws SAXException {
                  exceptions.add(exception);
              }
          });
		  try {
			validator.validate(xmlFile);
		} catch (IOException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			  out.append("XML Schema Validation failed. Invalid file or file location.").append(nl);
              log.println("N" + "," + "Invalid file or file location" + "," + "N/A" + "," + "N/A" + "," + "N/A");
			  return;
		}
		  if(exceptions.size()==0)
              sch=true;
		  else{sch = false;
              for (int i = 1; i < exceptions.size(); i++) {
                  Error.add(xsdError(exceptions.get(i).toString()));
                  Row.add(xsdRow(exceptions.get(i).toString()));
                  Column.add(xsdCol(exceptions.get(i).toString()));
                  ErrLoc.add("N/A");
                  if (!xsdError(exceptions.get(i).toString()).equals("")) ;
                  //For loop returns errors
              }
              }
        if((sch && tron)&& sch)
            out.append("File has PASSED validation.").append(nl);
        else{
                out.append("File has FAILED validation.  Errors have been noted in the log.").append(nl);
                result=false;
            }
        bustCommas(Error);
        bustCommas(ErrLoc);
        bustCommas(Row);
        bustCommas(Column);
        if(result)
            log.println("Y" + "," +"N/A" +  "," +"N/A"+  "," +"N/A"+ ","+"N/A");
         for(int l=0; l<Error.size();l++)
             log.println("N" + "," + Error.get(l) + "," + ErrLoc.get(l) + "," + Row.get(l) + "," + Column.get(l));
    }

    /**
     * Converts Schematron instructions from Inputstream to File so it can be processed by third-party application
     * @param in InputStream of instructions
     * @return File containing instructions (file deletes on completion)
     */
	public File convertToFile(InputStream in) {
        File tronFile = new File("temp.sch");
        tronFile.deleteOnExit();
        try {
            OutputStream os = new FileOutputStream(tronFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            //read from in to buffer
            while ((bytesRead = in.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            in.close();
            //flush OutputStream to write any buffered data to file
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tronFile;
    }

    /**
     * Fetches column number of error from output
     * @param str the output
     * @return the column number
     */
    String xsdCol(String str){
        return str.substring(str.indexOf(":", str.indexOf("columnNumber"))+1, str.indexOf(";", str.indexOf("columnNumber")));
    }

    /**
     * Puts every item in each List in quotes to prevent comma issues in .csv
     * @param list List that needs to be fool-proofed
     * @return Same List with brand new quotes around each element
     */
    List<String> bustCommas(List<String> list){
        for(int k=0;k<list.size();k++){
            list.set(k,"\""+list.get(k)+"\"");
        }
     return list;
    }
    /**
     * Fetches line number of error from output
     * @param str the output
     * @return the line number
     */
    String xsdRow(String str){
       return str.substring(str.indexOf("lineNumber:")+11,str.indexOf(";", str.indexOf("lineNumber:")-1));
    }
    /**
     * Fetches error summary from output
     * @param str the output
     * @return the summary
     */
    String xsdError(String str){
        return str.substring(str.indexOf(":", str.indexOf("columnNumber:")+14)+1,str.length());
    }
    /**
     * Fetches error summary from output
     * @param str the output
     * @return the summary
     */
    String tronErr(String str){
        String p1=""; String p2="";
        if(str.contains("svrl:text"))
        p2 = str.substring(str.indexOf("svrl:text")+10, str.lastIndexOf("svrl:text")-2);
        if((p1 + p2).equals(""))
            return str;
        return p1+p2;
    }
    /**
     * Fetches column number of error from output
     * @param str the output
     * @return the column number
     */
    String tronCol(String str){
            String result= str.substring(str.indexOf("col=")+4, str.lastIndexOf(">"));
        while(result.contains("\""))
            result=result.substring(0,result.indexOf("\""))+result.substring(result.indexOf("\"")+1, result.length());
        return result;}
    /**
     * Fetches line number of error from output
     * @param str the output
     * @return the line number
     */
    String tronRow(String str){
            String result= str.substring(str.indexOf("line=")+5,str.lastIndexOf("col=")-1);
        while(result.contains("\""))
            result=result.substring(0,result.indexOf("\""))+result.substring(result.indexOf("\"")+1, result.length());
        return result;}

    /**
     * Fetches the location of the error in the file
     * @param str the output
     * @return error location
     */
    String tronErrLoc(String str){
      return str.substring(str.indexOf("location=\"")+10, str.indexOf("\"", str.indexOf("location=\"")+11));
    }
}