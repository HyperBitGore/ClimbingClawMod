package com.hyperbitgore.climbingclaw;


import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClimbingClawItem extends Item {
	
	 private static final Logger LOGGER = LogUtils.getLogger();
	 private boolean attached;
	 private Vec3 move_dir;
	 
	public ClimbingClawItem(Properties p_41383_) {
		super(p_41383_);
		attached = false;
	}
	
	
	@Override
	public boolean canAttackBlock(BlockState p_41441_, Level p_41442_, BlockPos p_41443_, Player p_41444_) {
	      return false;
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
	    //player.sendSystemMessage(Component.translatable("Climbing claw used"));
	    //attached = !attached;
	    
		return super.use(level, player, hand); 
	   }
	
	
	
	@Override
	public InteractionResult useOn(UseOnContext pr) {
		 Player p = pr.getPlayer();
		 BlockPos pos = pr.getClickedPos();
		 ItemStack item = p.getItemInHand(pr.getHand());
		 Block b = pr.getLevel().getBlockState(pos).getBlock();
		 
		 if(withinRange(b, p)) {
			 
			 //item.setDamageValue(item.getDamageValue() - 1);
			 //p.sendSystemMessage(Component.translatable("Climbing claw used " + item.getDamageValue()));
		 }
		 
	     return super.useOn(pr);
	 }
	
	
	private boolean withinRange(Block block, Player p) {
		return true;
	}
	
	@Override
	public boolean onLeftClickEntity(ItemStack item, Player player, Entity entity) {
		return super.onLeftClickEntity(item, player, entity);
	}
}
