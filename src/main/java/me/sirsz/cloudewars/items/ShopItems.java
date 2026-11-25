package me.sirsz.cloudewars.items;

import com.l299l.newbedwars.api.NewBedwarsAPI;
import com.l299l.newbedwars.api.shop.ItemTexts;
import com.l299l.newbedwars.api.shop.ShopCategory;
import com.l299l.newbedwars.api.shop.ShopEditor;
import com.l299l.newbedwars.api.shop.ShopGui;
import com.l299l.newbedwars.api.shop.ShopItem;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import me.sirsz.cloudewars.CloudeWars;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cloude Wars' shop items: adds the snow golem egg to New Bedwars' items.yml (New Bedwars 2.7-beta and newer), and
 * puts it and the Wood item into the shop GUIs once (the egg after the silverfish, the wood after the wool). What a
 * server owner changes later stays: a GUI that already got an item is remembered in shop.yml and never touched again.
 */
public final class ShopItems {

    public static final String SNOW_GOLEM = "SnowGolem";
    private static final String WOOD = "Wood";
    private static final String SAVE_FILE = "shop.yml";
    /** item, the item it goes after in the GUIs */
    private static final List<String[]> PLACES = List.of(new String[] {SNOW_GOLEM, "Silverfish"}, new String[] {WOOD, "Wool"});

    private final JavaPlugin plugin;
    private final boolean addToGuis;
    private final File saveFile;

    public ShopItems(JavaPlugin plugin, YamlConfiguration config) {
        this.plugin = plugin;
        this.addToGuis = config.getBoolean("shop.add-items");
        this.saveFile = new File(plugin.getDataFolder(), SAVE_FILE);
    }

    public void register(NewBedwarsAPI api) {
        ShopItem egg = ShopItem.of(SNOW_GOLEM, "SNOW_GOLEM_SPAWN_EGG", 1, 5, "GOLD", CloudeWars.SNOW_GOLEM_LOGIC);
        ItemTexts english = new ItemTexts("&fSnow Golem", "&fSummons a fast snow golem that; &fpelts enemies with snowballs.;"
                + " &7Comes back to you if it falls; &7into the void.; ; &7Cost: &e/price/", "");
        if (api.registerShopItem(plugin, egg, Map.of("en", english))) {
            plugin.getLogger().info("Added the snow golem egg to New Bedwars' data/items.yml (5 gold).");
        }
        if (addToGuis) {
            addToGuis(api.getShop());
        }
    }

    private void addToGuis(ShopEditor shop) {
        shop.getItems().thenCompose(items -> {
            Set<String> existing = new HashSet<>();
            items.forEach(item -> existing.add(item.name().toLowerCase(Locale.ROOT)));
            return shop.getGuis().thenAccept(guis -> {
                for (ShopGui gui : guis) {
                    addToGui(shop, gui, existing);
                }
            });
        }).exceptionally(error -> {
            plugin.getLogger().warning("Could not put Cloude Wars' items into the shop: " + error);
            return null;
        });
    }

    private void addToGui(ShopEditor shop, ShopGui gui, Set<String> existingItems) {
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(saveFile);
        List<String> done = new ArrayList<>(saved.getStringList("added"));
        List<ShopCategory> categories = new ArrayList<>(gui.categories());
        List<String> added = new ArrayList<>();
        List<String> alreadyThere = new ArrayList<>();
        for (String[] place : PLACES) {
            String item = place[0];
            String key = gui.id() + ":" + item;
            if (done.contains(key) || !existingItems.contains(item.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (has(categories, item)) {
                alreadyThere.add(key); // remembered too, so removing it later is kept
                continue;
            }
            for (int i = 0; i < categories.size(); i++) {
                ShopCategory category = categories.get(i);
                List<String> entries = new ArrayList<>(category.items());
                int anchor = indexOf(entries, place[1]);
                if (anchor >= 0) {
                    // Written like the entry before it: "ci:Silverfish" -> "ci:SnowGolem", "silverfish" -> "snowgolem"
                    String before = entries.get(anchor);
                    entries.add(anchor + 1, before.startsWith("ci:") ? "ci:" + item
                            : before.equals(before.toLowerCase(Locale.ROOT)) ? item.toLowerCase(Locale.ROOT) : item);
                    categories.set(i, new ShopCategory(category.id(), category.name(), category.icon(), category.description(), entries));
                    added.add(key);
                    break;
                }
            }
        }
        if (added.isEmpty()) {
            if (!alreadyThere.isEmpty()) {
                remember(alreadyThere);
            }
            return;
        }
        // The 13-value ShopGui exists in 2.7-beta and 2.8-beta; null price overrides keep the GUI's own
        ShopGui changed = new ShopGui(gui.id(), gui.title(), gui.size(), gui.headerSize(), gui.footerSize(), false, gui.fill(),
                gui.empty(), gui.slots(), categories, gui.problems(), gui.usedBy(), null);
        shop.saveGui(changed).thenAccept(result -> {
            if (!result.isSuccess()) {
                plugin.getLogger().warning("Could not put Cloude Wars' items into shop GUI " + gui.id() + ": " + result.code()
                        + (result.detail() == null ? "" : " (" + result.detail() + ")"));
                return;
            }
            plugin.getLogger().info("Put " + added.stream().map(key -> key.substring(key.indexOf(':') + 1)).toList()
                    + " into shop GUI " + gui.id() + ".");
            added.addAll(alreadyThere);
            remember(added);
        });
    }

    private void remember(List<String> added) {
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(saveFile);
        List<String> done = new ArrayList<>(saved.getStringList("added"));
        done.addAll(added);
        saved.options().setHeader(List.of("Shop GUIs Cloude Wars already put its items into (gui:item); they are not",
                "touched again, so items you move or remove stay that way."));
        saved.set("added", done);
        try {
            saved.save(saveFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + SAVE_FILE + ": " + e.getMessage());
        }
    }

    private static boolean has(List<ShopCategory> categories, String item) {
        return categories.stream().anyMatch(category -> indexOf(category.items(), item) >= 0);
    }

    /** Where an item is in a category's list ("Silverfish", "silverfish" or "ci:Silverfish"), or -1. */
    private static int indexOf(List<String> entries, String item) {
        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            if ((entry.startsWith("ci:") ? entry.substring(3) : entry).equalsIgnoreCase(item)) {
                return i;
            }
        }
        return -1;
    }
}
