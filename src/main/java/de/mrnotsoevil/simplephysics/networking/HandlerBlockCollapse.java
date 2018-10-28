package de.mrnotsoevil.simplephysics.networking;

import de.mrnotsoevil.simplephysics.SimplePhysics;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class HandlerBlockCollapse implements IMessageHandler<MessageBlockCollapse, IMessage> {
    @Override
    public IMessage onMessage(MessageBlockCollapse message, MessageContext ctx) {
        NetworkHandler.getClientThreadListener().addScheduledTask(() -> {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if(message.getWorld() == player.getEntityWorld().provider.getDimension()) {
                SimplePhysics.proxy.effectBlockCollapsed(new BlockPos(message.getPosX(), message.getPosY(), message.getPosZ()), Block.getStateById(message.getBlockStateId()));
            }
        });
        return null;
    }
}
