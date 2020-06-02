package net.Indyuce.mmocore.api.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.ConfigMessage;
import net.Indyuce.mmocore.api.Waypoint;
import net.Indyuce.mmocore.api.event.PlayerCastSkillEvent;
import net.Indyuce.mmocore.api.event.PlayerExperienceGainEvent;
import net.Indyuce.mmocore.api.event.PlayerLevelUpEvent;
import net.Indyuce.mmocore.api.event.PlayerRegenResourceEvent;
import net.Indyuce.mmocore.api.experience.PlayerProfessions;
import net.Indyuce.mmocore.api.player.attribute.PlayerAttribute;
import net.Indyuce.mmocore.api.player.attribute.PlayerAttributes;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.Indyuce.mmocore.api.player.profess.SavedClassInformation;
import net.Indyuce.mmocore.api.player.profess.Subclass;
import net.Indyuce.mmocore.api.player.profess.resource.PlayerResource;
import net.Indyuce.mmocore.api.player.social.FriendRequest;
import net.Indyuce.mmocore.api.player.social.Party;
import net.Indyuce.mmocore.api.player.social.guilds.Guild;
import net.Indyuce.mmocore.api.player.stats.PlayerStats;
import net.Indyuce.mmocore.api.player.stats.StatType;
import net.Indyuce.mmocore.api.quest.PlayerQuests;
import net.Indyuce.mmocore.api.skill.PlayerSkillData;
import net.Indyuce.mmocore.api.skill.Skill;
import net.Indyuce.mmocore.api.skill.Skill.SkillInfo;
import net.Indyuce.mmocore.api.skill.SkillResult;
import net.Indyuce.mmocore.api.skill.SkillResult.CancelReason;
import net.Indyuce.mmocore.api.util.math.particle.SmallParticleEffect;
import net.Indyuce.mmocore.listener.SpellCast.SkillCasting;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.mmogroup.mmolib.MMOLib;
import net.mmogroup.mmolib.api.player.MMOData;
import net.mmogroup.mmolib.api.stat.StatMap;
import net.mmogroup.mmolib.version.VersionSound;

public class PlayerData extends OfflinePlayerData {

	/*
	 * is updated everytime the player joins the server. it is kept when the
	 * player is offline so the plugin can use #isOnline to check if the player
	 * is online
	 */
	private Player player;

	/*
	 * 'profess' can be null, you need to retrieve the player class using the
	 * getProfess() method which should never return null if the plugin is
	 * configured right
	 */
	private PlayerClass profess;
	private int level, experience, classPoints, skillPoints, attributePoints, attributeReallocationPoints;// skillReallocationPoints,
	private double mana, stamina, stellium;
	private Party party;
	private Guild guild;

	private final PlayerQuests questData;
	private final PlayerStats playerStats;
	private final List<UUID> friends = new ArrayList<>();
	private final Set<String> waypoints = new HashSet<>();
	private final Map<String, Integer> skills = new HashMap<>();
	private final List<SkillInfo> boundSkills = new ArrayList<>();
	private final PlayerProfessions collectSkills = new PlayerProfessions(this);
	private final PlayerSkillData skillData = new PlayerSkillData(this);
	private final PlayerAttributes attributes = new PlayerAttributes(this);
	private final Map<String, SavedClassInformation> classSlots = new HashMap<>();

	private long lastWaypoint, lastLogin, lastFriendRequest, actionBarTimeOut;

	/*
	 * NON-FINAL player data stuff made public to facilitate field change
	 */
	public int skillGuiDisplayOffset;
	public SkillCasting skillCasting;
	public boolean nocd;
	public CombatRunnable combat;

	public PlayerData(Player player) {
		super(player.getUniqueId());

		setPlayer(player);
		playerStats = new PlayerStats(this);
		questData = new PlayerQuests(this);
	}

	/*
	 * easily solves some issues where other plugins use PlayerData.get
	 */
	public static final PlayerData NOT_LOADED = new PlayerData();

	@Deprecated
	private PlayerData() {
		super(UUID.randomUUID());

		playerStats = new PlayerStats(this, new StatMap(new MMOData().setMMOCore(this)));
		questData = new PlayerQuests(this, null);
	}

	/*
	 * update all references after /mmocore reload so there can be garbage
	 * collection with old plugin objects like class or skill instances.
	 */
	public void update() {

		try {
			profess = profess == null ? null : MMOCore.plugin.classManager.get(profess.getId());
		} catch (NullPointerException exception) {
			MMOCore.log(Level.SEVERE, "[Userdata] Could not find class " + getProfess().getId() + " while refreshing player data.");
		}

		int j = 0;
		while (j < boundSkills.size())
			try {
				boundSkills.set(j, getProfess().getSkill(boundSkills.get(j).getSkill()));
				j++;
			} catch (NullPointerException notFound) {
				boundSkills.remove(j);
				MMOCore.log(Level.SEVERE, "[Userdata] Could not find skill " + boundSkills.get(j).getSkill().getId() + " in class "
						+ getProfess().getId() + " while refreshing player data.");
			}
	}

	public static PlayerData get(OfflinePlayer player) {
		return get(player.getUniqueId());
	}

	public static PlayerData get(UUID uuid) {
		return MMOCore.plugin.dataProvider.getDataManager().get(uuid);
	}

	public static Collection<PlayerData> getAll() {
		return MMOCore.plugin.dataProvider.getDataManager().getLoaded();
	}

	public void setPlayer(Player player) {
		this.player = player;
		this.lastLogin = System.currentTimeMillis();
	}

	public List<UUID> getFriends() {
		return friends;
	}

	public PlayerProfessions getCollectionSkills() {
		return collectSkills;
	}

	public PlayerQuests getQuestData() {
		return questData;
	}

	public Player getPlayer() {
		return player;
	}

	@Override
	public long getLastLogin() {
		return lastLogin;
	}

	public long getLastFriendRequest() {
		return lastFriendRequest;
	}

	@Override
	public int getLevel() {
		return Math.max(1, level);
	}

	public Party getParty() {
		return party;
	}

	public boolean hasGuild() {
		return guild != null;
	}

	public Guild getGuild() {
		return guild;
	}

	public int getClassPoints() {
		return classPoints;
	}

	public int getSkillPoints() {
		return skillPoints;
	}

	/*
	 * returns level up needed in order to level up
	 */
	public int getLevelUpExperience() {
		return getProfess().getExpCurve().getExperience(getLevel() + 1);
	}

	// public int getSkillReallocationPoints() {
	// return skillReallocationPoints;
	// }

	public int getAttributePoints() {
		return attributePoints;
	}

	public int getAttributeReallocationPoints() {
		return attributeReallocationPoints;
	}

	public boolean hasParty() {
		return party != null;
	}

	public boolean inGuild() {
		return guild != null;
	}

	public boolean isOnline() {
		return player.isOnline();
	}

	public void setLevel(int level) {
		this.level = Math.max(1, level);
		getStats().updateStats();
	}

	public void giveLevels(int value) {
		int total = 0;
		while (value-- > 0)
			total += getProfess().getExpCurve().getExperience(getLevel() + value + 1);
		giveExperience(total);
	}

	public void setExperience(int value) {
		experience = Math.max(0, value);
		refreshVanillaExp();
	}

	public void refreshVanillaExp() {
		if (MMOCore.plugin.configuration.overrideVanillaExp) {
			player.setLevel(getLevel());
			player.setExp(Math.max(0, Math.min(1, (float) experience / (float) getLevelUpExperience())));
		}
	}

	// public void setSkillReallocationPoints(int value) {
	// skillReallocationPoints = Math.max(0, value);
	// }

	public void setAttributePoints(int value) {
		attributePoints = Math.max(0, value);
	}

	public void setAttributeReallocationPoints(int value) {
		attributeReallocationPoints = Math.max(0, value);
	}

	public void setSkillPoints(int value) {
		skillPoints = Math.max(0, value);
	}

	public void setClassPoints(int value) {
		classPoints = Math.max(0, value);
	}

	public boolean hasSavedClass(PlayerClass profess) {
		return classSlots.containsKey(profess.getId());
	}

	public Set<String> getSavedClasses() {
		return classSlots.keySet();
	}

	public SavedClassInformation getClassInfo(PlayerClass profess) {
		return getClassInfo(profess.getId());
	}

	public SavedClassInformation getClassInfo(String profess) {
		return classSlots.get(profess);
	}

	public void applyClassInfo(PlayerClass profess, SavedClassInformation info) {
		classSlots.put(profess.getId(), info);
	}

	public void unloadClassInfo(PlayerClass profess) {
		classSlots.remove(profess.getId());
	}

	public Set<String> getWaypoints() {
		return waypoints;
	}

	public boolean hasWaypoint(Waypoint waypoint) {
		return waypoints.contains(waypoint.getId());
	}

	public void unlockWaypoint(Waypoint waypoint) {
		waypoints.add(waypoint.getId());
	}

	public long getWaypointCooldown() {
		return Math.max(0, lastWaypoint + 5000 - System.currentTimeMillis());
	}

	public void heal(double heal) {
		double newest = Math.max(0, Math.min(player.getHealth() + heal, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
		if (player.getHealth() == newest)
			return;

		PlayerRegenResourceEvent event = new PlayerRegenResourceEvent(this, PlayerResource.HEALTH, heal);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;

		getPlayer().setHealth(newest);
	}

	public void addFriend(UUID uuid) {
		friends.add(uuid);
	}

	@Override
	public void removeFriend(UUID uuid) {
		friends.remove(uuid);
	}

	@Override
	public boolean hasFriend(UUID uuid) {
		return friends.contains(uuid);
	}

	public void setParty(Party party) {
		this.party = party;
	}

	public void setGuild(Guild guild) {
		this.guild = guild;
	}

	public void log(Level level, String message) {
		MMOCore.plugin.getLogger().log(level, "[Userdata:" + player.getName() + "] " + message);
	}

	public void setLastFriendRequest(long ms) {
		lastFriendRequest = Math.max(0, ms);
	}

	public void sendFriendRequest(PlayerData target) {
		setLastFriendRequest(System.currentTimeMillis());

		FriendRequest request = new FriendRequest(this, target);
		new ConfigMessage("friend-request").addPlaceholders("player", getPlayer().getName(), "uuid", request.getUniqueId().toString())
				.sendAsJSon(target.getPlayer());
		MMOCore.plugin.requestManager.registerRequest(request);
	}

	public void warp(Waypoint waypoint) {

		/*
		 * this cooldown is only used internally to make sure the player is not
		 * spamming waypoints. there is no need to reset it when resetting the
		 * player waypoints data
		 */
		lastWaypoint = System.currentTimeMillis();

		giveStellium(-waypoint.getStelliumCost());

		new BukkitRunnable() {
			int x = player.getLocation().getBlockX(), y = player.getLocation().getBlockY(), z = player.getLocation().getBlockZ(), t;

			public void run() {
				if (player.getLocation().getBlockX() != x || player.getLocation().getBlockY() != y || player.getLocation().getBlockZ() != z) {
					player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, .5f);
					MMOCore.plugin.configuration.getSimpleMessage("warping-canceled").send(player);
					giveStellium(waypoint.getStelliumCost());
					cancel();
					return;
				}

				MMOCore.plugin.configuration.getSimpleMessage("warping-comencing", "left", "" + ((120 - t) / 20)).send(player);
				if (t++ >= 100) {
					player.teleport(waypoint.getLocation());
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1, false, false));
					player.playSound(player.getLocation(), VersionSound.ENTITY_ENDERMAN_TELEPORT.toSound(), 1, .5f);
					cancel();
					return;
				}

				player.playSound(player.getLocation(), VersionSound.BLOCK_NOTE_BLOCK_BELL.toSound(), 1, (float) (t / Math.PI * .015 + .5));
				double r = Math.sin((double) t / 100 * Math.PI);
				for (double j = 0; j < Math.PI * 2; j += Math.PI / 4)
					MMOLib.plugin.getVersion().getWrapper().spawnParticle(Particle.REDSTONE,
							player.getLocation().add(Math.cos((double) t / 20 + j) * r, (double) t / 50, Math.sin((double) t / 20 + j) * r), 1.25f,
							Color.PURPLE);
			}
		}.runTaskTimer(MMOCore.plugin, 0, 1);
	}

	public boolean hasReachedMaxLevel() {
		return getProfess().getMaxLevel() > 0 && getLevel() >= getProfess().getMaxLevel();
	}

	public void giveExperience(int value) {
		giveExperience(value, null);
	}

	public void giveExperience(int value, Location loc) {
		if (hasReachedMaxLevel()) {
			setExperience(0);
			return;
		}

		// display hologram
		if (MMOCore.plugin.getConfig().getBoolean("display-exp-holograms"))
			if (loc != null && MMOCore.plugin.hologramSupport != null)
				MMOCore.plugin.hologramSupport.displayIndicator(loc.add(.5, 1.5, .5),
						MMOCore.plugin.configuration.getSimpleMessage("exp-hologram", "exp", "" + value).message(), getPlayer());

		value = MMOCore.plugin.boosterManager.calculateExp(null, value);
		value *= 1 + getStats().getStat(StatType.ADDITIONAL_EXPERIENCE) / 100;

		PlayerExperienceGainEvent event = new PlayerExperienceGainEvent(this, value);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;

		experience += event.getExperience();

		int oldLevel = level, needed;
		while (experience >= (needed = getLevelUpExperience())) {

			if (hasReachedMaxLevel()) {
				experience = 0;
				break;
			}

			experience -= needed;
			level = getLevel() + 1;
		}

		if (level > oldLevel) {
			Bukkit.getPluginManager().callEvent(new PlayerLevelUpEvent(this, null, oldLevel, level));
			new ConfigMessage("level-up").addPlaceholders("level", "" + level).send(player);
			player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
			new SmallParticleEffect(player, Particle.SPELL_INSTANT);
			getStats().updateStats();
		}

		refreshVanillaExp();
	}

	public int getExperience() {
		return experience;
	}

	@Override
	public PlayerClass getProfess() {
		return profess == null ? MMOCore.plugin.classManager.getDefaultClass() : profess;
	}

	public void giveMana(double amount) {
		double newest = Math.max(0, Math.min(getStats().getStat(StatType.MAX_MANA), mana + amount));
		if (mana == newest)
			return;

		PlayerRegenResourceEvent event = new PlayerRegenResourceEvent(this, PlayerResource.MANA, amount);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;

		mana = newest;
	}

	public void giveStamina(double amount) {
		double newest = Math.max(0, Math.min(getStats().getStat(StatType.MAX_STAMINA), stamina + amount));
		if (stamina == newest)
			return;

		PlayerRegenResourceEvent event = new PlayerRegenResourceEvent(this, PlayerResource.STAMINA, amount);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;

		stamina = newest;
	}

	public void giveStellium(double amount) {
		double newest = Math.max(0, Math.min(getStats().getStat(StatType.MAX_STELLIUM), stellium + amount));
		if (stellium == newest)
			return;

		PlayerRegenResourceEvent event = new PlayerRegenResourceEvent(this, PlayerResource.STELLIUM, amount);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return;

		stellium = newest;
	}

	public double getMana() {
		return mana;
	}

	public double getStamina() {
		return stamina;
	}

	public double getStellium() {
		return stellium;
	}

	public PlayerStats getStats() {
		return playerStats;
	}

	public PlayerAttributes getAttributes() {
		return attributes;
	}

	public void setMana(double amount) {
		mana = Math.max(0, Math.min(amount, getStats().getStat(StatType.MAX_MANA)));
	}

	public void setStamina(double amount) {
		stamina = Math.max(0, Math.min(amount, getStats().getStat(StatType.MAX_STAMINA)));
	}

	public void setStellium(double amount) {
		stellium = Math.max(0, Math.min(amount, getStats().getStat(StatType.MAX_STELLIUM)));
	}

	public boolean isCasting() {
		return skillCasting != null;
	}

	/*
	 * returns if the action bar is not being used to display anything else and
	 * if the general info action bar can be displayed
	 */
	public boolean canSeeActionBar() {
		return actionBarTimeOut < System.currentTimeMillis();
	}

	public void setActionBarTimeOut(long timeOut) {
		actionBarTimeOut = System.currentTimeMillis() + (timeOut * 50);
	}

	public void displayActionBar(String message) {
		setActionBarTimeOut(60);
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
	}

	@Deprecated
	public void setAttribute(PlayerAttribute attribute, int value) {
		setAttribute(attribute.getId(), value);
	}

	@Deprecated
	public void setAttribute(String id, int value) {
		attributes.setBaseAttribute(id, value);
	}

	@Deprecated
	public Map<String, Integer> mapAttributePoints() {
		return getAttributes().mapPoints();
	}

	public int getSkillLevel(Skill skill) {
		return skills.getOrDefault(skill.getId(), 1);
	}

	public void setSkillLevel(Skill skill, int level) {
		setSkillLevel(skill.getId(), level);
	}

	public void setSkillLevel(String skill, int level) {
		skills.put(skill, level);
	}

	public void resetSkillLevel(String skill) {
		skills.remove(skill);
	}

	/*
	 * better use getProfess().findSkill(skill).isPresent()
	 */
	@Deprecated
	public boolean hasSkillUnlocked(Skill skill) {
		return getProfess().hasSkill(skill.getId()) && hasSkillUnlocked(getProfess().getSkill(skill.getId()));
	}

	/*
	 * (bug fix) any skill, when the player has the right level is instantly
	 * unlocked, therefore one must NOT check if the player has unlocked the
	 * skill by checking if the skills map contains the skill id as key. this
	 * only checks if the player has spent any skill point.
	 */
	public boolean hasSkillUnlocked(SkillInfo info) {
		return getLevel() >= info.getUnlockLevel();
	}

	public Map<String, Integer> mapSkillLevels() {
		return new HashMap<>(skills);
	}

	public void giveClassPoints(int value) {
		setClassPoints(classPoints + value);
	}

	public void giveSkillPoints(int value) {
		setSkillPoints(skillPoints + value);
	}

	public void giveAttributePoints(int value) {
		setAttributePoints(attributePoints + value);
	}

	// public void giveSkillReallocationPoints(int value) {
	// setSkillReallocationPoints(skillReallocationPoints + value);
	// }

	public void giveAttributeReallocationPoints(int value) {
		setAttributeReallocationPoints(attributeReallocationPoints + value);
	}

	public PlayerSkillData getSkillData() {
		return skillData;
	}

	public void setClass(PlayerClass profess) {
		this.profess = profess;

		// for (Iterator<SkillInfo> iterator = boundSkills.iterator();
		// iterator.hasNext();)
		// if (!getProfess().hasSkill(iterator.next().getSkill()))
		// iterator.remove();

		getStats().updateStats();
	}

	public boolean hasSkillBound(int slot) {
		return slot < boundSkills.size();
	}

	public SkillInfo getBoundSkill(int slot) {
		return slot >= boundSkills.size() ? null : boundSkills.get(slot);
	}

	public void setBoundSkill(int slot, SkillInfo skill) {
		if (boundSkills.size() < 6)
			boundSkills.add(skill);
		else
			boundSkills.set(slot, skill);
	}

	public void unbindSkill(int slot) {
		boundSkills.remove(slot);
	}

	public List<SkillInfo> getBoundSkills() {
		return boundSkills;
	}

	public boolean isInCombat() {
		return combat != null;
	}

	public boolean canChooseSubclass() {
		for (Subclass subclass : getProfess().getSubclasses())
			if (getLevel() >= subclass.getLevel())
				return true;
		return false;
	}

	public void updateCombat() {
		if (isInCombat())
			combat.update();
		else
			combat = new CombatRunnable(this);
	}

	public SkillResult cast(Skill skill) {
		return cast(getProfess().getSkill(skill));
	}

	public SkillResult cast(SkillInfo skill) {

		PlayerCastSkillEvent event = new PlayerCastSkillEvent(this, skill);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return new SkillResult(this, skill, CancelReason.OTHER);

		/*
		 * skill, mana and cooldown requirements are all calculated in the
		 * SkillResult instances. this cast(SkillResult) method only applies
		 * cooldown, reduces mana and send messages
		 */
		SkillResult cast = skill.getSkill().whenCast(this, skill);
		if (!cast.isSuccessful()) {
			if (!skill.getSkill().isPassive()) {
				if (cast.getCancelReason() == CancelReason.LOCKED)
					MMOCore.plugin.configuration.getSimpleMessage("not-unlocked-skill").send(player);
				
				if (cast.getCancelReason() == CancelReason.MANA)
					MMOCore.plugin.configuration.getSimpleMessage("casting.no-mana").send(player);

				if (cast.getCancelReason() == CancelReason.COOLDOWN)
					MMOCore.plugin.configuration.getSimpleMessage("casting.on-cooldown").send(player);
			}

			return cast;
		}

		if (!nocd) {
			double flatCooldownReduction = Math.max(0, Math.min(1, getStats().getStat(StatType.COOLDOWN_REDUCTION) / 100));
			flatCooldownReduction *= flatCooldownReduction > 0 ? skill.getModifier("cooldown", getSkillLevel(skill.getSkill())) * 1000 : 0;

			skillData.setLastCast(cast.getSkill(), System.currentTimeMillis() - (long) flatCooldownReduction);
			giveMana(-cast.getManaCost());
		}

		return cast;
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof PlayerData && ((PlayerData) obj).getUniqueId().equals(getUniqueId());
	}
}
