package main.commands.core;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import main.handlers.CommandLoader;
import main.lib.Utils;
import net.dv8tion.jda.api.entities.Member;

/**
 * Version 1.0
 * @author TinyPanda
 */

public class UnloadCommand extends Command {
	public UnloadCommand() {
		name = "unloadCommand";
		help = "unloads a command";
	}
	
	@Override
	protected void execute(CommandEvent event) {
		Member comandee = event.getMember();
		
		if (Utils.hasRoleWithName(comandee, "staff")) {
			CommandLoader loader = CommandLoader.getInstance();
			String commandName = event.getArgs().split(" ")[0];
			
			if (commandName.length() > 0) { 
				boolean result = loader.unloadCommand(event.getClient(), commandName);
				
				if (result) {
					event.reactSuccess();
				} else {
					event.reactError();
					event.reply("Either the command does not exist, or the command was not loaded.");
				}
			}
		}
	}
}
