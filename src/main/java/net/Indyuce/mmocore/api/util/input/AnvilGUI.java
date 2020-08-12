package net.Indyuce.mmocore.api.util.input;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Consumer;

import net.Indyuce.mmocore.MMOCore;
import net.mmogroup.mmolib.MMOLib;

public class AnvilGUI extends PlayerInput {
	private final int containerId;
	private final Inventory inventory;

	public AnvilGUI(Player player, InputType type, Consumer<String> output) {
		super(player, output);

		ItemStack paper = new ItemStack(Material.PAPER);
		ItemMeta paperMeta = paper.getItemMeta();
		paperMeta.setDisplayName(MMOCore.plugin.configManager.getSimpleMessage("player-input.anvil." + type.getLowerCaseName()).message());
		paper.setItemMeta(paperMeta);

		MMOLib.plugin.getVersion().getWrapper().handleInventoryCloseEvent(player);
		MMOLib.plugin.getVersion().getWrapper().setActiveContainerDefault(player);

		final Object container = MMOLib.plugin.getVersion().getWrapper().newContainerAnvil(player);

		inventory = MMOLib.plugin.getVersion().getWrapper().toBukkitInventory(container);
		inventory.setItem(0, paper);

		containerId = MMOLib.plugin.getVersion().getWrapper().getNextContainerId(player);
		MMOLib.plugin.getVersion().getWrapper().sendPacketOpenWindow(player, containerId);
		MMOLib.plugin.getVersion().getWrapper().setActiveContainer(player, container);
		MMOLib.plugin.getVersion().getWrapper().setActiveContainerId(container, containerId);
		MMOLib.plugin.getVersion().getWrapper().addActiveContainerSlotListener(container, player);
	}

	public void close() {
		MMOLib.plugin.getVersion().getWrapper().handleInventoryCloseEvent(getPlayer());
		MMOLib.plugin.getVersion().getWrapper().setActiveContainerDefault(getPlayer());
		MMOLib.plugin.getVersion().getWrapper().sendPacketCloseWindow(getPlayer(), containerId);

		InventoryClickEvent.getHandlerList().unregister(this);
		InventoryCloseEvent.getHandlerList().unregister(this);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void a(InventoryClickEvent event) {
		if (event.getInventory().equals(inventory)) {
			event.setCancelled(true);

			if (event.getRawSlot() == 2) {
				ItemStack clicked = inventory.getItem(event.getRawSlot());
				if (clicked != null && clicked.getType() != Material.AIR)
					output(clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : clicked.getType().toString());
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void b(InventoryCloseEvent event) {
		if (event.getInventory().equals(inventory))
			close();
	}
}