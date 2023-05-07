package com.hyperbitgore.climbingclaw;

import java.util.ArrayList;

import org.apache.commons.compress.compressors.lz77support.LZ77Compressor.Block;
import org.slf4j.Logger;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.glyphs.BakedGlyph.Effect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ScreenEvent.KeyPressed;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

//needs to match a value in a mod.toml in META-INF
@Mod(ClimbingClaw.modid)
public class ClimbingClaw {

	
	private int last_input = -1;
	//booleans for climbing
	private boolean attached;
	private boolean vertical = false;
	private boolean top = false;
	private boolean x_dir = false;
	//boolean if it's first attach
	private boolean first = true;
	//boolean to tell if you can move vertically
	private boolean vert_possible = false;
	
	private BlockPos last_pos = null;
	//current position
	private double x_pos = 0;
	private double y_pos = 0;
	private double z_pos = 0;
	
	// Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
	
	
	public static final String modid = "climbingclawmod";
	
	//item register
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ClimbingClaw.modid);
	
	//registering climbing claw item
	public static final RegistryObject<Item> CLIMBING_CLAW = ITEMS.register("climbingclaw", () -> new ClimbingClawItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1).durability(200))); 
	
	
	//test mod on server
	//make movement more responsive and less janky
	
	public ClimbingClaw() {
			IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

	        // Register the commonSetup method for modloading
	        modEventBus.addListener(this::commonSetup);

	        // Register the Deferred Register to the mod event bus so items get registered
	        ITEMS.register(modEventBus);

	        // Register ourselves for server and other game events we are interested in
	        MinecraftForge.EVENT_BUS.register(this);
	        
	        attached = false;
	}
	
	
	private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        
    }
	
	@SubscribeEvent
	public void keyPressed(InputEvent.Key event) {
		last_input = event.getKey();
	}
	
	
	private boolean checkAboveRange(Player p) {
		for(double y = p.getEyeY(); y <= p.getEyeY() + 1; y += 0.1) {
			if(p.getLevel().getBlockState(new BlockPos(p.getX(), y, p.getZ())).getMaterial().isSolidBlocking()) {
				return true;
			}
		}
		return false;
	}
	private boolean checkBelowRange(Player p) {
		for(double y = p.getY(); y >= p.getY() - 1; y -= 0.1) {
			if(p.getLevel().getBlockState(new BlockPos(p.getX(), y, p.getZ())).getMaterial().isSolidBlocking()) {
				return true;
			}
		}
		return false;
	}
	
	private boolean checkXRangePos(Player p) {
		for(double x = p.getX(); x <= p.getX() + 1; x += 0.1) {
			if(p.getLevel().getBlockState(new BlockPos(x, p.getEyeY(), p.getZ())).getMaterial().isSolidBlocking()) {
				return true;
			}
		}
		return false;
	}
	private boolean checkXRangeNeg(Player p) {
		for(double x = p.getX(); x >= p.getX() - 1; x -= 0.1) {
			if(p.getLevel().getBlockState(new BlockPos(x, p.getEyeY(), p.getZ())).getMaterial().isSolidBlocking()) {
				return true;
			}
		}
		return false;
	}
	
	
	private boolean checkZRangePos(Player p) {
		for(double z = p.getZ(); z <= p.getZ() + 1; z += 0.1) {
			if(p.getLevel().getBlockState(new BlockPos(p.getX(), p.getEyeY(), z)).getMaterial().isSolidBlocking()) {
				return true;
			}
		}
		return false;
	}
	private boolean checkZRangeNeg(Player p) {
		for(double z = p.getZ(); z >= p.getZ() - 1; z -= 0.1) {
			if(p.getLevel().getBlockState(new BlockPos(p.getX(), p.getEyeY(), z)).getMaterial().isSolidBlocking()) {
				return true;
			}
		}
		return false;
	}
	
	@SubscribeEvent
	public void onTick(TickEvent.PlayerTickEvent event) {
		
		Player p = event.player;
		switch(last_input) {
		case InputConstants.KEY_SPACE:
			attached = false;
			p.resetFallDistance();
			first = true;
			break;
		case InputConstants.KEY_W:
			if(!vertical || !top) {
				BlockPos b = new BlockPos(x_pos, y_pos + 1.8 + 0.01, z_pos);
				if(p.getLevel().getBlockState(b).isAir() && vert_possible) {
					y_pos += 0.01;
				}
			}
			break;
		case InputConstants.KEY_S:
			if(!vertical || !top) {
				BlockPos b = new BlockPos(x_pos, y_pos - 0.01, z_pos);
				if(p.getLevel().getBlockState(b).isAir() && vert_possible) {
					y_pos -= 0.01;
				}	
			}
			break;
		case InputConstants.KEY_A:
			
			break;
		case InputConstants.KEY_D:
			
			break;
		}
		last_input = -1;
		if(last_pos != null) {
			if(attached) {
				if(vertical) {
					if(checkAboveRange(p)) {
						p.setPos(p.getX(), y_pos, p.getZ());
					}else {
						p.setPos(x_pos, y_pos, z_pos);
					}
					x_pos = p.getX();
					z_pos = p.getZ();
					
				}else if(top) {
					//check below
					if(checkBelowRange(p)) {
						p.setPos(p.getX(), y_pos, p.getZ());
					}else {
						p.setPos(x_pos, y_pos, z_pos);
					}
					p.setPose(Pose.SWIMMING);
					x_pos = p.getX();
					z_pos = p.getZ();
				} 
				else {
					if(x_dir) {
						//check z dir
						if(checkZRangePos(p) || checkZRangeNeg(p)) {
							vert_possible = true;
							p.setPos(p.getX(), y_pos, z_pos);
						}else {
							vert_possible = false;
							p.setPos(x_pos, y_pos,  z_pos);
						}
						
						x_pos = p.getX();
					}else {
						//check x dir
						if(checkXRangePos(p) || checkXRangeNeg(p)) {
							vert_possible = true;
							p.setPos(x_pos, y_pos,  p.getZ());
						}else {
							vert_possible = false;
							p.setPos(x_pos, y_pos,  z_pos);
						}
						z_pos = p.getZ();
					}
				}
			}
		}
	}
	
	//https://forums.minecraftforge.net/topic/88603-solved-how-to-see-which-face-of-a-block-the-player-has-collided-with/
	//attach to wall and stop player from moving by taking event in for movement, as well let player move block he is attached to 
	@SubscribeEvent
	public void rightClick(PlayerInteractEvent.RightClickBlock event) {
		if(event.getItemStack().getItem() instanceof ClimbingClawItem) {
			if(event.getFace() != null) {
				Direction d = event.getFace();
				BlockPos bp = event.getPos();
				BlockPos f = bp;
				BlockPos above;
				Player p = event.getEntity();
				//checking if within claw range
				//if(first) {
					vertical = false;
					top = false;
					double x_off = p.getX() - bp.getX();
					double y_off = p.getY() - bp.getY();
					double z_off = p.getZ() - bp.getZ();
					boolean n_def = false;
					switch(d) {
					case UP:
						if(first) {
							if(Math.abs(x_off) > 1.2 || (Math.abs(y_off) > 1.0) || (Math.abs(z_off) > 1.2)){
								p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
								return;
							}
							
						}else {
							if(Math.abs(x_off) > 1.5 || (Math.abs(y_off) > 2.0) || (Math.abs(z_off) > 1.5)){
								p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
								return;
							}
							y_pos = f.above().getY();
							x_pos = f.above().getX() + 0.5;
							z_pos = f.above().getZ() + 0.5;
							n_def = true;
						}
						vertical = false;
						top = true;
						break;
					case DOWN:
						if(Math.abs(x_off) > 0.7 || (Math.abs(y_off) > 2.0) || (Math.abs(z_off) > 0.7)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						vertical = true;
						top = false;
						break;
					case EAST:
						if(Math.abs(x_off) > 1.31 || (Math.abs(y_off) > 1.0) || (Math.abs(z_off) > 1.0)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						x_dir = false;
						above = new BlockPos(x_pos, y_pos + 1.0, z_pos);
						if(!p.getLevel().getBlockState(above).isAir()) {
							if(p.getLevel().getBlockState(above).getMaterial().isSolidBlocking()) {
								y_pos -= 1.0;
							}
						}
						break;
					case WEST:
						if(Math.abs(x_off) > 0.31 || (Math.abs(y_off) > 1.0) || (Math.abs(z_off) > 1.0)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						x_dir = false;
						above = new BlockPos(x_pos, y_pos + 1.0, z_pos);
						if(!p.getLevel().getBlockState(above).isAir()) {
							if(p.getLevel().getBlockState(above).getMaterial().isSolidBlocking()) {
								y_pos -= 1.0;
							}
						}
						break;
					case SOUTH:
						if(Math.abs(x_off) > 1.0 || (Math.abs(y_off) > 1.0) || (Math.abs(z_off) > 1.31)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						x_dir = true;
						above = new BlockPos(x_pos, y_pos + 1.0, z_pos);
						if(!p.getLevel().getBlockState(above).isAir()) {
							if(p.getLevel().getBlockState(above).getMaterial().isSolidBlocking()) {
								y_pos -= 1.0;
							}
						}
						break;
					case NORTH:
						if(Math.abs(x_off) > 1.0 || (Math.abs(y_off) > 1.0) || (Math.abs(z_off) > 0.31)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						x_dir = true;
						above = new BlockPos(x_pos, y_pos + 1.0, z_pos);
						if(!p.getLevel().getBlockState(above).isAir()) {
							if(p.getLevel().getBlockState(above).getMaterial().isSolidBlocking()) {
								y_pos -= 1.0;
							}
						}
						break;
					}
					if(!n_def) {
						x_pos = f.getX() + x_off;
						y_pos = f.getY() + y_off;
						z_pos = f.getZ() + z_off;
					}
					first = false;
				/*}else {
					switch(d) {
					case UP:
						y_pos = f.above().getY();
						x_pos = f.above().getX() + 0.5;
						z_pos = f.above().getZ() + 0.5;
						vertical = false;
						top = true;
						break;
					case DOWN:
						vertical = true;
						top = false;
						break;
					case EAST:
						x_dir = false;
						vertical = false;
						break;
					case WEST:
						x_dir = false;
						vertical = false;
						break;
					case SOUTH:
						x_dir = true;
						vertical = false;
						break;
					case NORTH:
						x_dir = true;
						vertical = false;
						break;
					}
				}*/
				p.getItemInHand(event.getHand()).setDamageValue(p.getItemInHand(event.getHand()).getDamageValue() + 1);
				attached = true;
				if(p.getItemInHand(event.getHand()).getDamageValue() > p.getItemInHand(event.getHand()).getMaxDamage()) {
					p.broadcastBreakEvent(event.getHand());
					p.getItemInHand(event.getHand()).setCount(0);
					attached = false;
				}
				p.setPos(x_pos, y_pos, z_pos);
				last_pos = new BlockPos(x_pos, y_pos, z_pos);
			}
		}
	}
	
	// You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = modid, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
