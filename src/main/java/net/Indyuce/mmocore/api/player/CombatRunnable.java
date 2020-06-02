package net.Indyuce.mmocore.api.player;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.event.PlayerCombatEvent;

public class CombatRunnable extends BukkitRunnable {
	private final PlayerData player;

	private long lastHit = System.currentTimeMillis();

	public CombatRunnable(PlayerData player) {
		this.player = player;

		MMOCore.plugin.configuration.getSimpleMessage("now-in-combat").send(player.getPlayer());
		Bukkit.getPluginManager().callEvent(new PlayerCombatEvent(player, true));
		runTaskTimer(MMOCore.plugin, 20, 20);
	}

	public void update() {
		lastHit = System.currentTimeMillis();
	}

	@Override
	public void run() {
		if (lastHit + (MMOCore.plugin.configuration.combatLogTimer * 1000) < System.currentTimeMillis()) {
			Bukkit.getPluginManager().callEvent(new PlayerCombatEvent(player, false));
			MMOCore.plugin.configuration.getSimpleMessage("leave-combat").send(player.getPlayer());
			close();
		}
	}

	private void close() {
		player.combat = null;
		cancel();
	}
}
