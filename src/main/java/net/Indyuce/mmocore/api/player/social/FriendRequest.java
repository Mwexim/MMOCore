package net.Indyuce.mmocore.api.player.social;

import org.bukkit.Sound;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;

public class FriendRequest extends Request {
	private final PlayerData target;

	public FriendRequest(PlayerData creator, PlayerData target) {
		super(creator);

		this.target = target;
	}

	public PlayerData getTarget() {
		return target;
	}

	public void deny() {
		MMOCore.plugin.requestManager.unregisterRequest(getUniqueId());
		target.getPlayer().playSound(target.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
	}

	public void accept() {
		getCreator().setLastFriendRequest(0);
		getCreator().addFriend(target.getUniqueId());
		target.addFriend(getCreator().getUniqueId());
		MMOCore.plugin.configuration.getSimpleMessage("now-friends", "player", target.getPlayer().getName()).send(getCreator().getPlayer());
		MMOCore.plugin.configuration.getSimpleMessage("now-friends", "player", getCreator().getPlayer().getName()).send(target.getPlayer());
		MMOCore.plugin.requestManager.unregisterRequest(getUniqueId());
	}
}