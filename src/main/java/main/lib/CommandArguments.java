package main.lib;

import net.dv8tion.jda.api.entities.Member;

public class CommandArguments {
	String query;
	String targetUserID;
	Member targetUser;
	String adminID;
	Member admin;
	String reason;
	
	public CommandArguments(String query, Member targetUser, Member admin, String reason) {
		this.query = query;
		this.targetUser = targetUser;
		this.targetUserID = (targetUser != null) ? targetUser.getId() : "-1";
		this.admin = admin;
		this.adminID = admin.getId();
		this.reason = reason;
	}
	
	public String getQuery() {
		return query;
	}

	public Member getTargetUser() {
		return targetUser;
	}
	
	public String getTargetUserID() { 
		return targetUserID;
	}

	public Member getAdmin() {
		return admin;
	}
	
	public String getAdminID() { 
		return adminID;
	}

	public String getReason() {
		return reason;
	}
}
