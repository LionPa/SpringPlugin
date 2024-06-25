package io.lionpa.springplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public class Recipes {
    private static final NamespacedKey SPRING_RECIPE_KEY = new NamespacedKey(SpringPlugin.getPlugin(),"spring_recipe");

    public static void init(){
        ShapedRecipe springRecipe = new ShapedRecipe(SPRING_RECIPE_KEY, Items.SPRING);
        springRecipe.shape(
                "p",
                "p",
                "p"
        );
        springRecipe.setIngredient('p',new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE));

        Bukkit.addRecipe(springRecipe);
    }
}
