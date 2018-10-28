package de.mrnotsoevil.simplephysics;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;

public class PhysicsBreakBlockEvent extends Event {

    private WorldPhysics worldPhysics;
    private BlockPos pos;
    private ItemStack[] drops;

    public PhysicsBreakBlockEvent(WorldPhysics physics, BlockPos pos) {
        this.worldPhysics = physics;
        this.pos = pos;
    }

    @Override
    public boolean isCancelable()
    {
        return true;
    }

    public WorldPhysics getWorldPhysics() {
        return worldPhysics;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ItemStack[] getDrops() {
        return drops;
    }

    public void setDrops(ItemStack[] drops) {
        this.drops = drops;
    }
}
