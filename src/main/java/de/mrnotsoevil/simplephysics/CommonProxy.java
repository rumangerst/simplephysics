package de.mrnotsoevil.simplephysics;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if(!event.getWorld().isRemote) {
            // Queue into the physics
            if(SimplePhysics.worldPhysicsMap.containsKey(event.getWorld())) {
                SimplePhysics.worldPhysicsMap.get(event.getWorld()).queuePhysicsCheckAfterRemovalOf(event.getPos());
            }
            else {
                WorldPhysics physics = new WorldPhysics(event.getWorld());
                physics.queuePhysicsCheckAfterRemovalOf(event.getPos());
                SimplePhysics.worldPhysicsMap.put(event.getWorld(), physics);
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
