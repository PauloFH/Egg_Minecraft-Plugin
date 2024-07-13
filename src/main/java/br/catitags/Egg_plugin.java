package br.catitags;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class Egg_plugin extends JavaPlugin implements Listener {
    private Scoreboard scoreboard;
    private Objective objective;
    private File playerDataFile;
    private FileConfiguration playerData;
    private FileConfiguration config;
    private Map<UUID, Integer> playerPoints;
    private Map<String, Location> warps;
    private Map<UUID, ItemStack[]> playerInventories;
    private Map<UUID, Location> lastSurvivalLocations = new HashMap<>();
    private File lastLocationsFile;
    private FileConfiguration lastLocationsData;
    private Map<UUID, Long> lastTeleportTimes = new HashMap<>();
    private Map<UUID, BukkitTask> teleportTasks = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (teleportTasks.containsKey(playerUUID)) {
            if (event.getFrom().distanceSquared(Objects.requireNonNull(event.getTo())) > 0.1) {
                teleportTasks.get(playerUUID).cancel();
                teleportTasks.remove(playerUUID);
                player.sendMessage(ChatColor.RED + "Teleporte cancelado. Você se moveu.");
            }
        }
    }


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        assert manager != null;
        scoreboard = manager.getNewScoreboard();
        objective = scoreboard.registerNewObjective("points", "dummy", ChatColor.RED + "Pontos");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Inicializar mapas
        playerPoints = new HashMap<>();
        playerInventories = new HashMap<>();
        lastSurvivalLocations = new HashMap<>();
        lastTeleportTimes = new HashMap<>();
        teleportTasks = new HashMap<>();
        warps = new HashMap<>();
        loadConfig();
        loadPlayerPoints();
        loadLastLocations();
        loadWarps();
        Objects.requireNonNull(getCommand("lobby")).setExecutor(this);
        Objects.requireNonNull(getCommand("pvp")).setExecutor(this);
        Objects.requireNonNull(getCommand("survival")).setExecutor(this);
        Objects.requireNonNull(getCommand("loja")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpr")).setExecutor(this);

        getLogger().info("Egg_MC plugin habilitado!");
    }
    @Override
    public void onDisable() {
        savePlayerPoints();
        saveLastLocations();
        for (BukkitTask task : teleportTasks.values()) {
            task.cancel();
        }
        teleportTasks.clear();

        getLogger().info("Plugin desabilitado!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        player.setScoreboard(scoreboard);
        int points = playerPoints.getOrDefault(playerUUID, 0);
        Score score = objective.getScore(player.getName());
        score.setScore(points);
        String playerName = player.getName();
        player.sendTitle(ChatColor.RED + "Bem Vindo", ChatColor.BOLD + "Bem vindo " + ChatColor.GREEN + playerName + ChatColor.BOLD + " ao EGG MC", 10, 70, 20);
        event.setJoinMessage(ChatColor.WHITE + "Um novo Ovo " + (ChatColor.GOLD + playerName) + ChatColor.WHITE + " se juntou a nós ");
        if (warps.containsKey("lobby")) {
            playerInventories.put(playerUUID, player.getInventory().getContents());
            player.getInventory().clear();
            player.teleport(warps.get("lobby"));
            giveWarpSelector(player);
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (player.getWorld().getName().equalsIgnoreCase("world")) {
            lastSurvivalLocations.put(playerUUID, player.getLocation());
            saveLastLocations();
        }
        Score score = objective.getScore(player.getName());
        playerPoints.put(playerUUID, score.getScore());
        savePlayerPoints();

    }
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (isInSurvivalWorld((Player) event.getFrom())) {
            lastSurvivalLocations.put(playerUUID, event.getFrom().getSpawnLocation());
            saveLastLocations();
        }
    }
    private void handleSurvivalCommand(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (playerInventories.containsKey(playerUUID)) {
            player.getInventory().setContents(playerInventories.get(playerUUID));
            playerInventories.remove(playerUUID);
        }

        if (lastSurvivalLocations.containsKey(playerUUID)) {
            player.teleport(lastSurvivalLocations.get(playerUUID));
        } else {
            World survivalWorld = Bukkit.getWorld("world");
            if (survivalWorld != null) {
                player.teleport(survivalWorld.getSpawnLocation());
            } else {
                player.sendMessage(ChatColor.RED + "Mundo survival não encontrado!");
                return;
            }
        }

        player.sendMessage(ChatColor.GREEN + "Teleportado para o mundo survival!");
    }
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            UUID playerUUID = player.getUniqueId();
            Score score = objective.getScore(player.getName());
            score.setScore(score.getScore() + 1);
            playerPoints.put(playerUUID, score.getScore());
            savePlayerPoints();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.NETHER_STAR) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getDisplayName().equals(ChatColor.GREEN + "Seletor de Warp")) {
                openWarpMenu(player);
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Menu de Warps")) {
            event.setCancelled(true);

            if (clickedItem != null && clickedItem.hasItemMeta() && Objects.requireNonNull(clickedItem.getItemMeta()).hasDisplayName()) {
                String warpName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).toLowerCase();
                player.closeInventory();

                switch (warpName) {
                    case "lobby":
                    case "pvp":
                    case "loja":
                        warpPlayer(player, warpName);
                        break;
                    case "survival":
                        handleSurvivalCommand(player);
                        break;
                    default:
                        player.sendMessage(ChatColor.RED + "Warp inválido!");
                }
            }
        }
    }
    private void openWarpMenu(Player player) {
        Inventory warpMenu = Bukkit.createInventory(null, 9, ChatColor.DARK_PURPLE + "Menu de Warps");

        ItemStack lobbyItem = createWarpItem(Material.BEACON, "Lobby", ChatColor.AQUA);
        ItemStack pvpItem = createWarpItem(Material.IRON_SWORD, "PvP", ChatColor.RED);
        ItemStack survivalItem = createWarpItem(Material.GRASS_BLOCK, "Survival", ChatColor.GREEN);
        ItemStack lojaItem = createWarpItem(Material.GOLD_INGOT, "Loja", ChatColor.YELLOW);

        warpMenu.setItem(1, lobbyItem);
        warpMenu.setItem(3, pvpItem);
        warpMenu.setItem(5, survivalItem);
        warpMenu.setItem(7, lojaItem);

        player.openInventory(warpMenu);
    }

    private ItemStack createWarpItem(Material material, String name, ChatColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void giveWarpSelector(Player player) {
        ItemStack warpSelector = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = warpSelector.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Seletor de Warp");
            List<String> lore = Arrays.asList(
                    ChatColor.GRAY + "Clique com o botão direito",
                    ChatColor.GRAY + "para abrir o menu de warps"
            );
            meta.setLore(lore);
            warpSelector.setItemMeta(meta);
        }
        player.getInventory().setItem(4, warpSelector);
    }

    private void loadPlayerPoints() {
        for (String key : playerData.getKeys(false)) {
            UUID playerUUID = UUID.fromString(key);
            int points = playerData.getInt(key);
            playerPoints.put(playerUUID, points);
        }
    }

    private void savePlayerPoints() {
        for (Map.Entry<UUID, Integer> entry : playerPoints.entrySet()) {
            playerData.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        saveDefaultConfig();
        config = getConfig();
    }

    private void loadWarps() {
        warps = new HashMap<>();
        for (String warpName : Objects.requireNonNull(config.getConfigurationSection("warps")).getKeys(false)) {
            String path = "warps." + warpName;
            World world = Bukkit.getWorld(Objects.requireNonNull(config.getString(path + ".world")));
            if (world == null) {
                getLogger().warning("Mundo não encontrado para o warp: " + warpName);
                continue;
            }
            Location loc = new Location(
                    world,
                    config.getDouble(path + ".x"),
                    config.getDouble(path + ".y"),
                    config.getDouble(path + ".z"),
                    (float) config.getDouble(path + ".yaw"),
                    (float) config.getDouble(path + ".pitch")
            );
            warps.put(warpName, loc);
        }
    }
    private void warpPlayer(Player player, String warpName) {
        UUID playerUUID = player.getUniqueId();

        if (warps.containsKey(warpName)) {
            Location warpLocation = warps.get(warpName);
            if (warpName.equalsIgnoreCase("survival")) {
                if (playerInventories.containsKey(playerUUID)) {
                    player.getInventory().setContents(playerInventories.get(playerUUID));
                    playerInventories.remove(playerUUID);
                }
            }
            else if (player.getWorld().getName().equalsIgnoreCase("world")) {
                playerInventories.put(playerUUID, player.getInventory().getContents());
                player.getInventory().clear();
            }
            player.teleport(warpLocation);
            if (warpName.equalsIgnoreCase("lobby") || warpName.equalsIgnoreCase("pvp") || warpName.equalsIgnoreCase("loja")) {
                giveWarpSelector(player);
            }

            player.sendMessage(ChatColor.GREEN + "Teleportado para " + warpName + "!");
        } else {
            player.sendMessage(ChatColor.RED + "Warp não encontrado!");
        }
    }
    private void teleportToRandomLocation(Player player) {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Mundo survival não encontrado!");
            return;
        }

        int borderSize = 5000;
        int chunkSize = 16; //
        int maxAttempts = 20;

        for (int i = 0; i < maxAttempts; i++) {
            int x = (int) (Math.random() * borderSize * 2) - borderSize;
            int z = (int) (Math.random() * borderSize * 2) - borderSize;
            x = (x / chunkSize) * chunkSize + chunkSize / 2;
            z = (z / chunkSize) * chunkSize + chunkSize / 2;
            if (!world.isChunkGenerated(x / chunkSize, z / chunkSize)) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z);
            Location location = new Location(world, x, y + 1, z);

            if (isSafeLocation(location)) {
                player.teleport(location);
                player.sendMessage(ChatColor.GREEN + "Teleportado para uma localização aleatória no mundo survival!");
                lastSurvivalLocations.put(player.getUniqueId(), location);
                saveLastLocations();

                return;
            }
        }

        player.sendMessage(ChatColor.RED + "Não foi possível encontrar uma localização segura após " + maxAttempts + " tentativas.");
    }
    private boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);

        return !feet.getType().isSolid() && !head.getType().isSolid() && ground.getType().isSolid();
    }
    private void saveLastLocations() {
        for (Map.Entry<UUID, Location> entry : lastSurvivalLocations.entrySet()) {
            String path = entry.getKey().toString();
            Location loc = entry.getValue();
            lastLocationsData.set(path + ".world", Objects.requireNonNull(loc.getWorld()).getName());
            lastLocationsData.set(path + ".x", loc.getX());
            lastLocationsData.set(path + ".y", loc.getY());
            lastLocationsData.set(path + ".z", loc.getZ());
            lastLocationsData.set(path + ".yaw", loc.getYaw());
            lastLocationsData.set(path + ".pitch", loc.getPitch());
        }
        try {
            lastLocationsData.save(lastLocationsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLastLocations() {
        for (String key : lastLocationsData.getKeys(false)) {
            UUID playerUUID = UUID.fromString(key);
            World world = Bukkit.getWorld(Objects.requireNonNull(lastLocationsData.getString(key + ".world")));
            double x = lastLocationsData.getDouble(key + ".x");
            double y = lastLocationsData.getDouble(key + ".y");
            double z = lastLocationsData.getDouble(key + ".z");
            float yaw = (float) lastLocationsData.getDouble(key + ".yaw");
            float pitch = (float) lastLocationsData.getDouble(key + ".pitch");
            lastSurvivalLocations.put(playerUUID, new Location(world, x, y, z, yaw, pitch));
        }
    }
    private boolean isInSurvivalWorld(Player player) {
        return player.getWorld().getName().equalsIgnoreCase("world");
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        switch (label.toLowerCase()) {
            case "lobby":
            case "pvp":
            case "loja":
                warpPlayer(player, label.toLowerCase());
                return true;
            case "survival":
                handleSurvivalCommand(player);
                return true;
            case "tpr":
                handleTprCommand(player);
                return true;
            default:
                return false;
        }
    }
    private void handleTprCommand(Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastTeleportTime = lastTeleportTimes.getOrDefault(playerUUID, 0L);

        if (currentTime - lastTeleportTime < 300000) {
            long remainingTime = (300000 - (currentTime - lastTeleportTime)) / 1000;
            player.sendMessage(ChatColor.RED + "Você precisa esperar mais " + remainingTime + " segundos para usar o /tpr novamente.");
            return;
        }

        if (teleportTasks.containsKey(playerUUID)) {
            player.sendMessage(ChatColor.RED + "Você já tem um teleporte pendente.");
            return;
        }

        if (!isInSurvivalWorld(player)) {
            player.sendMessage(ChatColor.RED + "Você só pode usar o /tpr no mundo survival.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Teleporte iniciado. Não se mova por 5 segundos.");
        Location initialLocation = player.getLocation();

        BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.getLocation().distanceSquared(initialLocation) < 0.1) {
                teleportToRandomLocation(player);
                lastTeleportTimes.put(playerUUID, System.currentTimeMillis());
            } else {
                player.sendMessage(ChatColor.RED + "Teleporte cancelado. Você se moveu.");
            }
            teleportTasks.remove(playerUUID);
        }, 100L);

        teleportTasks.put(playerUUID, task);
    }
}
