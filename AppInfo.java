class AppInfo {
	String hashID;
	String username;
	String remoteIP;
	String remoteUsername;
	String remotePort;
	String remoteHash;

	public AppInfo(String username) {
		hashID = "";
		this.username = username;
		remoteHash = "";
		remoteIP = "";
		remotePort = "";
		remoteUsername = "";
	}
}
