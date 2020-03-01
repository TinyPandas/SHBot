package main.lib;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class Utils {
	public static CommandArguments getArgs(CommandEvent event, boolean required, boolean delete) {
		String[] args = event.getArgs().split("\\s+");
		Member admin = event.getMember();
		String targetUserQuery = null, messageID = null;
		Member targetUser = null;
		String reason = "No reason provided";
		
		if (delete) {
			if (event.getMessage() != null) 
				event.getMessage().delete().queue();
		}
		
		if (args.length == 1) {
			targetUserQuery = args[0];
		} else if(args.length >= 2) {
			targetUserQuery = args[0];
			messageID = StringUtils.join(Arrays.copyOfRange(args, 1, args.length), " ");
		} else {
			event.reply("You have not provided a userID.");
		}
		
		if (targetUserQuery != null) {
			List<Member> potMembers = FinderUtil.findMembers(targetUserQuery, event.getGuild());
			if (potMembers.size() > 1) {
				event.reply("Multiple members found. Please be more specific.");
			} else if(potMembers.size() == 1) {
				targetUser = potMembers.get(0);
			} else {
				if (required)
					event.reply("No users found with the query provided.");
			}
		}
		
		if (messageID != null) {
			for (TextChannel c : event.getGuild().getTextChannels()) {
				try {
					Message m = c.retrieveMessageById(messageID).complete();
					if (m != null) {
						reason = m.getContentRaw().replaceAll("`", "");
						//TODO log message before delete
						m.delete().queue();
					}
				} catch (IllegalArgumentException iae) {
					reason = messageID;
				} catch (ErrorResponseException ere) {
					//Provided a valid snowflakeID, however no message was found.
					//Nothing to handle.
				}
			}
		}
		
		return new CommandArguments(targetUserQuery, targetUser, admin, reason);
	}
	
	public static boolean hasRoleWithName(Member member, String roleName) {
		List<Role> roles = member.getRoles();
		for (Role r:roles) {
			if (r.getName().equalsIgnoreCase(roleName)) {
				return true;
			}
		}
		return false;
	}
	
	public static String getDate(long milli) {
		Date date = new Date(milli);
		
		return date.toGMTString();
	}
	
	public static String getLength(long length) {
		if (length >= 60) {
			return Long.toString(length / 60) + " hours";
		} else {
			return Long.toString(length) + " minutes";
		}
	}
}
