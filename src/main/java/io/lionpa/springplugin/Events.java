package io.lionpa.springplugin;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Events implements Listener {
    private static final int MODEL_PARTS = 5;
    @EventHandler
    public static void interact(PlayerInteractEvent e){
        Player player = e.getPlayer();
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getItem();

        if (item == null) return;
        if (item.getType() == Material.AIR) return;
        if (item.getItemMeta() == null) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(Items.ITEM_KEY)) return;

        if (item.getItemMeta().getPersistentDataContainer().get(Items.ITEM_KEY, PersistentDataType.STRING).equals("spring")) {
            e.setCancelled(true);
            if (e.getBlockFace() != BlockFace.UP) return;

            Location location = e.getClickedBlock().getRelative(BlockFace.UP).getLocation().toCenterLocation();

            if (tooManySprings(location.getChunk())){
                player.sendMessage("Слишком много пружин в одном чанке!");
                return;
            }

            if (player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SURVIVAL) {
                item.setAmount(item.getAmount() - 1);
            }

            spawnSpring(location);
        }
    }
    private static boolean tooManySprings(Chunk chunk){
        int springs = 0;
        for (Entity entity : chunk.getEntities()){
            if (entity.getPersistentDataContainer().has(SUBS_KEY)) springs++;
        }
        return springs >= 15;
    }

    private static void spawnSpring(Location location){
        ItemDisplay[] model = new ItemDisplay[MODEL_PARTS];

        for (int i = 0; i < model.length; i++){
            ItemDisplay part = location.getWorld().spawn(location,ItemDisplay.class);
            part.setItemStack(new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE));

            location.add(0,0.1f,0);

            model[i] = part;
        }

        location.add(0,model.length*-0.1f,0);

        spawnMainEntity(location,model);
    }
    private static final NamespacedKey SUBS_KEY = new NamespacedKey(SpringPlugin.getPlugin(),"subs");
    private static Interaction spawnMainEntity(Location location, Entity... sub){
        Interaction interaction = location.getWorld().spawn(location.clone().add(0,-0.5f,0), Interaction.class);
        interaction.setInteractionHeight(0.5f);
        interaction.setInteractionWidth(1);

        List<String> subsId = new ArrayList<>();
        for (Entity entity : sub) {
            subsId.add(String.valueOf(entity.getUniqueId()));
        }

        interaction.getPersistentDataContainer().set(SUBS_KEY,PersistentDataType.LIST.strings(),subsId);

        return interaction;

    }
    @EventHandler
    public static void move(EntityMoveEvent e){
        move(e.getTo(),e.getEntity());
    }
    @EventHandler
    public static void playerMove(PlayerMoveEvent e){
        move(e.getTo(),e.getPlayer());
    }
    private static void move(Location location, Entity entity){
        for (Interaction interaction : location.getNearbyEntitiesByType(Interaction.class,0.25f,0.49f,0.25f)){
            if (!interaction.getPersistentDataContainer().has(SUBS_KEY)) continue;

            playAnimation(interaction);

            new BukkitRunnable() {
                @Override
                public void run() {
                    entity.setFallDistance(-10);
                    Vector velocity = entity.getVelocity().add(entity.getLocation().getDirection().multiply(0.02f)).setY(1.15f);
                    entity.setVelocity(velocity);
                    jumpSound(interaction.getLocation());
                }
            }.runTaskLater(SpringPlugin.getPlugin(),2);
            break;
        }
    }

    private static final int ANIMATION_DURATION = 10;

    private static void playAnimation(Interaction main){
        ItemDisplay[] model = getModel(main);

        final int[] tick = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tick[0] >= ANIMATION_DURATION){
                    cancel();
                }
                frame(model,tick[0]);
                tick[0]++;
            }
        }.runTaskTimer(SpringPlugin.getPlugin(),0,1);
    }
    private static void frame(ItemDisplay[] model, int tick){
        for (int i = 0; i < MODEL_PARTS; i++){

            ItemDisplay part = model[i];
            Transformation t = part.getTransformation();

            if (tick <= ANIMATION_DURATION/2f) {
                t.getTranslation().set(0, lerp(0,0.2f, (float) tick / ANIMATION_DURATION * 2f)*i, 0);
            } else {
                t.getTranslation().set(0, lerp(0.2f,0, (tick - ANIMATION_DURATION/2f) / ANIMATION_DURATION * 2f)*i, 0);
            }
            part.setInterpolationDuration(2);
            part.setInterpolationDelay(0);
            part.setTransformation(t);
        }
    }
    private static float lerp(float a, float b, float f){
        return a + f * (b - a);
    }
    @EventHandler
    public static void damage(EntityDamageEvent e){
        if (e.getEntity() instanceof Player player){
            if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;

            for (Interaction interaction : player.getLocation().getNearbyEntitiesByType(Interaction.class,0.25f,1f,0.25f)) {
                if (!interaction.getPersistentDataContainer().has(SUBS_KEY)) continue;
                e.setCancelled(true);
            }
        }
    }
    @EventHandler
    public static void breakSpring(EntityDamageByEntityEvent e){
        if (e.getDamager() instanceof Player){
            if (e.getEntity().getType() != EntityType.INTERACTION) return;
            if (!e.getEntity().getPersistentDataContainer().has(SUBS_KEY)) return;
            ItemDisplay[] model = getModel((Interaction) e.getEntity());
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (ItemDisplay part : model){
                        part.remove();
                    }
                    e.getDamager().getWorld().dropItem(e.getEntity().getLocation(), Items.SPRING);
                    e.getEntity().remove();
                }
            }.runTaskLater(SpringPlugin.getPlugin(),1);

        }
    }
    private static void jumpSound(Location location){
        final float[] pitch = {1.5f};
        World world = location.getWorld();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                world.playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT,0.5f,pitch[0]);
                world.playSound(location, Sound.BLOCK_NOTE_BLOCK_GUITAR,0.25f,pitch[0]);
                pitch[0] += 0.15f;
            }
        }.runTaskTimer(SpringPlugin.getPlugin(),0,1);
        new BukkitRunnable() {
            @Override
            public void run() {
                task.cancel();
            }
        }.runTaskLater(SpringPlugin.getPlugin(),3);
    }

    private static ItemDisplay[] getModel(Interaction main){
        ItemDisplay[] model = new ItemDisplay[MODEL_PARTS];

        List<String> modelPartIds = main.getPersistentDataContainer().get(SUBS_KEY,PersistentDataType.LIST.strings());
        for (int i = 0; i < MODEL_PARTS; i++){
            String uuid = modelPartIds.get(i);
            ItemDisplay part = null;
            for (ItemDisplay display : main.getLocation().getNearbyEntitiesByType(ItemDisplay.class,1f,1f,1f)){
                if (!String.valueOf(display.getUniqueId()).equals(uuid)) continue;
                part = display;
            }
            model[i] = part;
        }
        return model;
    }
}
