package main.actions;

import java.awt.Color;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import main.database.DBManager;
import main.database.ModerationLogDB;
import main.lib.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class WarnAction extends ModAction {
	public WarnAction(String targetUserID, String targetUserName, String adminID, String adminName, String reason) {
		super(targetUserID, targetUserName, adminID, adminName, reason);
	}

	@Override
	public boolean execute(Guild guild, TextChannel channelOfExecution) {
		DBObject log = ModerationLogDB.generateLog(getTargetUserID(), "Warned", getAdminID(), getReason());
		DBCollection logs = DBManager.getInstance().addDocument(Constants.ModLogs, getTargetUserID(), log);
		int length = (int)log.get("length");
		
		EmbedBuilder result = new EmbedBuilder();
		result.setTitle(String.format("<%s> has been warned.", getTargetUserName()));
		result.setDescription("Reason:\n" + getReason());
		result.addField("Length", Integer.toString(length), true);
		result.addField("Times muted", Long.toString(logs.count()), true);
		result.addField("Discord ID", getTargetUserID(), true);
		result.addField("Name", getTargetUserName(), true);
		result.addField("Moderator ID", getAdminID(), true);
		result.addField("Moderator", getAdminName(), true);
		result.setColor(Color.ORANGE);
		
		TextChannel muteLog = guild.getTextChannelById(Constants.mute_log);
		if (muteLog == null) {
			muteLog = guild.getTextChannelsByName("mute-log", true).get(0);
		}
		muteLog.sendMessage(result.build()).queue();
		
		guild.getMemberById(getTargetUserID()).getUser().openPrivateChannel().queue(pc -> {
			EmbedBuilder warnMsg = new EmbedBuilder();
			warnMsg.setTitle(String.format("You have been warned in %s.", guild.getName()));
			warnMsg.setDescription(getReason());
			warnMsg.setColor(Color.ORANGE);
			warnMsg.setFooter("This has been logged to your record.");
			pc.sendMessage(warnMsg.build()).queue();
		});
		
		return true;
	}
}
