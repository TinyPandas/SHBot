package main.events;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import main.actions.infractions.MuteAction;
import main.actions.lib.ModAction;
import main.database.DBManager;
import main.handlers.MuteHandler;
import main.lib.Constants;
import main.lib.MessageObject;
import main.lib.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageEvent extends ListenerAdapter {
	HashMap<String, List<MessageObject>> track = new HashMap<>();
	HashMap<String, List<String>> msgHist = new HashMap<>();
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		super.onGuildMessageReceived(event);
		Member member = event.getMember();
		
		if (member.getUser() == null || member.getUser().isBot() || member.getUser().isFake()) {
			return;
		}
		
		/** Log Images */ 
		List<String> attachments = new ArrayList<String>();
		
		Message m = event.getMessage();
		if (m != null) {
			attachments = Utils.downloadAttachments(m);
		}
		
		User admin = event.getJDA().getSelfUser();
		Member admin2 = event.getGuild().getMember(admin);
		
		DBManager manager = DBManager.getInstance();
		DBCollection col = manager.getCollection(Constants.MainDB, Constants.FilterList);
		DBObject messageLog = new BasicDBObject()
							.append("messageID", event.getMessageId())
							.append("authorID", member.getId())
							.append("original", event.getMessage().getContentDisplay())
							.append("current", event.getMessage().getContentDisplay())
							.append("channelID", event.getChannel().getId())
							.append("edited", false)
							.append("deleted", false);
		
		if (attachments.size() > 0) {
			((BasicDBObject)messageLog).append("images", String.join(", ", attachments));
		}
		
		manager.addDocument(Constants.MainDB, Constants.ChatLogs, messageLog);
		
		if (!(track.containsKey(member.getId()))) {
			track.put(member.getId(), new ArrayList<MessageObject>());
		}
		
		//Sending Messages too quickly
		if (!(MuteHandler.isMuted(member.getId()))) {
			MessageObject msg = new MessageObject(member.getId(), System.currentTimeMillis());
			List<MessageObject> hist = track.get(member.getId());
			hist.add(msg);
			
			for (Iterator<MessageObject> it = hist.iterator();it.hasNext();) {
				MessageObject obj = it.next();
				if (obj.isDeleted()) {
					it.remove();
				}
			}
			
			if (hist.size() >= 5) {
				ModAction mute = new MuteAction(member.getId(), member.getEffectiveName(), admin.getId(), admin2.getEffectiveName(), "You are sending messages too quickly.", new ArrayList<>(), null);
				((MuteAction)mute).deleteContext(event.getChannel(), event.getMessageId(), hist.size(), true);
				//mute.execute(event.getGuild(), event.getChannel());
			}
			
			track.replace(member.getId(), hist);
		}
		
		//Sending repeating messages
		if (!(msgHist.containsKey(member.getId()))) {
			msgHist.put(member.getId(), new ArrayList<>());
		}
		
		if (!(MuteHandler.isMuted(member.getId()))) {
			List<String> pastMessages = msgHist.get(member.getId());
			String currentMessage = event.getMessage().getContentStripped();
			
			int matches = 0;
			
			if (pastMessages.size() > 0) { 
				for (int i=pastMessages.size();i>0;i--) {
					if (pastMessages.get(i-1).equalsIgnoreCase(currentMessage)) {
						matches++;
					}
				}
			}
			
			pastMessages.add(currentMessage);
			
			if (pastMessages.size() > 10) {
				pastMessages.remove(0); //First in first out
			}
			
			if (matches >= 3) {
				ModAction mute = new MuteAction(member.getId(), member.getEffectiveName(), admin.getId(), admin2.getEffectiveName(), "Sending the same message repeatedly. \n " + currentMessage + " (x" + matches + ")", new ArrayList<>(), null);
				((MuteAction)mute).deleteContext(event.getChannel(), event.getMessageId(), matches, false);
				//mute.execute(event.getGuild(), event.getChannel());
			}
			
			msgHist.replace(member.getId(), pastMessages);
		}
		
		//Message filtered words.
		String[] words = event.getMessage().getContentStripped().split("\\s+");
		
		boolean flagged = false;
		int count = 0;
		
		for (String word:words) {
			DBObject query = new BasicDBObject("word", word);
			DBCursor cur = col.find(query);
			
			if (cur.size() > 0) {
				count += 1;
				flagged = true;
			}
		}
		
		if (flagged && count > 0) {
			event.getMessage().delete().queue();
			
			TextChannel com = event.getGuild().getTextChannelById(Constants.commands);
			if (com == null) {
				com = event.getGuild().getTextChannelsByName("commands", true).get(0);
			}
			com.sendMessage(String.format("%s has been flagged by word filter with %s word(s).", member.getEffectiveName(), count)).queue();
		}
	}

	@Override
	public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
		super.onGuildMessageUpdate(event);
		Member member = event.getMember();
		
		if (member.getUser().isBot() || member.getUser().isFake()) {
			return;
		}
		
		DBManager manager = DBManager.getInstance();
		DBCollection col = manager.getCollection(Constants.MainDB, Constants.ChatLogs);
		DBObject query = new BasicDBObject("messageID", event.getMessageId());
		DBCursor cur = col.find(query);
		DBObject obj = cur.next();
		String oldContent = "";
		
		if (obj != null) {
			oldContent = obj.get("current").toString();
			
			obj.put("edited", true);
			obj.put("current", event.getMessage().getContentDisplay());
			col.update(query, obj);
		}
		
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("**Message Updated**");
		builder.setColor(Color.YELLOW);
		builder.addField("User", event.getMember().getEffectiveName(), true);
		builder.addField("Channel", event.getChannel().getAsMention(), true);
		builder.addField("Before", oldContent, false);
		builder.addField("After", event.getMessage().getContentDisplay(), false);
		
		TextChannel log = event.getGuild().getTextChannelById(Constants.chat_log);
		if (log == null) {
			log = event.getGuild().getTextChannelsByName("chat-log", true).get(0);
		}
		log.sendMessage(builder.build()).queue();
		
		DBObject dbLog = new BasicDBObject()
				.append("messageID", event.getMessage().getId())
				.append("authorID", event.getMember().getId())
				.append("before", oldContent)
				.append("after", event.getMessage().getContentDisplay());
		
		manager.addDocument(Constants.MainDB, Constants.ChatEditLogs, dbLog);
	}

	@Override
	public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
		super.onGuildMessageDelete(event);	
		
		DBManager manager = DBManager.getInstance();
		DBCollection col = manager.getCollection(Constants.MainDB, Constants.ChatLogs);
		DBObject query = new BasicDBObject("messageID", event.getMessageId());
		DBCursor cur = col.find(query);
		DBObject obj = cur.next();
		String oldContent = "";
		
		if (obj != null) {
			oldContent = obj.get("current").toString();
			
			obj.put("deleted", true);
			col.update(query, obj);
		}
		
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("**Message Deleted**");
		builder.setColor(Color.RED);
		builder.addField("User", event.getGuild().getMemberById(obj.get("authorID").toString()).getEffectiveName(), true);
		builder.addField("Channel", event.getChannel().getAsMention(), true);
		builder.addField("Content", oldContent, false);
		
		TextChannel log = event.getGuild().getTextChannelById(Constants.chat_log);
		if (log == null) {
			log = event.getGuild().getTextChannelsByName("chat-log", true).get(0);
		}
		log.sendMessage(builder.build()).queue();
		
		DBObject dbLog = new BasicDBObject()
				.append("messageID", obj.get("messageID"))
				.append("authorID", obj.get("authorID"))
				.append("before", oldContent);
		
		manager.addDocument(Constants.MainDB, Constants.ChatDelLogs, dbLog);
	}
}