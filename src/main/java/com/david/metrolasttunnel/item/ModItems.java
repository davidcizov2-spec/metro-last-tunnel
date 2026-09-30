package com.david.metrolasttunnel.item;

import com.david.metrolasttunnel.MetroLastTunnel;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MetroLastTunnel.MODID);

    public static final RegistryObject<Item> FILTER = ITEMS.register("filter", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> DOSIMETER = ITEMS.register("dosimeter", () -> new DosimeterItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FLASHLIGHT = ITEMS.register("flashlight", () -> new FlashlightItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<ArmorItem> GAS_MASK = ITEMS.register("gas_mask", () -> new GasMaskItem(ModArmorMaterials.FABRIC, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DOSIMETER);
            event.accept(FLASHLIGHT);
            event.accept(BATTERY);
            event.accept(FILTER);
            event.accept(GAS_MASK);
        }
    }
}
