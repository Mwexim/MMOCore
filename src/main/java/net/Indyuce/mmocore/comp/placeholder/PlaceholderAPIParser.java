package net.Indyuce.mmocore.comp.placeholder;

import org.bukkit.OfflinePlayer;

import me.clip.placeholderapi.PlaceholderAPI;
import io.lumine.mythic.lib.MythicLib;

public class PlaceholderAPIParser implements PlaceholderParser {
	public PlaceholderAPIParser() {
		new RPGPlaceholders().register();
	}

	@Override
	public String parse(OfflinePlayer player, String string) {
		return MythicLib.plugin.parseColors(PlaceholderAPI.setPlaceholders(player, string.replace("%player%", player.getName())));
	}
}
