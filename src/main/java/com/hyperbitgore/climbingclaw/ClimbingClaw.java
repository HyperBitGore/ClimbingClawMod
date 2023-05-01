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
	private boolean attached;
	private boolean vertical = false;
	private boolean top = false;
	private boolean x_dir = false;
	private boolean first = true;
	private BlockPos last_pos = null;
	//current position
	private double x_pos = 0;
	private double y_pos = 0;
	private double z_pos = 0;
	//offset from block you move
	private double x_dif = 0;
	private double y_dif = 0;
	private double z_dif = 0;
	
	
	// Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
	
	
	public static final String modid = "climbingclawmod";
	
	//item register
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ClimbingClaw.modid);
	//item properties
	
	
	//registering climbing claw item
	public static final RegistryObject<Item> CLIMBING_CLAW = ITEMS.register("climbingclaw", () -> new ClimbingClawItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1).durability(1000))); 
	
	//add collision on movement
	//add durability
	
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
	
	/*@SubscribeEvent
	public void onFall(LivingFallEvent event) {
		if(event.getEntity().getClass() == Player.class) {
			System.out.println("Player falling");
		}
	}*/
	
	@SubscribeEvent
	public void onTick(TickEvent.PlayerTickEvent event) {
		
		Player p = event.player;
		switch(last_input) {
		case InputConstants.KEY_SPACE:
			attached = false;
			first = true;
			break;
		case InputConstants.KEY_W:
			if(vertical) {
				double xc = p.getLookAngle().x() * 0.02;
				double zc = p.getLookAngle().z() * 0.02;
				BlockPos b = new BlockPos(x_pos + x_dif + xc, y_pos + y_dif + p.getEyeHeight(), z_pos + z_dif + zc);
				System.out.println("Player: " + p.getX() + ", " + p.getY() + ", " + p.getZ());
				System.out.println("Check: " + b.getX() + ", " + b.getY() + ", " + b.getZ());
				if(p.getLevel().getBlockState(b).isAir()) {
					x_dif += xc;
					z_dif += zc;
				}
			}else {
				BlockPos b = new BlockPos(x_pos + x_dif, y_pos + y_dif + 0.01, z_pos + z_dif);
				if(p.getLevel().getBlockState(b).isAir()) {
					y_dif += 0.01;
				}
			}
			break;
		case InputConstants.KEY_S:
			if(vertical) {
				double xc = p.getLookAngle().x() * 0.02;
				double zc = p.getLookAngle().z() * 0.02;
				BlockPos b = new BlockPos(x_pos + x_dif - xc, y_pos + y_dif + p.getEyeHeight(), z_pos + z_dif - zc);
				if(p.getLevel().getBlockState(b).isAir()) {
					x_dif -= xc;
					z_dif -= zc;
				}
			}else {
				BlockPos b = new BlockPos(x_pos + x_dif, y_pos + y_dif - 0.01, z_pos + z_dif);
				if(p.getLevel().getBlockState(b).isAir()) {
					y_dif -= 0.01;
				}
			}
			break;
		case InputConstants.KEY_A:
			if(x_dir) {
				BlockPos b = new BlockPos(x_pos + x_dif + 0.01, y_pos + y_dif, z_pos + z_dif);
				if(p.getLevel().getBlockState(b).isAir()) {
					x_dif += 0.01;
				}
			}else {
				BlockPos b = new BlockPos(x_pos + x_dif, y_pos + y_dif, z_pos + z_dif + 0.01);
				if(p.getLevel().getBlockState(b).isAir()) {
					z_dif += 0.01;
				}
			}
			break;
		case InputConstants.KEY_D:
			if(x_dir) {
				BlockPos b = new BlockPos(x_pos + x_dif - 0.01, y_pos + y_dif, z_pos + z_dif);
				if(p.getLevel().getBlockState(b).isAir()) {
					x_dif -= 0.01;
				}
			}else {
				BlockPos b = new BlockPos(x_pos + x_dif, y_pos + y_dif, z_pos + z_dif - 0.01);
				if(p.getLevel().getBlockState(b).isAir()) {
					z_dif -= 0.01;
				}
			}
			break;
		}
		last_input = -1;
		if(x_dif >= 0.5) {
			x_dif = 0.5;
		}else if(x_dif <= -0.5) {
			x_dif = -0.5;
		}
		if(y_dif >= 0.5) {
			y_dif = 0.5;
		}else if(y_dif <= -0.5) {
			y_dif = -0.5;
		}
		if(z_dif >= 0.5) {
			z_dif = 0.5;
		}else if(z_dif <= -0.5) {
			z_dif = -0.5;
		}
		
		if(last_pos != null) {
			if(attached) {
				if(vertical) {
					if(top) {
						p.setPose(Pose.SWIMMING);
					}
					//maybe needed later
				}
				p.setPos(x_pos + x_dif, y_pos + y_dif, z_pos + z_dif);
			}
		}
	}
	
	/*@SubscribeEvent
	public void onClientInput(MovementInputUpdateEvent event) {
		
	}
	
	@SubscribeEvent 
	public void onPlayerAttribute(EntityAttributeCreationEvent event) {
		
	}*/
	
	//https://forums.minecraftforge.net/topic/88603-solved-how-to-see-which-face-of-a-block-the-player-has-collided-with/
	//attach to wall and stop player from moving by taking event in for movement, as well let player move block he is attached to 
	@SubscribeEvent
	public void rightClick(PlayerInteractEvent.RightClickBlock event) {
		if(event.getItemStack().getItem() instanceof ClimbingClawItem) {
			//System.out.println("Item right clicked!");
			if(event.getFace() != null) {
				//System.out.println("Item right clicked a face!");
				Direction d = event.getFace();
				Axis a = d.getAxis();
				//System.out.println("Axis: " + a.toString() + ", Direction: " + d);
				BlockPos bp = event.getPos();
				BlockPos f = bp;
				BlockPos above;
				Player p = event.getEntity();
				//checking if within claw range
				if(first) {
					//System.out.println("Direction: " + d);
					//System.out.println(Math.abs(bp.getX() - p.getX()) + ", " + (Math.abs(bp.getY() - p.getY()) + ", " + (Math.abs(bp.getZ() - p.getZ()))));
					vertical = false;
					double x_off = p.getX() - bp.getX();
					double y_off = p.getY() - bp.getY();
					double z_off = p.getZ() - bp.getZ();
					
					switch(d) {
					case UP:
						if(Math.abs(x_off) > 1.2 || (Math.abs(y_off) > 1.0) || (Math.abs(z_off) > 1.2)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						//f = bp.above();
						vertical = true;
						top = true;
						//f.atY(bp.getY() + 1);
						break;
					case DOWN:
						if(Math.abs(x_off) > 0.7 || (Math.abs(y_off) > 2.0) || (Math.abs(z_off) > 0.7)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						//f = bp.below();
						vertical = true;
						top = false;
						//f.atY(bp.getY() - 1);
						break;
					case EAST:
						if(Math.abs(x_off) > 1.31 || (Math.abs(y_off) > 1.0) || (Math.abs(z_off) > 1.0)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						//f = bp.east();
						x_dir = false;
						above = new BlockPos(x_pos, y_pos + 1.0, z_pos);
						if(!p.getLevel().getBlockState(above).isAir()) {
							if(p.getLevel().getBlockState(above).getMaterial().isSolidBlocking()) {
								y_pos -= 1.0;
							}
						}
						//f = new BlockPos(f.getX() + 0.8, f.getY(), f.getZ());
						break;
					case WEST:
						if(Math.abs(x_off) > 0.31 || (Math.abs(y_off) > 1.0) || (Math.abs(z_off) > 1.0)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						//f = bp.west();
						x_dir = false;
						above = new BlockPos(x_pos, y_pos + 1.0, z_pos);
						if(!p.getLevel().getBlockState(above).isAir()) {
							if(p.getLevel().getBlockState(above).getMaterial().isSolidBlocking()) {
								y_pos -= 1.0;
							}
						}
						//f = new BlockPos(f.getX(), f.getY(), f.getZ());
						break;
					case SOUTH:
						if(Math.abs(x_off) > 1.0 || (Math.abs(y_off) > 1.0) || (Math.abs(z_off) > 1.31)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						//f = bp.south();
						x_dir = true;
						above = new BlockPos(x_pos, y_pos + 1.0, z_pos);
						if(!p.getLevel().getBlockState(above).isAir()) {
							if(p.getLevel().getBlockState(above).getMaterial().isSolidBlocking()) {
								y_pos -= 1.0;
							}
						}
						//f = new BlockPos(f.getX(), f.getY(), f.getZ() + 0.5);
						break;
					case NORTH:
						if(Math.abs(x_off) > 1.0 || (Math.abs(y_off) > 1.0) || (Math.abs(z_off) > 0.31)){
							p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
							return;
						}
						//f = bp.north();
						x_dir = true;
						above = new BlockPos(x_pos, y_pos + 1.0, z_pos);
						if(!p.getLevel().getBlockState(above).isAir()) {
							if(p.getLevel().getBlockState(above).getMaterial().isSolidBlocking()) {
								y_pos -= 1.0;
							}
						}
						//f = new BlockPos(f.getX(), f.getY(), f.getZ() - 0.5);
						break;
					}
					x_pos = f.getX() + x_off;
					y_pos = f.getY() + y_off;
					z_pos = f.getZ() + z_off;
					first = false;
				}else {
					switch(d) {
					case UP:
						vertical = true;
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
					x_pos = x_pos + x_dif;
					y_pos = y_pos + y_dif;
					z_pos = z_pos + z_dif;
				}
				
				//AABB bound = new AABB(new BlockPos(p.getX(), p.getY(), p.getZ()), new BlockPos(p.getX() + 0.5, p.getY() - 0.5, p.getZ()));
				//ArrayList<VoxelShape> cols = (ArrayList<VoxelShape>) p.getLevel().getCollisions(p, bound);
				/*if(p.isColliding(f, p.getLevel().getBlockState(f))) {
					System.out.println("Colliding");
				}*/
				//System.out.println("Above: " + above.getX() + ", " + above.getY() + ", " + above.getZ());
				System.out.println("org: " + bp.getX() + ", " + bp.getY() + ", " + bp.getZ());
				System.out.println(x_pos + ", " + y_pos + ", " + z_pos + ", axis: " + a);
				p.setPos(x_pos, y_pos, z_pos);
				x_dif = 0;
				y_dif = 0;
				z_dif = 0;
				attached = true;
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
