package de.mrnotsoevil.simplephysics;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public WorldPhysics getPhysics(World world) {
        WorldPhysics physics = SimplePhysics.worldPhysicsMap.getOrDefault(world, null);
        if(physics == null) {
            physics = new WorldPhysics(world);
            SimplePhysics.worldPhysicsMap.put(world, physics);
        }
        return physics;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if(event.isCanceled())
            return;
        if(!event.getWorld().isRemote) {
            WorldPhysics physics = getPhysics(event.getWorld());
            physics.cancelPhysicsCheck(event.getPos());
            physics.queuePhysicsCheckAfterRemovalOf(event.getPos());
        }
    }

    @SubscribeEvent
    public void onBlockExplode(ExplosionEvent.Detonate event) {
        if(event.isCanceled())
            return;
        if(!event.getWorld().isRemote) {
            WorldPhysics physics = getPhysics(event.getWorld());
            if(physics == null)
                return;
            for (BlockPos pos : event.getExplosion().getAffectedBlockPositions()) {
                if(!physics.isAirOrIgnored(pos))
                  physics.queuePhysicsCheckAfterRemovalOf(pos);
            }
        }
    }

    @SubscribeEvent
    public void calculatePhysicsStep(TickEvent.ServerTickEvent event) {
        for(WorldPhysics worldPhysics : SimplePhysics.worldPhysicsMap.values()) {
            worldPhysics.tick(event);
        }
    }

    public void effectBlockCollapsed(BlockPos pos, IBlockState block) {

    }

}
