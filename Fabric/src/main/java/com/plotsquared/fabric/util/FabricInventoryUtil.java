package com.plotsquared.fabric.util;

import com.google.inject.Singleton;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.PlotInventory;
import com.plotsquared.core.plot.PlotItemStack;
import com.plotsquared.core.util.InventoryUtil;
import com.plotsquared.fabric.player.FabricPlayer;
import com.sk89q.worldedit.fabric.FabricAdapter;
import net.kyori.adventure.text.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
public class FabricInventoryUtil extends InventoryUtil {

    @SuppressWarnings("deprecation") // Paper deprecation
    private static @Nullable ItemStack getItem(PlotItemStack item) {
        if (item == null) {
            return null;
        }
        Item material = FabricAdapter.adapt(item.getType());
        if (material == null) {
            return null;
        }
        ItemStack stack = new ItemStack(material, item.getAmount());
        CompoundTag meta = null;
        if (item.getName() != null) {
            Component nameComponent = FabricUtil.MINI_MESSAGE.deserialize(item.getName());
            stack.set(DataComponents.CUSTOM_NAME, FabricUtil.FABRIC_AUDIENCES.toNative(nameComponent));
        }
        if (item.getLore() != null) {
            List<String> lore = new ArrayList<>();
            for (String entry : item.getLore()) {
                lore.add(FabricUtil.LEGACY_COMPONENT_SERIALIZER.serialize(FabricUtil.MINI_MESSAGE.deserialize(entry)));
            }
            setLore(stack, lore);
        }
        return stack;
    }

    @SuppressWarnings("deprecation") // Paper deprecation
    @Override
    public void open(PlotInventory inv) {
        FabricPlayer bp = (FabricPlayer) inv.getPlayer();
        Inventory inventory = new Inventory(bp.getPlatformPlayer());
        SimpleContainer container = new SimpleContainer(54);
        PlotItemStack[] items = inv.getItems();
        for (int i = 0; i < inv.getLines() * 9; i++) {
            PlotItemStack item = items[i];
            if (item != null) {
                container.setItem(i, getItem(item));
            }
        }
        bp.getPlatformPlayer().openMenu(new SimpleMenuProvider((i, inventory1, player) ->
                new ChestMenu(MenuType.GENERIC_9x6, 54, inventory, container, 6),
                net.minecraft.network.chat.Component.translatable(inv.getTitle())));
    }

    @Override
    public void close(PlotInventory inv) {
        if (!inv.isOpen()) {
            return;
        }
        FabricPlayer bp = (FabricPlayer) inv.getPlayer();
        bp.getPlatformPlayer().closeContainer();
    }

    @Override
    public boolean setItemChecked(PlotInventory inv, int index, PlotItemStack item) {
        FabricPlayer bp = (FabricPlayer) inv.getPlayer();
        InventoryMenu opened = bp.getPlatformPlayer().inventoryMenu;
        ItemStack stack = getItem(item);
        if (stack == null) {
            return false;
        }
        if (!inv.isOpen()) {
            return true;
        }
        opened.setItem(opened.getSlot(index).x, opened.getSlot(index).y, stack);
        opened.broadcastChanges();
        return true;
    }

    @SuppressWarnings("deprecation") // Paper deprecation
    public PlotItemStack getItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        // int id = item.getTypeId();
        Item id = item.getItem();
        int amount = item.getCount();
        return new PlotItemStack(id.toString(), amount, item.getDisplayName().getString(), getLore(item).toArray(new String[0]));
    }

    @Override
    public PlotItemStack[] getItems(PlotPlayer<?> player) {
        FabricPlayer bp = (FabricPlayer) player;
        Inventory inv = bp.getPlatformPlayer().getInventory();
        return IntStream.range(0, 36).mapToObj(i -> getItem(inv.getItem(i)))
                .toArray(PlotItemStack[]::new);
    }

    @SuppressWarnings("deprecation") // #getTitle is needed for Spigot compatibility
    @Override
    public boolean isOpen(PlotInventory plotInventory) {
        if (!plotInventory.isOpen()) {
            return false;
        }
        //FabricPlayer bp = (FabricPlayer) plotInventory.getPlayer();
        //Inventory opened = bp.player.containerMenu;
        if (plotInventory.isOpen()) {
            return true;
            /*
            if (opened.getType() == InventoryType.CRAFTING) {
                opened.getTitle();
            }*/
        }
        return false;
    }

    public static List<String> getLore(ItemStack stack) {
        if(stack.has(DataComponents.LORE)) {
            return stack.get(DataComponents.LORE).lines().stream().map(net.minecraft.network.chat.Component::getString).collect(
                    Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static void setLore(ItemStack stack, List<String> lore) {
        stack.set(DataComponents.LORE, new ItemLore(lore.stream().map(net.minecraft.network.chat.Component::literal).collect(Collectors.toList())));
    }
}
