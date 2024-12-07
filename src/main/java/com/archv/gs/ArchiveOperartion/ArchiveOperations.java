package com.archv.gs.ArchiveOperartion;

import java.util.Properties;

import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.apache.log4j.PropertyConfigurator;

import com.archv.gs.connector.connector.CECUtil;
import com.archv.gs.connector.util.PropertyReader;

import org.apache.log4j.Logger;

public class ArchiveOperations {

	public static Properties prop;
	public static Logger log = Logger.getLogger(ArchiveOperations.class);


	public void doArchiveOperations(){

		try {
			PropertyReader pr = new PropertyReader();

			String pathSep = System.getProperty("file.separator");
			prop=pr.loadPropertyFile();
			String logpath = prop.getProperty("LOG4J_FILE_PATH");
			String activityRoot= prop.getProperty("LOG_PATH");
			String logPropertyFile =logpath+pathSep+"log4j.properties"; 


			PropertyConfigurator.configure(logPropertyFile);
			log = Logger.getLogger(ArchiveOperations.class);

			PropertyReader.loadLogConfiguration(logPropertyFile, activityRoot, "ArchiveCases.log");
			log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>> Entered into Do Archive Method >>>>>>>>>>>>>>>>>>>>>>>>>>>>");

			CECUtil ceutil = new CECUtil();
			ceutil.archiveByCaseStatus();

			log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>> Do Archive Completed Sucessfully >>>>>>>>>>>>>>>>>>>>>>>>>>>> \n");

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error Occured while grouping : "+e.fillInStackTrace());
		}

	}

	public static void main(String[] args) {
		ArchiveOperations cc = new ArchiveOperations();
		cc.doArchiveOperations();
	}

}
