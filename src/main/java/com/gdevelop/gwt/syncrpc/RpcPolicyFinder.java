package com.gdevelop.gwt.syncrpc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URISyntaxException;
import java.net.URL;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


public class RpcPolicyFinder {
  private static final String GWT_PRC_POLICY_FILE_EXT = ".gwt.rpc";
  private static final Map<String, String> CACHE_POLICY_FILE = new HashMap<String, String>();
  
  private static final Logger logger = Logger.getLogger(RpcPolicyFinder.class.getName());
  
  public static String getCachedPolicyFile(String url){
    return CACHE_POLICY_FILE.get(url);
  }
  
  public static Map<String, String> searchPolicyFileInClassPath(){
    Map<String, String> result = new HashMap<String, String>();
    String classPath = System.getProperty("java.class.path");
    StringTokenizer st = new StringTokenizer(classPath, File.pathSeparator);
    while (st.hasMoreTokens()){
      String path = st.nextToken();
      File f = new File(path);
      if (f.isDirectory()){
        result.putAll(searchPolicyFileInDirectory(path));
      }
      // TODO: Search in jar, zip files
    }
    
    if (result.size() == 0){
      logger.warning("No RemoteService in the classpath");
    }else{
      dumpRemoteService(result);
    }
    
    return result;
  }
  public static Map<String, String> searchPolicyFileInDirectory(String path) {
    Map<String, String> result = new HashMap<String, String>();
    
    String policyName = null;
    File f = new File(path);
    String[] children = f.list(new FilenameFilter(){
      public boolean accept(File dir, String name) {
        if (name.endsWith(GWT_PRC_POLICY_FILE_EXT)){
          return true;
        }
        return false;
      }
    });
    
    for (String child : children){
      policyName = child.substring(0, child.length() - GWT_PRC_POLICY_FILE_EXT.length());
      try {
        result.putAll(parsePolicyName(policyName, new FileInputStream(new File(path ,child))));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    return result;
  }
  
  private static Map<String, String> parsePolicyName(String policyName, InputStream in) throws IOException {
    Map<String, String> result = new HashMap<String, String>();
    
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String line = reader.readLine();
    while (line != null){
      int pos = line.indexOf(", false, false, false, false, _, ");
      if (pos > 0){
        result.put(line.substring(0, pos), policyName);
        result.put(line.substring(0, pos) + "Async", policyName);
      }
      line = reader.readLine();
    }
    
    return result;
  }
  
  public static Map<String, String> fetchSerializationPolicyName(String moduleBaseURL) throws IOException, URISyntaxException{
    Map<String, String> result = new HashMap<String, String>();
    
    moduleBaseURL = moduleBaseURL.trim(); //remove outer trim just in case
    String[] urlparts = moduleBaseURL.split("/");
    String moduleNoCacheJs = urlparts[urlparts.length-1] + ".nocache.js";   //get last word of url appended with .nocache.js

    String responseText = "";
    responseText = getResposeText(moduleBaseURL + moduleNoCacheJs);
    // parse the .nocache.js for list of Permutation name
    // Permutation name is 32 chars surrounded by apostrophe
    String regex = "\'([A-Z0-9]){32}\'";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(responseText);
    // while (matcher.find())
    if (matcher.find()){
      String permutationFile = matcher.group();
      permutationFile = permutationFile.replace("\'", "");

      // Load the first permutation html file
      permutationFile += ".cache.html";
      responseText = getResposeText(moduleBaseURL + permutationFile);
      matcher = pattern.matcher(responseText);
      int i = 0;
      while (matcher.find()){
        String policyName = matcher.group();
        policyName = policyName.replace("\'", "");
        if (0 == i++){
          // The first one is the permutation name
          continue;
        }
        responseText = getResposeText(moduleBaseURL + policyName + GWT_PRC_POLICY_FILE_EXT);
        result.putAll(parsePolicyName(policyName, new ByteArrayInputStream(responseText.getBytes("UTF8"))));
      }
    }
    
    if (result.size() == 0){
      logger.warning("No RemoteService fetched from server");
    }else{
      dumpRemoteService(result);
    }
    
    return result;
  }

  private static String getResposeText(String myurl) throws IOException, URISyntaxException {
    URL url = new URL(myurl);
    DefaultHttpClient client = new DefaultHttpClient ( );
    HttpGet httpGet = new HttpGet ( url.toURI ( ) );
    HttpResponse response = client.execute ( httpGet );
    
    InputStream is = response.getEntity ( ).getContent ( );
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;
    while ((len = is.read(buffer)) > 0){
      baos.write(buffer, 0, len);
    }
    String responseText = baos.toString("UTF8");
    
    if (myurl.endsWith(GWT_PRC_POLICY_FILE_EXT)){
      CACHE_POLICY_FILE.put(myurl, responseText);
    }
    
    return responseText;
  }
  
  private static void dumpRemoteService(Map<String, String> result){
    if (result.size() > 0){
      logger.fine("Found following RemoteService(s) in the classpath:");
      String s = "";
      for (String className : result.keySet()){
        s += className + "\n";
      }
      logger.fine(s);
    }else{
      logger.warning("No RemoteService in the result");
    }
  }
  
  // Test
  public static void main(String[] args) throws Exception{
    Map<String, String> policyMap = searchPolicyFileInClassPath();
    System.out.println(policyMap);
    
    policyMap = fetchSerializationPolicyName("http://localhost:8888/rpcsuite/");
    System.out.println(policyMap);
  }
}
