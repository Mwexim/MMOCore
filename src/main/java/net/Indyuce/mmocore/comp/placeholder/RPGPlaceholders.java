package net.Indyuce.mmocore.comp.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.experience.PlayerProfessions;
import net.Indyuce.mmocore.api.experience.Profession;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.stats.StatType;
import net.Indyuce.mmocore.api.quest.PlayerQuests;
import net.mmogroup.mmolib.api.util.AltChar;

public class RPGPlaceholders
		extends PlaceholderExpansion /** implements Relational */
{

	@Override
	public String getAuthor() {
		return "Indyuce";
	}

	@Override
	public String getIdentifier() {
		return "mmocore";
	}

	@Override
	public String getVersion() {
		return MMOCore.plugin.getDescription().getVersion();
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier) {

		if (identifier.equals("level"))
			return "" + PlayerData.get(player).getLevel();

		else if (identifier.equals("level_percent")) {
			PlayerData playerData = PlayerData.get(player);
			double current = playerData.getExperience(), next = playerData.getLevelUpExperience();
			return MMOCore.plugin.configuration.decimal.format(current / next * 100);
		}

		else if (identifier.equals("combat"))
			return String.valueOf(PlayerData.get(player).isInCombat());

		else if (identifier.equals("health"))
			return StatType.MAX_HEALTH.format(player.getHealth());

		else if (identifier.equals("class"))
			return PlayerData.get(player).getProfess().getName();

		else if (identifier.startsWith("profession_percent_")) {
			PlayerProfessions professions = PlayerData.get(player).getCollectionSkills();
			String name = identifier.substring(19).replace(" ", "-").replace("_", "-").toLowerCase();
			Profession profession = MMOCore.plugin.professionManager.get(name);
			double current = professions.getExperience(profession), next = professions.getLevelUpExperience(profession);
			return MMOCore.plugin.configuration.decimal.format(current / next * 100);
		}

		else if (identifier.startsWith("bound_")) {
			int slot = Math.max(0, Integer.parseInt(identifier.substring(6)) - 1);
			PlayerData playerData = PlayerData.get(player);
			return playerData.hasSkillBound(slot) ? playerData.getBoundSkill(slot).getSkill().getName()
					: MMOCore.plugin.configuration.noSkillBoundPlaceholder;
		}
		
		else if (identifier.startsWith("profession_"))
			return "" + PlayerData.get(player).getCollectionSkills()
					.getLevel(identifier.substring(11).replace(" ", "-").replace("_", "-").toLowerCase());

		else if (identifier.equals("max_health"))
			return StatType.MAX_HEALTH.format(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

		else if (identifier.equals("experience"))
			return "" + PlayerData.get(player).getExperience();

		else if (identifier.equals("next_level"))
			return "" + PlayerData.get(player).getLevelUpExperience();

		else if (identifier.equals("class_points"))
			return "" + PlayerData.get(player).getClassPoints();

		else if (identifier.equals("skill_points"))
			return "" + PlayerData.get(player).getSkillPoints();

		else if (identifier.equals("attribute_points"))
			return "" + PlayerData.get(player).getAttributePoints();

		else if (identifier.equals("attribute_reallocation_points"))
			return "" + PlayerData.get(player).getAttributeReallocationPoints();

		else if (identifier.startsWith("attribute_"))
			return String.valueOf(PlayerData.get(player).getAttributes()
					.getAttribute(MMOCore.plugin.attributeManager.get(identifier.substring(10).toLowerCase().replace("_", "-"))));

		else if (identifier.equals("mana"))
			return MMOCore.plugin.configuration.decimal.format(PlayerData.get(player).getMana());

		else if (identifier.equals("mana_bar")) {
			PlayerData data = PlayerData.get(player);
			return data.getProfess().getManaDisplay().generateBar(data.getMana(), data.getStats().getStat(StatType.MAX_MANA));
		}

		else if (identifier.equals("stamina"))
			return MMOCore.plugin.configuration.decimal.format(PlayerData.get(player).getStamina());

		else if (identifier.equals("stamina_bar")) {
			String format = "";
			PlayerData data = PlayerData.get(player);
			double ratio = 20 * data.getStamina() / data.getStats().getStat(StatType.MAX_STAMINA);
			for (double j = 1; j < 20; j++)
				format += (ratio >= j ? MMOCore.plugin.configuration.staminaFull
						: ratio >= j - .5 ? MMOCore.plugin.configuration.staminaHalf : MMOCore.plugin.configuration.staminaEmpty)
						+ AltChar.listSquare;
			return format;
		}

		else if (identifier.startsWith("stat_")) {
			StatType type = StatType.valueOf(identifier.substring(5).toUpperCase());
			return type == null ? "Invalid Stat" : type.format(PlayerData.get(player).getStats().getStat(type));
		}

		else if (identifier.equals("stellium"))
			return MMOCore.plugin.configuration.decimal.format(PlayerData.get(player).getStellium());

		else if (identifier.equals("stellium_bar")) {
			String format = "";
			PlayerData data = PlayerData.get(player);
			double ratio = 20 * data.getStellium() / data.getStats().getStat(StatType.MAX_STELLIUM);
			for (double j = 1; j < 20; j++)
				format += (ratio >= j ? ChatColor.BLUE : ratio >= j - .5 ? ChatColor.AQUA : ChatColor.WHITE) + AltChar.listSquare;
			return format;
		}

		else if (identifier.equals("quest")) {
			PlayerQuests data = PlayerData.get(player).getQuestData();
			return data.hasCurrent() ? data.getCurrent().getQuest().getName() : "None";
		}

		else if (identifier.equals("quest_progress")) {
			PlayerQuests data = PlayerData.get(player).getQuestData();
			return data.hasCurrent()
					? MMOCore.plugin.configuration.decimal
							.format((int) (double) data.getCurrent().getObjectiveNumber() / data.getCurrent().getQuest().getObjectives().size() * 100)
					: "0";
		}

		else if (identifier.equals("quest_objective")) {
			PlayerQuests data = PlayerData.get(player).getQuestData();
			return data.hasCurrent() ? data.getCurrent().getFormattedLore() : "None";
		}

		else if (identifier.startsWith("guild_")) {
			String placeholder = identifier.substring(6);
			PlayerData data = PlayerData.get(player);
			if (data.getGuild() == null)
				return "";

			if (placeholder.equalsIgnoreCase("name"))
				return data.getGuild().getName();
			else if (placeholder.equalsIgnoreCase("tag"))
				return data.getGuild().getTag();
			else if (placeholder.equalsIgnoreCase("leader"))
				return Bukkit.getOfflinePlayer(data.getGuild().getOwner()).getName();
			else if (placeholder.equalsIgnoreCase("members"))
				return "" + data.getGuild().getMembers().count();
			else if (placeholder.equalsIgnoreCase("online_members"))
				return "" + data.getGuild().getMembers().countOnline();
		}

		return null;
	}
}
