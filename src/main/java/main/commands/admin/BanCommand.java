package main.commands.admin;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import main.actions.infractions.BanAction;
import main.actions.lib.ModAction;
import main.lib.CommandArguments;
import main.lib.Utils;
import net.dv8tion.jda.api.entities.Member;

/**
 * Version 1.0
 * @author TinyPanda
 */

public class BanCommand extends Command {
	public BanCommand() {
		name = "ban";
		help = "ban <userQuery> <reason>";
		category = new Category("staff");
	}
	
	@Override
	protected void execute(CommandEvent event) {
		Member user = event.getMember();
		
		if (Utils.hasRoleWithName(user, "staff")) {
			CommandArguments args = Utils.getArgs(event, false, true);
			
			if (args.getTargetUser().getUser().isBot()) {
				return;
			}
		
			ModAction action = new BanAction((args.getTargetUserID().equalsIgnoreCase("-1")) ? args.getQuery() : args.getTargetUserID(), (args.getTargetUser() != null) ? args.getTargetUser().getEffectiveName() : args.getQuery(), args.getAdminID(), args.getAdmin().getEffectiveName(), args.getReason(), args.getImages(), args.getMessageID());
			action.execute(event.getGuild(), event.getTextChannel());
		}
	}
}
