package de.mrnotsoevil.simplephysics.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MessageBlockCollapse implements IMessage {

    private int world;
    private int posX;
    private int posY;
    private int posZ;
    private int blockStateId;

    public MessageBlockCollapse() {

    }

    public MessageBlockCollapse(World world, BlockPos pos, IBlockState block) {
        this.world = world.provider.getDimension();
        this.posX = pos.getX();
        this.posY = pos.getY();
        this.posZ = pos.getZ();
        this.blockStateId = Block.getStateId(block);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        world = buf.readInt();
        posX = buf.readInt();
        posY = buf.readInt();
        posZ = buf.readInt();
        blockStateId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(getWorld());
        buf.writeInt(getPosX());
        buf.writeInt(getPosY());
        buf.writeInt(getPosZ());
        buf.writeInt(getBlockStateId());
    }

    public int getWorld() {
        return world;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public int getPosZ() {
        return posZ;
    }

    public int getBlockStateId() {
        return blockStateId;
    }
}

