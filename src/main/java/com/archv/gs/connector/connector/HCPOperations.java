package com.archv.gs.connector.connector;

import java.io.*;
import java.util.Properties;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.xml.XMLSerializer;

import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.archv.gs.connector.connector.Utils;
import com.archv.gs.connector.util.PropertyReader;

public class HCPOperations {
	
	private String hcp_uname = null;
	private String hcp_password = null;
	private String hcp_namespace = null;
	private String hcp_tenant = null;
	private String hcp_host=null;
	public static Logger log = Logger.getLogger(HCPOperations.class);
	
	public HCPOperations()
	{
		try
		{
			PropertyReader pr = new PropertyReader();
			Properties prop = pr.loadPropertyFile();
			this.hcp_uname = prop.getProperty("HCP_USERNAME");
			this.hcp_password = prop.getProperty("HCP_PASSWORD");
			this.hcp_namespace = prop.getProperty("HCP_NAMESPACE");
			this.hcp_tenant = prop.getProperty("HCP_TENANT");
			this.hcp_host=prop.getProperty("HCP_HOST");

		}
		catch (Exception e)
		{
			log.info("Error Occured while initiating connector class : "+e);
		}
	}
	
	
	public void addMetaData(String objID, byte[] metaData) throws IOException, InterruptedException{
		
		JSONObject HCPsearchResult = searchHCP(objID, 1, 0);
		for(int cnt=1; cnt<10 && HCPsearchResult.getJSONObject("queryResult").getJSONObject("status").getInt("results")!=1; cnt++){
			System.out.println("No results for round "+cnt+". Sleeping for 1 min.");
			log.info("No results for round "+cnt+". Sleeping for 1 min.");
			Thread.sleep(60000);
			HCPsearchResult = searchHCP(objID, 1, 0);	
		}
				
		if(HCPsearchResult.getJSONObject("queryResult").getJSONObject("status").getInt("results")==1){
			
			String objUrl = HCPsearchResult.getJSONObject("queryResult").getJSONArray("resultSet").getJSONObject(0).getString("urlName");
			System.out.println("objUrl = "+objUrl);
			log.info("objUrl = "+objUrl);
			
			objUrl = objUrl.replaceAll("\\{", "%7B");
			objUrl = objUrl.replaceAll("\\}", "%7D");
        	String url = objUrl + "?type=custom-metadata&annotation=myannotation";

        	HttpPut request = new HttpPut(url);

        	//add authorization header for user(base64) "exampleuser" with password(md5) "passw0rd"
        	//request.addHeader(HCPAuthHeaderKey, auth);
        	request.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        	String auth = "hcp-ns-auth=" + Utils.getBase64Value(hcp_uname) + ":" + Utils.getMD5Value(hcp_password);
        	request.setHeader("Cookie", auth);

        	//setup byte array entity for upload(PUT)
       	 	ByteArrayEntity requestEntity = new ByteArrayEntity(metaData);

       	 	//set the request to use the byte array
       	 	request.setEntity(requestEntity);
       	 	//execute PUT request
       	 	HttpClient client = HttpClientBuilder.create().build();
       	 	HttpResponse response = client.execute(request);

       	 	//print response status to console
       	 	System.out.println("Response Code : "+ response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
       	 	log.info("Response Code : "+ response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
       	 	
       	 	if(response.getStatusLine().getStatusCode()==201){
       	 		log.info("Meta added successfully");	
       	 	}else{
       	 		log.info("Meta not added : "+response.getStatusLine().getReasonPhrase());
       	 	}
		
		}else{
			System.out.println("No results found in HCP. Metadata not added");
			log.info("No results found in HCP. Metadata not added");
		}
    }
	
	public JSONObject searchHCP(String searchKey, int count, int offset) {

		// ====== Searching in HCP to retrieve Object URL ======  
		searchKey = searchKey.substring(1, searchKey.length()-1);
		searchKey = searchKey.replaceAll("-", "\\-");
        try {
            String url = "https://" + hcp_tenant + "." + hcp_host + "/query";
            
            //Creating HTTP POST object
            HttpPost httpPost = new HttpPost(url);
            httpPost.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
            
            //Setting HTTP POST headers for Authentication, Request type, Respond type and
            String auth = "hcp-ns-auth=" + Utils.getBase64Value(hcp_uname) + ":" + Utils.getMD5Value(hcp_password);
            httpPost.addHeader("Cookie", auth);
            httpPost.addHeader("Content-Type", "application/xml");
            httpPost.addHeader("Accept", "application/json");
            
            Map obj = new LinkedHashMap();
            obj.put("query", "+(utf8Name:FN\\{"+searchKey+"\\}*)");
            obj.put("count", count);
            obj.put("offset", offset);
            obj.put("verbose", "true");
            String jsonText = "{\"object\" :" + JSONObject.fromObject(obj) + "}";
            System.out.println("HCPsearchQuery = "+jsonText);
            log.info("HCPsearchQuery = "+jsonText);
            
            XMLSerializer serializer = new XMLSerializer();
            JSON json = JSONSerializer.toJSON(jsonText);
            serializer.setRootName("queryRequest");
            serializer.setTypeHintsEnabled(false);
            String xml = serializer.write(json);
            System.out.println("HCPsearchQuery XML = "+xml);
            log.info("HCPsearchQuery XML = "+xml);
            
            xml = xml.replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();
            StringEntity stringEntity = new StringEntity(xml, HTTP.UTF_8);
            httpPost.setEntity(stringEntity);
            
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(httpPost);
            JSONObject retObject = (JSONObject) JSONSerializer.toJSON(EntityUtils.toString(response.getEntity()));
            System.out.println("HCPsearchResult = "+retObject);
            log.info("HCPsearchResult = "+retObject);
            return retObject;
        } catch (UnsupportedEncodingException ex) {
        	log.info("UnsupportedEncodingException : "+ex);
            return null;
        } catch (IOException ex) {
            log.info("IOException : "+ex);
            return null;
        }
        
    }
	
	public static void main(String[] args) {
	//	HCPOperations hcpOperations = new HCPOperations();
	//	hcpOperations.searchHCP("{806B0F6C-0000-C01A-AC84-3447FC7975C7}", 1, 0);
	/*	try {
			hcpOperations.addMetaData("{F0D4DB6B-0000-C154-9D83-402E228DBC9D}", null);
		} catch (IOException e) {
			e.printStackTrace();
		} */
		
	}

}
