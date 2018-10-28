package de.mrnotsoevil.simplephysics;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void effectBlockCollapsed(BlockPos pos, IBlockState block) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        World world = player.getEntityWorld();
        world.spawnParticle(EnumParticleTypes.BLOCK_DUST, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0, Block.getStateId(block));
        world.spawnParticle(EnumParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0.1, 0);
        world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, block.getBlock().getSoundType().getBreakSound(), SoundCategory.BLOCKS, block.getBlock().getSoundType().getVolume(),
                block.getBlock().getSoundType().getPitch(), false);
    }

}
