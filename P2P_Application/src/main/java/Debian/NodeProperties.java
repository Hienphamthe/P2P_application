package main.java.Debian;

public class NodeProperties {
//	final public static int sipPort = 5060;
	final public static String p2pPort = "8080";

	public static String permUri = null;
	public static String tempUri = null;
	
	public static String userName;
	public static String host = "TKLAB";
	
	public static String p2pIP;


	public static void setUri(String name) {
            userName = name;

//            permUri ="sip:"+name+"@"+host+".de";
//            tempUri ="sip:"+ name+"@"+p2pIP+":"+sipPort;
            permUri = name+"@"+host+".de";
            tempUri = name+"@"+p2pIP;
	}
}
