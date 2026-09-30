package com.david.metrolasttunnel.block;

import com.david.metrolasttunnel.MetroLastTunnel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MetroLastTunnel.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MetroLastTunnel.MODID);

    public static final RegistryObject<Block> RADIOACTIVE_STONE = BLOCKS.register("radioactive_stone", () -> new RadioactiveBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.5F).sound(SoundType.STONE).lightLevel(state -> 2)));

    public static final RegistryObject<Block> METRO_CONCRETE = BLOCKS.register("metro_concrete", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 8.0F).sound(SoundType.STONE)));

    public static final RegistryObject<Block> RUSTED_METAL = BLOCKS.register("rusted_metal", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(3.0F, 8.0F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> METRO_LAMP = BLOCKS.register("metro_lamp", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.GLASS).lightLevel(state -> 15)));

    public static final RegistryObject<Item> RADIOACTIVE_STONE_ITEM = ITEMS.register("radioactive_stone", () -> new BlockItem(RADIOACTIVE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> METRO_CONCRETE_ITEM = ITEMS.register("metro_concrete", () -> new BlockItem(METRO_CONCRETE.get(), new Item.Properties()));
    public static final RegistryObject<Item> RUSTED_METAL_ITEM = ITEMS.register("rusted_metal", () -> new BlockItem(RUSTED_METAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> METRO_LAMP_ITEM = ITEMS.register("metro_lamp", () -> new BlockItem(METRO_LAMP.get(), new Item.Properties()));

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ModBlocks::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(RADIOACTIVE_STONE_ITEM);
            event.accept(METRO_CONCRETE_ITEM);
            event.accept(RUSTED_METAL_ITEM);
            event.accept(METRO_LAMP_ITEM);
        }
    }
}
