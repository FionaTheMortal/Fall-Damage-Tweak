package fionathemortal.falldamagetweak;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@Mod(FallDamageTweak.MOD_ID)
public class FallDamageTweak 
{
	public static final String MOD_ID = "falldamagetweak";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	
	private static final String  PROTOCOL_VERSION = "1";
	private static SimpleChannel NETWORK_CHANNEL;
	
	private static ConcurrentHashMap<Integer, Float> fallDamageMap = new ConcurrentHashMap<>();
	
	public
	FallDamageTweak()
	{
		MinecraftForge.EVENT_BUS.register(FallDamageTweak.class);
		
		initChannelAndRegisterPackets();
	}
	
	public static void 
	initChannelAndRegisterPackets()
	{
		NETWORK_CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(FallDamageTweak.MOD_ID, "main"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals);
		
		int messageID = 0;
		
		NETWORK_CHANNEL.registerMessage(
			messageID++, 
			FallDamagePacket.class, 
			FallDamagePacket::encodeFallDamagePacket,
			FallDamagePacket::decodeFallDamagePacket, 
			FallDamagePacket::handleFallDamagePacket);
	}
	
	public static void	
	sendFallDamagePacketToServer(ClientPlayerEntity player, float distance, float damageMultiplier)
	{
		NETWORK_CHANNEL.sendTo(
			new FallDamagePacket(distance, damageMultiplier),
			player.connection.getNetworkManager(), 
			NetworkDirection.PLAY_TO_SERVER);
	}
	
	public static boolean
	isClientSide(Entity entity)
	{
		return entity.world.isRemote;
	}
	
	public static float
	getFallDistanceFromVelocity(double velocity)
	{
		float result = 0.0f;
		
		float vy_f32 = (float)velocity;
		
		if (velocity < 0.0)
		{
			float v_sqr  = vy_f32 * vy_f32;
			float v_cube = v_sqr * v_sqr;
			
			if (vy_f32 > -2.137f)
			{
				final float a = -0x1.ad6b2963f7138p-4f + 0x1.1200b00000000p-4f;
				final float b =  0x1.8a9c601c1aa83p+2f;
				final float c =  0x1.310a0384cb755p+0f;
				final float d = -0x1.1b5955fb8c6a6p-3f;
				final float e =  0x1.daf267ef86b17p-7f;
				
				float r1 = a + (b + c * v_sqr) * v_sqr;
				float r2 = (d + e * v_sqr) * v_cube * v_sqr;
				
				result = r1 + r2;
			}
			else
			{
				final float a =  0x1.fa43007c54cacp+5f + 0x1.4cb2938a44e00p-2f;
				final float b = -0x1.e1e800f4a15a9p+4f;
				final float c =  0x1.154f8050a89aap+3f;
				final float d = -0x1.88796591fd14fp-1f;
				final float e =  0x1.e91b30952764bp-6f;

				float r1 = a + (b + c * v_sqr) * v_sqr;
				float r2 = (d + e * v_sqr) * v_cube * v_sqr;
				
				result = r1 + r2;
			}
		}
		
		return result;
	}
	
	public static void
	applyClientFallDamage(ServerPlayerEntity entity, float simulatedDistance, float damageMultiplier)
	{
		fallDamageMap.put(entity.getEntityId(), simulatedDistance);
		
		entity.onLivingFall(simulatedDistance, damageMultiplier);
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	@SuppressWarnings("resource")
	public static void
	livingFallEvent(LivingFallEvent event)
	{
		Entity entity = event.getEntity();
		
		if (isClientSide(entity))
		{
			float originalDistance = event.getDistance();			
			float velocityDistance = getFallDistanceFromVelocity(entity.getMotion().y);

			float distance = originalDistance;
			
			if (velocityDistance < originalDistance)
			{
				distance = velocityDistance;
				
				event.setDistance(distance);
			}
			
            if (Minecraft.getInstance().player == entity)
            {
            	sendFallDamagePacketToServer((ClientPlayerEntity)entity, distance, event.getDamageMultiplier());
            }
		}
		else
		{
	        if (entity instanceof ServerPlayerEntity)
	        {
	            if (!fallDamageMap.containsKey(entity.getEntityId()))
	            {
	            	event.setCanceled(true);
	            }
	            else
	            {
	            	fallDamageMap.remove(entity.getEntityId());
	            }
	        }
	        else
	        {
				float originalDistance = event.getDistance();			
				float velocityDistance = getFallDistanceFromVelocity(entity.getMotion().y);

				float distance = originalDistance;
				
				if (velocityDistance < originalDistance)
				{
					distance = velocityDistance;
					
					event.setDistance(distance);
				}
	        }
		}
	}
}