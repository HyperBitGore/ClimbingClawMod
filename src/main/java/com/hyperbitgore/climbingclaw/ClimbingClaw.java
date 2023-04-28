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

	private boolean attached;
	private boolean vertical = false;
	private boolean top = false;
	private boolean x_dir = false;
	private boolean first = false;
	private BlockPos last_pos = null;
	//current position
	private double x_pos = 0;
	private double y_pos = 0;
	private double z_pos = 0;
	//offset from block you move
	private double x_dif = 0;
	private double y_dif = 0;
	private double z_dif = 0;
	
	//used when vertical attachment for movement
	private Direction dir;
	
	// Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
	
	
	public static final String modid = "climbingclawmod";
	
	//item register
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ClimbingClaw.modid);
	//item properties
	
	
	//registering climbing claw item
	public static final RegistryObject<Item> CLIMBING_CLAW = ITEMS.register("climbingclaw", () -> new ClimbingClawItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1).durability(1000))); 
	
	//add you actually moving between blocks instead of teleporting
		//maybe just change to setting new pos from adding difs
	//add durability
	//improve moving while climbing
	
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
		switch(event.getKey()) {
		case InputConstants.KEY_SPACE:
			attached = false;
			break;
		case InputConstants.KEY_W:
			if(vertical) {
				x_dif += dir.getStepX() * 0.01;
				z_dif += dir.getStepZ() * 0.01;
			}else {
				y_dif += 0.01;
			}
			break;
		case InputConstants.KEY_S:
			if(vertical) {
				x_dif += dir.getStepX() * 0.01;
				z_dif += dir.getStepZ() * 0.01;
			}else {
				y_dif -= 0.01;
			}
			break;
		case InputConstants.KEY_A:
			if(x_dir) {
				x_dif += 0.01;
			}else {
				z_dif += 0.01;
			}
			break;
		case InputConstants.KEY_D:
			if(x_dir) {
				x_dif -= 0.01;
			}else {
				z_dif -= 0.01;
			}
			break;
		}
		if(Math.abs(x_dif) >= 1) {
			x_dif = 1;
		}
		if(Math.abs(y_dif) >= 1) {
			y_dif = 1;
		}
		if(Math.abs(z_dif) >= 1) {
			z_dif = 1;
		}
	}
	
	/*@SubscribeEvent
	public void onFall(LivingFallEvent event) {
		if(event.getEntity() instanceof Player) {
			System.out.println("Player falling");
		}
	}*/
	
	@SubscribeEvent
	public void onTick(TickEvent.PlayerTickEvent event) {
		Player p = event.player;
		if(last_pos != null) {
			if(attached) {
				if(vertical) {
					if(top) {
						p.setPose(Pose.SWIMMING);
					}
					dir = p.getDirection();
					p.sendSystemMessage(Component.translatable("Dir: " + dir.getStepX() + ", " + dir.getStepY() + ", " + dir.getStepZ()));
					
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
				if(Math.abs(bp.getX() - p.getX()) > 1.5 || (Math.abs(bp.getY() - p.getY()) > 1.5) || (Math.abs(bp.getZ() - p.getZ()) > 1.5)){
					p.sendSystemMessage(Component.translatable("You need to be closer to attach"));
					return;
				}
				
				vertical = false;
				switch(d) {
				case UP:
					f = bp.above();
					x_pos = f.getX() + 0.5;
					y_pos = f.getY() + 0.5;
					z_pos = f.getZ() + 0.5;
					vertical = true;
					top = true;
					//f.atY(bp.getY() + 1);
					break;
				case DOWN:
					f = bp.below();
					x_pos = f.getX() + 0.5;
					y_pos = f.getY() - 1;
					z_pos = f.getZ() + 0.5;
					vertical = true;
					top = false;
					//f.atY(bp.getY() - 1);
					break;
				case EAST:
					//f = bp.east();
					x_pos = f.getX() + 1.5;
					y_pos = f.getY();
					z_pos = f.getZ() + 0.5;
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
					//f = bp.west();
					x_pos = f.getX() - 0.5;
					y_pos = f.getY();
					z_pos = f.getZ() + 0.5;
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
					//f = bp.south();
					x_pos = f.getX() + 0.5;
					y_pos = f.getY();
					z_pos = f.getZ() + 1.5;
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
					//f = bp.north();
					x_pos = f.getX() + 0.5;
					y_pos = f.getY();
					z_pos = f.getZ() - 0.5;
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
				//AABB bound = new AABB(new BlockPos(p.getX(), p.getY(), p.getZ()), new BlockPos(p.getX() + 0.5, p.getY() - 0.5, p.getZ()));
				//ArrayList<VoxelShape> cols = (ArrayList<VoxelShape>) p.getLevel().getCollisions(p, bound);
				/*if(p.isColliding(f, p.getLevel().getBlockState(f))) {
					System.out.println("Colliding");
				}*/
				//System.out.println("Above: " + above.getX() + ", " + above.getY() + ", " + above.getZ());
				System.out.println("org: " + bp.getX() + ", " + bp.getY() + ", " + bp.getZ());
				System.out.println(x_pos + ", " + y_pos + ", " + z_pos + ", dir: " + d + ", axis: " + a);
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
