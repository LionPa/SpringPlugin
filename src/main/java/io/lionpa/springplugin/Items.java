package io.lionpa.springplugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class Items {
    public static final NamespacedKey ITEM_KEY = new NamespacedKey(SpringPlugin.getPlugin(),"ITEM");

    public static ItemStack SPRING;
    public static void init(){
        initSpring();
    }
    private static void initSpring(){
        SPRING = new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        ItemMeta meta = SPRING.getItemMeta();
        meta.setDisplayName("§rПружина");
        meta.addEnchant(Enchantment.MENDING,1,true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING,"spring");
        SPRING.setItemMeta(meta);
    }
}
