package com.kotakotik.lolfireaspectdofire;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.FireAspectEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.play.server.SAnimateBlockBreakPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("lolfireaspectdofire")
public class FireAspectDoFire {

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public FireAspectDoFire() {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static boolean isSword(ItemStack stack) {
        return stack.getItem() instanceof SwordItem;
    }

    public static boolean hasFireAspect(ItemStack stack) {
        return EnchantmentHelper.getEnchantments(stack).keySet().stream().anyMatch(enchantment -> enchantment instanceof FireAspectEnchantment);
    }

    public static boolean allChecks(ItemStack stack) {
        return isSword(stack) && hasFireAspect(stack);
    }

    static Random random = new Random();

    public static void igniteBlock(BlockPos pos, Direction face, World world, PlayerEntity plr, ItemStack stack) {
        BlockPos blockpos1 = pos.offset(face);
        if (AbstractFireBlock.canLightBlock(world, blockpos1, plr.getHorizontalFacing())) {
            world.playSound(plr, blockpos1, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, random.nextFloat() * 0.4F + 0.8F);
            BlockState blockstate1 = AbstractFireBlock.getFireForPlacement(world, blockpos1);
            world.setBlockState(blockpos1, blockstate1, 11);
            if (plr instanceof ServerPlayerEntity) {
                CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity)plr, blockpos1, stack);
                stack.damageItem(1, plr, (p_219998_1_) -> {
                    p_219998_1_.sendBreakAnimation(stack.getEquipmentSlot());
                });
            }
        }
    }

    public static void igniteBlock(PlayerInteractEvent.LeftClickBlock event) {
        igniteBlock(event.getPos(), event.getFace(), event.getWorld(), event.getPlayer(), event.getItemStack());
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid="lolfireaspectdofire")
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlockLeftClicked(PlayerInteractEvent.LeftClickBlock event) {
            ItemStack stack = event.getItemStack();
            if(allChecks(stack)) {
                igniteBlock(event);
            }
        }

        @SubscribeEvent
        public static void onEntityAttacked(AttackEntityEvent event) {
            // players cant attack with offhand so its safe to assume its the main hand
            if (event.getTarget() instanceof CreeperEntity && allChecks((event.getPlayer()).getHeldItemMainhand())) {
                ((CreeperEntity) event.getTarget()).ignite();
            }
        }
    }
}
