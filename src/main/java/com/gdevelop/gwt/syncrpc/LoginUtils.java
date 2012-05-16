package com.gdevelop.gwt.syncrpc;


import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gdata.client.GoogleAuthTokenFactory;
import com.google.gdata.util.AuthenticationException;
import com.google.gwt.user.client.rpc.StatusCodeException;


public class LoginUtils {
  private static final String GAE_SERVICE_NAME = "ah";
  
  /**
   *
   * @param loginUrl Should be http://localhost:8888 for local development mode 
   * or https://example.appspot.com for deployed app
   * @param serviceUrl Should be http://localhost:8888/yourApp.html
   * @param email
   * @param password
   * @return The CookieManager for subsequence call
   * @throws IOException
   * @throws AuthenticationException
 * @throws URISyntaxException 
   */
  public static CookieStore loginAppEngine(String loginUrl, String serviceUrl, 
                                    String email, String password) throws IOException,
                                                            AuthenticationException, URISyntaxException {
    boolean localDevMode = false;
    
    if (loginUrl.startsWith("http://localhost")){
      localDevMode = true;
    }
    
       CookieStore cookieStore = new BasicCookieStore ( );
      
      if (localDevMode) {
        loginUrl += "/_ah/login";
        URL url = new URL(loginUrl);
        email = URLEncoder.encode(email, "UTF-8");
        serviceUrl = URLEncoder.encode(serviceUrl, "UTF-8");
        String requestData = "email=" + email + "&continue=" + serviceUrl;
        DefaultHttpClient client = new DefaultHttpClient ( );
        client.setCookieStore ( cookieStore );
        HttpPost httpPost = new HttpPost ( url.toURI ( ) );
        httpPost.setHeader ( "Content-Type", "application/x-www-form-urlencoded");
        httpPost.setEntity ( new StringEntity ( requestData ) );
        HttpResponse response = client.execute ( httpPost );
        
        int statusCode = response.getStatusLine ( ).getStatusCode ( );
        if ((statusCode != HttpStatus.SC_OK)
            && (statusCode != HttpStatus.SC_MOVED_TEMPORARILY)) {
          String responseText = response.getStatusLine ( ).getReasonPhrase ( );
          throw new StatusCodeException(statusCode, responseText);
        }
        response.getEntity ( ).consumeContent ( );
      }else{
        GoogleAuthTokenFactory factory = new GoogleAuthTokenFactory(GAE_SERVICE_NAME, "", null);
        // Obtain authentication token from Google Accounts
        String token = factory.getAuthToken(email, password, null, null, GAE_SERVICE_NAME, "");
        loginUrl = loginUrl + "/_ah/login?continue=" + URLEncoder.encode(serviceUrl, "UTF-8")
            + "&auth=" + token;
        URL url = new URL(loginUrl);
   
        DefaultHttpClient client = new DefaultHttpClient ( );
        client.setCookieStore ( cookieStore );
        HttpGet httpGet = new HttpGet ( url.toURI ( ) );
        HttpResponse response = client.execute ( httpGet );
        
        int statusCode = response.getStatusLine ( ).getStatusCode ( );
        if ((statusCode != HttpStatus.SC_OK)
                 && (statusCode != HttpStatus.SC_MOVED_TEMPORARILY)) {
          String responseText = response.getStatusLine ( ).getReasonPhrase ( );
          throw new StatusCodeException(statusCode, responseText);
        }
        response.getEntity ( ).consumeContent ( );
      }
      
      return cookieStore;
  }

  public static CookieStore loginFormBasedJ2EE(String loginUrl, String username, 
                                             String password) throws IOException, URISyntaxException
  {              
      CookieStore cookieStore = new BasicCookieStore ( );
      
      // GET the form
      URL url = new URL(loginUrl);
      DefaultHttpClient client = new DefaultHttpClient ( );
      client.setCookieStore ( cookieStore );
      HttpGet httpGet = new HttpGet ( url.toURI ( ) );
      HttpResponse response = client.execute ( httpGet );
      
      int statusCode = response.getStatusLine ( ).getStatusCode ( );
      if ((statusCode != HttpStatus.SC_OK)
               && (statusCode != HttpStatus.SC_MOVED_TEMPORARILY)) {
        String responseText = response.getStatusLine ( ).getReasonPhrase ( );
        throw new StatusCodeException(statusCode, responseText);
      }
      response.getEntity ( ).consumeContent ( );
      
      // Perform login
      loginUrl += "j_security_check";
      url = new URL(loginUrl);
      username = URLEncoder.encode(username, "UTF-8");
      password = URLEncoder.encode(password, "UTF-8");
      String requestData = "j_username=" + username + "&j_password=" + password;

      HttpPost httpPost = new HttpPost ( url.toURI ( ) );
      httpPost.setHeader ( "Content-Type", "application/x-www-form-urlencoded");
      httpPost.setEntity ( new StringEntity ( requestData ) );
      response = client.execute ( httpPost );
      
      statusCode = response.getStatusLine ( ).getStatusCode ( );
      if ((statusCode != HttpStatus.SC_OK)
               && (statusCode != HttpStatus.SC_MOVED_TEMPORARILY)) {
        String responseText = response.getStatusLine ( ).getReasonPhrase ( );
        throw new StatusCodeException(statusCode, responseText);
      }
      response.getEntity ( ).consumeContent ( );

      return cookieStore;
  }
}
