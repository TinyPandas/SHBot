package main.commands.admin;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import main.actions.ModAction;
import main.actions.RemoveMuteAction;
import main.lib.CommandArguments;
import main.lib.Utils;
import net.dv8tion.jda.api.entities.Member;

public class RemoveMuteCommand extends Command {
	public RemoveMuteCommand() {
		name = "removemute";
		help = "removemute <userQuery> <muteIndex>";
	}
	
	@Override
	protected void execute(CommandEvent event) {
		Member user = event.getMember();
		
		if (Utils.hasRoleWithName(user, "staff")) {
			CommandArguments args = Utils.getArgs(event, true, true);
			String index = event.getMessage().getContentStripped().split("\\s+")[2];
			
			ModAction action = new RemoveMuteAction(args.getTargetUserID(), args.getTargetUser().getEffectiveName(), args.getAdminID(), args.getAdmin().getEffectiveName(), args.getReason(), index);
			action.execute(event.getGuild(), event.getTextChannel());
		}
	}
}
