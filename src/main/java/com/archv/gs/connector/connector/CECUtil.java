package com.archv.gs.connector.connector;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.xml.XMLSerializer;

import org.apache.log4j.Logger;

import com.archv.gs.connector.util.PropertyReader;
import com.filenet.api.admin.FixedStorageArea;
import com.filenet.api.admin.StoragePolicy;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Document;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.property.Property;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import com.filenet.api.collection.DocumentSet;
import com.filenet.apiimpl.property.PropertyImpl;
import com.filenet.apiimpl.property.PropertyStringImpl;

public class CECUtil {

	private String uname = null;
	private String password = null;
	private String cp = null;
	private String ceuri = null;
	private String objName=null;
	public static Logger log = Logger.getLogger(CECUtil.class);
	public static Logger extAppLogger = Logger.getLogger("ExternalAppLogger");

	public static ObjectStore objectStore = null;

	public static Domain domain = null;
	public static Connection connection = null;
	private String jaaspath = null;
	private String archivingstoragearea = null;
	private String archivingstoragepolicy = null;
	private String archivingcasestatus = null;

	public CECUtil()
	{
		try
		{
			PropertyReader pr = new PropertyReader();
			Properties prop = pr.loadPropertyFile();
			this.uname = prop.getProperty("USERNAME");
			this.password = prop.getProperty("PASSWORD");
			this.ceuri = prop.getProperty("CEURI");
			this.cp = prop.getProperty("CONNECTION");
			this.objName=prop.getProperty("OBJECTSTORENAME");
			this.jaaspath = prop.getProperty("JAAS_PATH");
			this.archivingstoragearea = prop.getProperty("ARCHIVINGSTORAGEAREA");
			this.archivingstoragepolicy = prop.getProperty("ARCHIVINGSTORAGEPOLICY");
			this.archivingcasestatus = prop.getProperty("ARCHIVINGCASESTATUS");
			System.out.println("Properties from Connection file is Username is " + this.uname + " Password is ********* " + " CE URI " + this.ceuri + " Connection point " + this.cp);
			log.info("Properties from Connection file is Username is " + this.uname + " Password is ********* " + " CE URI " + this.ceuri + " Connection point " + this.cp);
		}
		catch (Exception e)
		{
			log.info("Error Occured while initiating connector class : "+e);
		}
	}


	public void archiveByCaseStatus() throws EngineRuntimeException{
		
//		try {

		System.setProperty("java.security.auth.login.config", jaaspath);
		connection = Factory.Connection.getConnection(ceuri);
		Subject sub = UserContext.createSubject(connection, uname,password,null);
		UserContext.get().pushSubject(sub);
		domain = Factory.Domain.getInstance(connection, null);
		objectStore = Factory.ObjectStore.fetchInstance(domain, objName, null);
		System.out.println("ObjectStore FetchInstance, ObjectStore Name = "+objectStore.get_DisplayName());
		log.info("ObjectStore FetchInstance, ObjectStore Name = "+objectStore.get_DisplayName());
		extAppLogger.info("**************** Initializing. ****************");
		
		SearchScope search = new SearchScope(objectStore);
		
		SearchSQL sql = new SearchSQL("SELECT * FROM [HCP_PrProcess] WHERE [CmAcmCaseState] = "+archivingcasestatus);
		IndependentObjectSet independentObjectSet = search.fetchObjects(sql,Integer.getInteger("50"),null, Boolean.valueOf(true));

		Folder fldr;
		Document doc;
		int i = 0;
		Iterator it = independentObjectSet.iterator();
		while (it.hasNext()){
			
			fldr = (Folder)it.next();  
			DocumentSet documents = fldr.get_ContainedDocuments();		   
			Iterator itd = documents.iterator();
               
			while(itd.hasNext()){
				
				doc = (Document)itd.next();
                
                if(!doc.get_StorageArea().get_DisplayName().equals("hcp")){
                	System.out.println("Moving Document="+doc.get_Name()+" , from StorageArea="+doc.get_StorageArea().get_DisplayName()+" (Folder="+fldr.get_FolderName()+") to HCP ... ");
                	log.info("Moving Document="+doc.get_Name()+" , from StorageArea="+doc.get_StorageArea().get_DisplayName()+" (Folder="+fldr.get_FolderName()+") to HCP ... ");
                	extAppLogger.info("| Document ID | "+doc.get_Id()+" | Folder ID | "+fldr.get_Id()+" | Document Name="+doc.get_Name()+" & Folder Name="+fldr.get_FolderName());
                	
                	FixedStorageArea strArea = Factory.FixedStorageArea.fetchInstance(objectStore, new Id(archivingstoragearea), null);
                	StoragePolicy strPolicy = Factory.StoragePolicy.fetchInstance(objectStore,new Id(archivingstoragepolicy),null);
                    doc.moveContent(strArea);
                    doc.set_StoragePolicy(strPolicy);
                    doc.save(RefreshMode.REFRESH);
                    
                    // ====== Creating MetaData XML with Document properties ======
    /*              com.filenet.api.property.Properties xProp = doc.getProperties();
                    Iterator propIt = xProp.iterator();
                    
                    Map obj = new LinkedHashMap();
                    
                    while(propIt.hasNext()){
                    	com.filenet.apiimpl.property.PropertyImpl ss = (PropertyImpl) propIt.next();
                    	if( ss.getObjectValue() instanceof Double)
        				{
        					obj.put(ss.getPropertyName(), String.format ("%.2f", ss.getObjectValue()));
        				}
        				else if( ss.getObjectValue() instanceof Boolean)
        				{
        					obj.put(ss.getPropertyName(), (Boolean)ss.getObjectValue());
        				}
        				else if( ss.getObjectValue() instanceof Float)
        				{
        					obj.put(ss.getPropertyName(), (Float)ss.getObjectValue());
        				}
        				else if( ss.getObjectValue() instanceof String)
        				{
        					obj.put(ss.getPropertyName(), ss.getStringValue());
        				}
        				else if( ss.getObjectValue() instanceof Date)
        				{
        					obj.put(ss.getPropertyName(), ss.getObjectValue().toString());
        				}
        				else if( ss.getObjectValue() instanceof Integer)
        				{
        					obj.put(ss.getPropertyName(), (Integer)ss.getObjectValue());
        				}
        				else if( ss.getObjectValue() instanceof String[])
        				{
        					obj.put(ss.getPropertyName(), (String[])ss.getObjectValue());
        				}
        				else if( ss.getValue() instanceof Integer[])
        				{
        					obj.put(ss.getPropertyName(), (Integer[])ss.getObjectValue());
        				}
        				else if( ss.getValue() instanceof Date[])
        				{	
        					obj.put(ss.getPropertyName(), (Date[])ss.getObjectValue());
        				}
        				else if( ss.getValue() instanceof Double[])
        				{
        					obj.put(ss.getPropertyName(), (Double[])ss.getObjectValue());
        				} 
        				else 
        				{
        					log.info("Key Not Avilable "+ss.getPropertyName());
        				}
                    	
                    }

                    String jsonText = JSONObject.fromObject(obj).toString();
                    System.out.println("Metadata json = "+jsonText);
                    log.info("Metadata json = "+jsonText);
                  
                    XMLSerializer serializer = new XMLSerializer();
                    JSON json = JSONSerializer.toJSON(jsonText);
                    serializer.setRootName("world-params");
                    serializer.setTypeHintsEnabled(false);
                    String xml = serializer.write(json);
                    System.out.println("Metadata XML = "+xml);
                    log.info("Metadata XML = "+xml);
                  	byte[] bytesByXML = xml.getBytes(Charset.forName("UTF-8"));    */

                    //HCPOperations hcpOpr = new HCPOperations();
					//hcpOpr.addMetaData(doc.get_Id().toString(), bytesByXML);

                    i++;
                }
                
            } 
		}
		System.out.println("Moved "+i+" documents to HCP.");
		extAppLogger.info("Moved "+i+" documents to HCP.");
		extAppLogger.info("**************** Completed. *******************");
/*		
		} catch (IOException e) {
			log.info("IOException:"+e);
		}catch (InterruptedException e) {
			log.info("InterruptedException:"+ e);
		}   */
		
	}

}
