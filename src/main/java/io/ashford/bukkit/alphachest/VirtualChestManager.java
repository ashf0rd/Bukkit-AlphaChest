package io.ashford.bukkit.alphachest;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.ashford.bukkit.alphachest.InventoryIO;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;

public class VirtualChestManager {
	private static final String YAML_CHEST_EXTENSION = ".chest.yml";
	private static final int YAML_EXTENSION_LENGTH = YAML_CHEST_EXTENSION
			.length();

	private final File dataFolder;
	private final Logger logger;
	private final HashMap<String, Inventory> chests;

	public VirtualChestManager(File dataFolder, Logger logger) {
		this.logger = logger;
		this.dataFolder = dataFolder;

		this.chests = new HashMap<String, Inventory>();

		load();
	}

	/**
	 * Loads all existing chests from the data folder.
	 */
	private void load() {
		dataFolder.mkdirs();

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(YAML_CHEST_EXTENSION);
			}
		};
		for (File chestFile : dataFolder.listFiles(filter)) {
			String chestFileName = chestFile.getName();
			logger.log(Level.INFO, "Attempting to load chest: " + chestFileName);
			try {
				try {
					UUID playerUUID = UUID.fromString(chestFileName.substring(0, chestFileName.length() - YAML_EXTENSION_LENGTH));
					chests.put(playerUUID.toString(), InventoryIO.loadFromYaml(chestFile));
				} catch (IllegalArgumentException e) {
					logger.log(Level.INFO,
							"Attempting to convert chest to uuid: "
									+ chestFileName);
					// Assume that the filename isn't a UUID, and is therefore
					// an old player-name chest
					String playerName = chestFileName.substring(0,
							chestFileName.length() - YAML_EXTENSION_LENGTH);
					Boolean flagPlayerNotFound = true;
					for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
						// Search all the known players, load inventory, flag
						// old file for deletion
						if (player.getName().toLowerCase().equals(playerName)) {
							flagPlayerNotFound = false;
							chests.put(player.getUniqueId().toString(),
									InventoryIO.loadFromYaml(chestFile));
							File newChestFile = new File(dataFolder, player
									.getUniqueId().toString()
									+ YAML_CHEST_EXTENSION);
							InventoryIO.saveToYaml(
									getChest(player.getUniqueId().toString()),
									newChestFile);
							chestFile.delete();
							logger.log(
									Level.INFO,
									"Converted chest " + chestFileName
											+ " to uuid "
											+ player.getUniqueId());
						}
					}
					if (flagPlayerNotFound) {
						logger.log(Level.WARNING, "Loading non-UUID YAML chest: "
								+ chestFileName);
						chests.put((chestFileName.substring(0, chestFileName.length() - YAML_EXTENSION_LENGTH)), InventoryIO.loadFromYaml(chestFile));
					}
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "Couldn't load chest file: "
						+ chestFileName);
			}
		}

		logger.info("Loaded " + chests.size() + " chests");
	}

	/**
	 * Saves all existing chests to the data folder.
	 * 
	 * @return the number of successfully written chests
	 */
	public int save() {
		int savedChests = 0;

		dataFolder.mkdirs();

		Iterator<Entry<String, Inventory>> chestIterator = chests.entrySet()
				.iterator();
		while (chestIterator.hasNext()) {
			final Entry<String, Inventory> entry = chestIterator.next();
			final String targetString = entry.getKey();
			final Inventory chest = entry.getValue();

			final File chestFile = new File(dataFolder, targetString
					+ YAML_CHEST_EXTENSION);
			if (chest == null) {
				// Chest got removed, so we have to delete the file.
				chestFile.delete();
				chestIterator.remove();

			} else {
				try {
					// Write the chest file in YAML format
					InventoryIO.saveToYaml(chest, chestFile);

					savedChests++;
				} catch (IOException e) {
					logger.log(Level.WARNING, "Couldn't save chest file: "
							+ chestFile.getName(), e);
				}
			}
		}

		return savedChests;
	}

	/**
	 * Gets a player's virtual chest.
	 */
	public Inventory getChest(String input) {	
		String targetChest = checkForUUID(input);
		Inventory chest = chests.get(targetChest);
		
		if (chest == null) {
			chest = Bukkit.getServer().createInventory(null, 6 * 9);
			chests.put(targetChest, chest);
		}
		
		return chest;
	}
	
	public String checkForUUID(String input) {
		String output;
		
		for (OfflinePlayer targetPlayer : Bukkit.getOfflinePlayers()) {
			if (targetPlayer.getName().equalsIgnoreCase(input)) {
				output = targetPlayer.getUniqueId().toString();
				return output;
			}
		}
		return input;
	}
	
	/**
	 * Clears a player's virtual chest.
	 */
	public void removeChest(String input) {
		// Put a null to the map so we remember to delete the file when saving!
		String target = checkForUUID(input);
		chests.put(target, null);
	}

	/**
	 * Gets the number of virtual chests.
	 * 
	 * @return the number of virtual chests
	 */
	public int getChestCount() {
		return chests.size();
	}
}
