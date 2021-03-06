package com.hm.achievement.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.hm.achievement.category.CommandAchievements;
import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.category.NormalAchievements;
import com.hm.achievement.exception.PluginLoadError;
import com.hm.achievement.lifecycle.Reloadable;
import com.hm.achievement.utils.MaterialHelper;
import com.hm.achievement.utils.StringHelper;

/**
 * Class providing all the items displayed in the GUIs.
 * 
 * @author Pyves
 */
@Singleton
public class GUIItems implements Reloadable {

	private final Map<OrderedCategory, ItemStack> orderedAchievementItems = new TreeMap<>();

	// Various other item stacks displayed in the GUI.
	private ItemStack previousButton;
	private ItemStack nextButton;
	private ItemStack backButton;
	private ItemStack achievementNotStartedDefault;
	private final Map<String, ItemStack> achievementNotStarted = new HashMap<>();
	private ItemStack achievementStartedDefault;
	private final Map<String, ItemStack> achievementStarted = new HashMap<>();
	private ItemStack achievementReceivedDefault;
	private final Map<String, ItemStack> achievementReceived = new HashMap<>();
	private ItemStack achievementLock;
	private ItemStack categoryLock;

	private final YamlConfiguration mainConfig;
	private final YamlConfiguration langConfig;
	private final YamlConfiguration guiConfig;
	private final MaterialHelper materialHelper;
	private int serverVersion;

	private String configListAchievementFormat;
	private String configIcon;

	private String langListAchievementsInCategoryPlural;
	private String langListAchievementInCategorySingular;

	@Inject
	public GUIItems(@Named("main") YamlConfiguration mainConfig, @Named("lang") YamlConfiguration langConfig,
			@Named("gui") YamlConfiguration guiConfig, MaterialHelper materialHelper, int serverVersion) {
		this.mainConfig = mainConfig;
		this.langConfig = langConfig;
		this.guiConfig = guiConfig;
		this.materialHelper = materialHelper;
		this.serverVersion = serverVersion;
	}

	@Override
	public void extractConfigurationParameters() throws PluginLoadError {
		configListAchievementFormat = "&8" + mainConfig.getString("ListAchievementFormat");
		configIcon = StringEscapeUtils.unescapeJava(mainConfig.getString("Icon"));

		langListAchievementsInCategoryPlural = langConfig.getString("list-achievements-in-category-plural");
		langListAchievementInCategorySingular = langConfig.getString("list-achievements-in-category-singular");

		orderedAchievementItems.clear();
		// getShallowKeys returns a LinkedHashSet, preserving the ordering specified in the file.
		List<String> orderedCategories = new ArrayList<>(guiConfig.getKeys(false));
		// Prepare item stacks displayed in the GUI for Multiple achievements.
		for (MultipleAchievements category : MultipleAchievements.values()) {
			String categoryName = category.toString();
			// Sum all achievements in the sub-categories of this category.
			int totalAchievements = 0;
			for (String subcategory : mainConfig.getConfigurationSection(categoryName).getKeys(false)) {
				totalAchievements += mainConfig.getConfigurationSection(categoryName + '.' + subcategory).getKeys(false)
						.size();
			}
			ItemStack itemStack = createItemStack(categoryName);
			buildItemLore(itemStack, categoryName, totalAchievements);
			orderedAchievementItems.put(new OrderedCategory(orderedCategories.indexOf(categoryName), category), itemStack);
		}

		// Prepare item stacks displayed in the GUI for Normal achievements.
		for (NormalAchievements category : NormalAchievements.values()) {
			String categoryName = category.toString();
			ItemStack itemStack = createItemStack(categoryName);
			buildItemLore(itemStack, categoryName, mainConfig.getConfigurationSection(categoryName).getKeys(false).size());
			orderedAchievementItems.put(new OrderedCategory(orderedCategories.indexOf(categoryName), category), itemStack);
		}

		// Prepare item stack displayed in the GUI for Commands achievements.
		String categoryName = CommandAchievements.COMMANDS.toString();
		ItemStack itemStack = createItemStack(categoryName);
		buildItemLore(itemStack, categoryName, mainConfig.getConfigurationSection(categoryName).getKeys(false).size());
		orderedAchievementItems.put(new OrderedCategory(orderedCategories.indexOf(categoryName),
				CommandAchievements.COMMANDS), itemStack);

		if (serverVersion >= 13) {
			achievementNotStartedDefault = createItemStack("AchievementNotStarted", "red_terracotta", 0);
			achievementStartedDefault = createItemStack("AchievementStarted", "yellow_terracotta", 0);
			achievementReceivedDefault = createItemStack("AchievementReceived", "lime_terracotta", 0);
		} else {
			achievementNotStartedDefault = createItemStack("AchievementNotStarted", "stained_clay", 14);
			achievementStartedDefault = createItemStack("AchievementStarted", "stained_clay", 4);
			achievementReceivedDefault = createItemStack("AchievementReceived", "stained_clay", 5);
		}
		for (String type : guiConfig.getConfigurationSection("AchievementNotStarted").getKeys(false)) {
			if (!"Item".equals(type) && !"Metadata".equals(type)) {
				achievementNotStarted.put(type, createItemStack("AchievementNotStarted." + type));
			}
		}
		for (String type : guiConfig.getConfigurationSection("AchievementStarted").getKeys(false)) {
			if (!"Item".equals(type) && !"Metadata".equals(type)) {
				achievementStarted.put(type, createItemStack("AchievementStarted." + type));
			}
		}
		for (String type : guiConfig.getConfigurationSection("AchievementReceived").getKeys(false)) {
			if (!"Item".equals(type) && !"Metadata".equals(type)) {
				achievementReceived.put(type, createItemStack("AchievementReceived." + type));
			}
		}
		previousButton = createButton("PreviousButton", "list-previous-message", "list-previous-lore");
		nextButton = createButton("NextButton", "list-next-message", "list-next-lore");
		backButton = createButton("BackButton", "list-back-message", "list-back-lore");
		achievementLock = createButton("AchievementLock", "list-achievement-not-unlocked", null);
		categoryLock = createButton("CategoryLock", "list-category-not-unlocked", null);
	}

	/**
	 * Creates an ItemStack based on information extracted from gui.yml.
	 *
	 * @param categoryName
	 * @return the item for the category
	 */
	private ItemStack createItemStack(String categoryName) {
		return createItemStack(categoryName, null, 0);
	}

	/**
	 * Creates an ItemStack based on information extracted from gui.yml or default values if not found.
	 *
	 * @param categoryName
	 * @param defaultMaterial
	 * @param defaultMetadata
	 * @return the item for the category
	 */
	@SuppressWarnings("deprecation")
	private ItemStack createItemStack(String categoryName, String defaultMaterial, int defaultMetadata) {
		String path = categoryName + ".Item";
		Material material = materialHelper.matchMaterial(guiConfig.getString(path, defaultMaterial), Material.BEDROCK,
				"gui.yml (" + path + ")");
		short metadata = (short) guiConfig.getInt(categoryName + ".Metadata", defaultMetadata);
		return new ItemStack(material, 1, metadata);
	}

	/**
	 * Creates an ItemStack used as a button in the category GUI.
	 * 
	 * @param category
	 * @param msg
	 * @param lore
	 * @return the item stack
	 */
	private ItemStack createButton(String category, String msg, String lore) {
		ItemStack button = createItemStack(category);
		ItemMeta meta = button.getItemMeta();
		String displayName = ChatColor.translateAlternateColorCodes('&',
				StringEscapeUtils.unescapeJava(langConfig.getString(msg)));
		meta.setDisplayName(displayName);
		if (lore != null) {
			String loreString = ChatColor.translateAlternateColorCodes('&',
					StringEscapeUtils.unescapeJava(langConfig.getString(lore)));
			if (!loreString.isEmpty()) {
				meta.setLore(Collections.singletonList(loreString));
			}
		}
		button.setItemMeta(meta);
		return button;
	}

	/**
	 * Sets the metadata of an ItemStack representing a category in the main GUI.
	 *
	 * @param item
	 * @param categoryName
	 * @param totalAchievements
	 */
	private void buildItemLore(ItemStack item, String categoryName, int totalAchievements) {
		ItemMeta itemMeta = item.getItemMeta();
		// Some lang.yml keys differ slightly for the category name (e.g. Treasure*s* -> list-treasure).
		String langKey = StringHelper.getClosestMatch("list-" + categoryName.toLowerCase(), langConfig.getKeys(false));
		String displayName = langConfig.getString(langKey);
		// Construct title of the category item.
		if (StringUtils.isBlank(displayName)) {
			itemMeta.setDisplayName("");
		} else {
			String formattedDisplayName = StringUtils.replaceEach(configListAchievementFormat,
					new String[] { "%ICON%", "%NAME%" }, new String[] { configIcon, "&l" + displayName + "&8" });
			itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', formattedDisplayName));
		}

		// Construct lore of the category item.
		String amountMessage;
		if (totalAchievements > 1) {
			amountMessage = StringUtils.replaceOnce(langListAchievementsInCategoryPlural, "AMOUNT",
					Integer.toString(totalAchievements));
		} else {
			amountMessage = StringUtils.replaceOnce(langListAchievementInCategorySingular, "AMOUNT",
					Integer.toString(totalAchievements));
		}
		itemMeta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&8" + amountMessage)));
		item.setItemMeta(itemMeta);
	}

	public ItemStack getAchievementNotStarted(String type) {
		return achievementNotStarted.getOrDefault(type, achievementNotStartedDefault);
	}

	public ItemStack getAchievementStarted(String type) {
		return achievementStarted.getOrDefault(type, achievementStartedDefault);
	}

	public ItemStack getAchievementReceived(String type) {
		return achievementReceived.getOrDefault(type, achievementReceivedDefault);
	}

	public Map<OrderedCategory, ItemStack> getOrderedAchievementItems() {
		return orderedAchievementItems;
	}

	public ItemStack getPreviousButton() {
		return previousButton;
	}

	public ItemStack getNextButton() {
		return nextButton;
	}

	public ItemStack getBackButton() {
		return backButton;
	}

	public ItemStack getAchievementLock() {
		return achievementLock;
	}

	public ItemStack getCategoryLock() {
		return categoryLock;
	}

}
