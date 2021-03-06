package main.actions.infractions;

import java.awt.Color;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import main.actions.lib.InfractionAction;
import main.database.DBManager;
import main.database.ModerationLogDB;
import main.handlers.MuteHandler;
import main.lib.Constants;
import main.lib.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class MuteAction extends InfractionAction {
	public MuteAction(String targetUserID, String targetUserName, String adminID, String adminName, String reason, List<String> images, String messageID) {
		super(targetUserID, targetUserName, adminID, adminName, reason, images, messageID);
	}
	
	private String contextString = "";
	
	public void deleteContext(TextChannel channelOfExecution, String messageId, int limit, final boolean logEach) {
		if (limit == -1) {
			limit = 10;
		}
		final int countLimit = limit;
		final long start = System.currentTimeMillis() / 1000L;
		
		channelOfExecution.getHistoryBefore(messageId, 50).queue(history -> {
			List<Message> retrieved = history.getRetrievedHistory();
			int count = 0;
			for (Message m:retrieved) {
				long postDate = m.getTimeCreated().toInstant().getEpochSecond();
				long dif = start-postDate;
				//Last minute
				if (dif > 60) {
					break;
				}
				
				
				TextChannel chatLog = channelOfExecution.getGuild().getTextChannelById(Constants.chat_log);
				if (chatLog == null) {
					chatLog = channelOfExecution.getGuild().getTextChannelsByName("chat-log", true).get(0);
				}
				if (m.getAuthor().getId().equalsIgnoreCase(getTargetUserID()) && count < countLimit) {
					EmbedBuilder builder = new EmbedBuilder();
					builder.setTitle("**Message Deleted**");
					builder.addField("User", m.getMember().getEffectiveName(), true);
					builder.addField("Channel", m.getTextChannel().getAsMention(), true);
					builder.addField("Content", m.getContentRaw(), false);
					
					chatLog.sendMessage(builder.build()).queue();
					
					if (logEach) {
						contextString = m.getContentRaw().replaceAll("'", "") + "\n" + contextString;
					}
					
					m.delete().queue();
				}
			}
			
			if (contextString.length() > 0) {
				contextString += "\n" + getReason();
				setReason("``` \n" + contextString + " \n```");
			}
			
			execute(channelOfExecution.getGuild(), channelOfExecution);
		});
	}
	
	@Override
	public boolean execute(Guild guild, TextChannel channelOfExecution) {
		// below currently accepts y,d,m,h (years, days, minutes, hours). Ex: ;mute user 15m reason
		long length = -1;
		String reason = getReason();
		String firstWord = reason.contains(" ") ? reason.split(" ")[0] : reason;
		if (firstWord.length() > 0) {
			Pattern p = Pattern.compile("[0-9]+[ydmh]");
			Matcher m = p.matcher(firstWord);
			if (m.find()) {
                String match = m.group(0);
				System.out.println("Match: "+match);
				String numStr = match.substring(0,match.length() - 1);
				int number = Integer.parseInt(numStr);
				char lastChar = match.charAt(match.length() - 1);
				switch (lastChar) {
					case 'm':
						break; // already in minutes
					case 'h':
						number *= 60;
						break;
					case 'd':
						number *= 60*24;
						break;
					case 'y':
						number *= 60*24*365;
						break;
					default:
						break;
				}
				length = number;
				String [] arr = reason.split(" ", 2);
				if (arr.length > 1) {
					reason = arr[1];
				} else {
					reason = "";
				}
            }
		}
        
		DBObject log = ModerationLogDB.generateLog(getTargetUserID(), "Muted", getAdminID(), reason, getImages(), getMessageID(), length);
		DBCollection logs = DBManager.getInstance().addDocument(Constants.ModLogs, getTargetUserID(), log);
		length = (long)Integer.toUnsignedLong((int)log.get("length"));
		EmbedBuilder result = new EmbedBuilder();
		result.setTitle(String.format("<%s> has been muted.", getTargetUserName()));
		result.setDescription(log.get("reason").toString());
		result.addField("Length", Utils.getLength(length), true);
		result.addField("Times muted", Long.toString(logs.count() - 1), true); //-1 to not count this instance.
		result.addField("Discord ID", getTargetUserID(), true);
		result.addField("Name", getTargetUserName(), true);
		result.addField("Moderator ID", getAdminID(), true);
		result.addField("Moderator", getAdminName(), true);
		result.setColor(Color.RED);
		
		MuteHandler.mute(guild.getId(), getTargetUserID(), System.currentTimeMillis() + ((length*1000) * 60));// + (length*60*1000));
		
		TextChannel muteLog = guild.getTextChannelById(Constants.mute_log);
		if (muteLog == null) { 
			muteLog = guild.getTextChannelsByName("mute-log", true).get(0);
		}

		guild.addRoleToMember(getTargetUserID(), guild.getRolesByName("muted", true).get(0)).queue();
		try {
			guild.mute(guild.getMemberById(getTargetUserID()), true).queue();
		} catch (Exception e) {
			//May not be in voice channel.
		}
		
		muteLog.sendMessage(result.build()).queue();

		if (channelOfExecution.getParent().getId().equalsIgnoreCase("356054271680184324")) {
			channelOfExecution.sendMessage(result.build()).queue();
		}
		
		guild.getMemberById(getTargetUserID()).getUser().openPrivateChannel().queue(pc -> {
			result.clearFields();
			result.setTitle(String.format("You have been muted in %s.", guild.getName()));
			//result.addField("Length", Utils.getLength(length), true);
			result.addField("Times muted", Long.toString(logs.count()), true);
			result.setFooter("Take this time to think about what you did. You only get so many chances...");
			pc.sendMessage(result.build()).queue();
		});
		
		return true;
	}
}
