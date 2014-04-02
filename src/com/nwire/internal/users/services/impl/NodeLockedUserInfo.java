package com.nwire.internal.users.services.impl;

import com.nwire.studio.core.system.IUserInfo;
import com.nwire.studio.core.system.SystemStatus;
 
public class NodeLockedUserInfo implements IUserInfo {
	private static final long serialVersionUID = 1L;
	private String fullName;
	private String organization;
	private String email;

	public NodeLockedUserInfo() {
	}

	public Object getData(String key, Object[] args) {
		if (key.equals("spring-beans")) {
			return  "classpath:com/nwire/studio/tools/nwire-tools-beans.xml";
		}else{
			return "true";
		}
	} 
	public SystemStatus getSystemStatus() {
		return SystemStatus.Valid;
	}
	
	public SystemStatus getFeatureStatus(String featureId) {
		return getSystemStatus();
	}

	public String getSystemStatusMessage() {
		return "";
	}

	public String getType() {
		return "VALID";
	}

	public String getFullName() {
 		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}