package net.Indyuce.mmocore.gui;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.event.PlayerChangeClassEvent;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.Indyuce.mmocore.api.player.profess.SavedClassInformation;
import net.Indyuce.mmocore.gui.api.EditableInventory;
import net.Indyuce.mmocore.gui.api.GeneratedInventory;
import net.Indyuce.mmocore.gui.api.PluginInventory;
import net.Indyuce.mmocore.gui.api.item.InventoryItem;
import net.Indyuce.mmocore.gui.api.item.Placeholders;
import net.Indyuce.mmocore.gui.api.item.SimplePlaceholderItem;
import net.Indyuce.mmocore.manager.SoundManager;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ClassConfirmation extends EditableInventory {
	public ClassConfirmation() {
		super("class-confirm");
	}

    @Override
    public InventoryItem load(String function, ConfigurationSection config) {
        return function.equalsIgnoreCase("yes") ? new YesItem(config) : new SimplePlaceholderItem(config);
    }

    public GeneratedInventory newInventory(PlayerData data, PlayerClass profess, PluginInventory last) {
        return new ClassConfirmationInventory(data, this, profess, last);
    }

    public class UnlockedItem extends InventoryItem<ClassConfirmationInventory> {

        public UnlockedItem(ConfigurationSection config) {
            super(config);
        }

        @Override
        public Placeholders getPlaceholders(ClassConfirmationInventory inv, int n) {
            PlayerClass profess = inv.profess;
            SavedClassInformation info = inv.getPlayerData().getClassInfo(profess);
            Placeholders holders = new Placeholders();

            int nextLevelExp = inv.getPlayerData().getLevelUpExperience();
            double ratio = (double) info.getExperience() / (double) nextLevelExp;

            StringBuilder bar = new StringBuilder("" + ChatColor.BOLD);
            int chars = (int) (ratio * 20);
            for (int j = 0; j < 20; j++)
                bar.append(j == chars ? "" + ChatColor.WHITE + ChatColor.BOLD : "").append("|");

			holders.register("percent", decimal.format(ratio * 100));
			holders.register("progress", bar.toString());
			holders.register("class", profess.getName());
			holders.register("unlocked_skills", info.getSkillKeys().size());
            holders.register("class_skills", profess.getSkills().size());
            holders.register("next_level", "" + nextLevelExp);
            holders.register("level", info.getLevel());
            holders.register("exp", info.getExperience());
            holders.register("skill_points", info.getSkillPoints());

            return holders;
        }
    }

    public class YesItem extends SimplePlaceholderItem<ClassConfirmationInventory> {
        private final InventoryItem unlocked, locked;

        public YesItem(ConfigurationSection config) {
            super(Material.BARRIER, config);

            Validate.isTrue(config.contains("unlocked"), "Could not load 'unlocked' config");
            Validate.isTrue(config.contains("locked"), "Could not load 'locked' config");

            unlocked = new UnlockedItem(config.getConfigurationSection("unlocked"));
            locked = new InventoryItem<ClassConfirmationInventory>(config.getConfigurationSection("locked")) {

                @Override
                public Placeholders getPlaceholders(ClassConfirmationInventory inv, int n) {
                    Placeholders holders = new Placeholders();
                    holders.register("class", inv.profess.getName());
                    return holders;
                }
            };
        }

        @Override
        public ItemStack display(ClassConfirmationInventory inv, int n) {
            return inv.getPlayerData().hasSavedClass(inv.profess) ? unlocked.display(inv, n) : locked.display(inv, n);
        }
	}

	public class ClassConfirmationInventory extends GeneratedInventory {
		private final PlayerClass profess;
		private final PluginInventory last;

		public ClassConfirmationInventory(PlayerData playerData, EditableInventory editable, PlayerClass profess, PluginInventory last) {
			super(playerData, editable);

			this.profess = profess;
			this.last = last;
		}

		@Override
		public void whenClicked(InventoryClickEvent event, InventoryItem item) {
			if (event.getInventory() != event.getClickedInventory())
				return;

			if (item.getFunction().equals("back"))
				last.open();

			else if (item.getFunction().equals("yes")) {

				PlayerChangeClassEvent called = new PlayerChangeClassEvent(playerData, profess);
				Bukkit.getPluginManager().callEvent(called);
				if (called.isCancelled())
					return;

				playerData.giveClassPoints(-1);
				(playerData.hasSavedClass(profess) ? playerData.getClassInfo(profess)
						: new SavedClassInformation(MMOCore.plugin.dataProvider.getDataManager().getDefaultData())).load(profess, playerData);
				MMOCore.plugin.configManager.getSimpleMessage("class-select", "class", profess.getName()).send(player);
				MMOCore.plugin.soundManager.play(player, SoundManager.SoundEvent.SELECT_CLASS);
				player.closeInventory();
			}
		}

		@Override
		public String calculateName() {
			return getName();
		}
	}
}
