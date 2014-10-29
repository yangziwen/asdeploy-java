package com.ablesky.asdeploy.util;

import org.apache.commons.lang3.math.NumberUtils;

public class SystemUtil {
	
	public static final String HTTP_SCHEME = "http";
	public static final int HTTP_PORT = NumberUtils.toInt(System.getProperty("asdeploy.http.port"), 80);
	
	public static final String HTTPS_SCHEME = "https";
	public static final int HTTPS_PORT = NumberUtils.toInt(System.getProperty("asdeploy.https.port"), 443);
	
	public static final String LDAP_URL = System.getProperty("asdeploy.ldap.url");
	
}
