package net.Indyuce.mmocore.command.rpg.debug;

import org.bukkit.command.CommandSender;

import net.Indyuce.mmocore.command.api.CommandMap;

public class DebugCommandMap extends CommandMap {
	public DebugCommandMap(CommandMap parent) {
		super(parent, "debug");

		addFloor(new StatValueCommandMap(this));
	}

	@Override
	public CommandResult execute(CommandSender sender, String[] args) {
		return CommandResult.THROW_USAGE;
	}
}