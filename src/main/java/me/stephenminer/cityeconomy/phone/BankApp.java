package me.stephenminer.cityeconomy.phone;

import me.stephenminer.cityeconomy.CityEconomy;
import me.stephenminer.cityeconomy.util.DataSender;
import me.stephenminer.cityeconomy.util.Receipt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DateFormat;
import java.util.*;

public class PhoneGui {
    private final CityEconomy plugin;
    private Inventory inv;
    private final Player viewer;
    private State state;

    public PhoneGui(Player viewer){
        this.plugin = JavaPlugin.getPlugin(CityEconomy.class);
        this.viewer = viewer;
        mainMenu();
    }

    public void mainMenu(){
        state = State.MAIN;
        this.inv = Bukkit.createInventory(null,27, viewer + "'s Phone");
        for (int i = 0; i < inv.getSize(); i++){
            inv.setItem(i,filler());
        }
        requestBal();
        inv.setItem(11, receiptButton());
        inv.setItem(15,transferButton());
        viewer.setMetadata("phone-inv", new FixedMetadataValue(plugin,true));
        viewer.openInventory(inv);
    }

    public void receipts(){
        state  = State.RECEIPTS;
        this.inv = Bukkit.createInventory(null, 18, "Past Transactions");
        for (int i = 0; i < inv.getSize(); i++){
            inv.setItem(i,filler());
        }
        requestReceipts();
        inv.setItem(13, backButton());
        viewer.setMetadata("phone-inv", new FixedMetadataValue(plugin,true));
        viewer.openInventory(inv);
    }

    public void requestBal(){
        DataSender sender = new DataSender();
        sender.requestBalance(viewer,viewer.getUniqueId());
    }
    public void requestReceipts(){
        DataSender sender = new DataSender();
        sender.requestReceipts(viewer);
    }

    public void updateBal(double bal){
        double toDisplay = beautifyDouble(bal);
        inv.setItem(13,balItem(toDisplay));
    }

    public void updateReceipts(Receipt[] receipts){
        Arrays.sort(receipts,Comparator.comparingLong(Receipt::timestamp));
        for (int i = 0; i < 9; i++){
            ItemStack display = receiptItem(receipts[i]);
            inv.setItem(i,display);
        }
    }




    public void processClick(InventoryClickEvent event){
        int slot = event.getSlot();
        event.setCancelled(true);
        requestBal();
        if (state  == State.MAIN) {
            switch (slot) {
                case 15 -> {
                    viewer.setMetadata("sending-money", new FixedMetadataValue(plugin, true));
                    viewer.closeInventory();
                }
                case 12 -> receipts();
                case 17 -> viewer.closeInventory();
            }
        }else if (state == State.RECEIPTS){
            if (slot == 13){
                mainMenu();
            }
        }
    }

    /**
     * @param num
     * @return double reduced to 2 decimal places
     */
    private double beautifyDouble(double num){
        int convert = (int) (num *100);
        return convert / 100d;
    }

    private ItemStack filler(){
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack backButton(){
        String name = state == State.MAIN ? "Close" : "Back";
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack receiptButton(){
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Click to View Past Transactions");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack transferButton(){
        ItemStack item = new ItemStack( Material.CHEST_MINECART);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Click to Send Money to Another Player");
        item.setItemMeta(meta);
        return item;
    }




    private ItemStack balItem(double bal ){
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwnerProfile(viewer.getPlayerProfile());
        meta.setDisplayName(ChatColor.GOLD + "Balance: " + bal);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack receiptItem(Receipt receipt){
        ItemStack item = new ItemStack( Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(receipt.amount() + " (" + timeDate(receipt.timestamp()) + ")");
        List<String> lore = new ArrayList<>();
        boolean senderMerchant = receipt.amount() < 0;
        lore.add("Reason: " + receipt.reason());
        if (senderMerchant) lore.add("Sender: " + receipt.merchant());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String timeDate(long time){
        return DateFormat.getDateInstance().format(new Date(time));
    }

    public Inventory inv(){ return inv; }

    public State getState(){ return state; }
    public void setState(State state){ this.state = state; }
    public enum State{
        MAIN,
        RECEIPTS
    }







}
