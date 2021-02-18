package fionathemortal.falldamagetweak;

import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class FallDamagePacket 
{
    float distance;
    float damageMultiplier;
	
    public
    FallDamagePacket(float distance, float damageMultiplier)
    {
    	this.distance = distance;
    	this.damageMultiplier = damageMultiplier;
    }
    
	public static void
	encodeFallDamagePacket(FallDamagePacket packet, PacketBuffer packetBuffer)
	{
		packetBuffer.writeFloat(packet.distance);
		packetBuffer.writeFloat(packet.damageMultiplier);
	}

	public static FallDamagePacket 
	decodeFallDamagePacket(PacketBuffer packetBuffer)
	{
		float calculatedDistanceFromVelocit = packetBuffer.readFloat();
		float damageMultiplier = packetBuffer.readFloat();
	
		FallDamagePacket result = new FallDamagePacket(calculatedDistanceFromVelocit, damageMultiplier);
		
		return result;
	}	
	
	public static void 
	handleFallDamagePacket(FallDamagePacket packet, Supplier<NetworkEvent.Context> context)
	{
		ServerPlayerEntity player = context.get().getSender();
		
		context.get().enqueueWork(() -> FallDamageTweak.applyClientFallDamage(player, packet.distance, packet.damageMultiplier));
	}
}
