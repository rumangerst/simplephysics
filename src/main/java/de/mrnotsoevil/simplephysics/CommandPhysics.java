package de.mrnotsoevil.simplephysics;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.Map;

public class CommandPhysics extends CommandBase {
    @Override
    public String getName() {
        return "sphysics";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "sphysics <status|clear>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
       if(args.length >= 1) {
           if(args[0].equals("status")) {
               for(Map.Entry<World, WorldPhysics> entry : SimplePhysics.worldPhysicsMap.entrySet()) {
                   sender.sendMessage(new TextComponentString(entry.getKey().getWorldInfo().getWorldName() + ": " + entry.getValue().getQueueSize() + " operations running"));
               }
           }
           else if(args[0].equals("clear")) {
               for(Map.Entry<World, WorldPhysics> entry : SimplePhysics.worldPhysicsMap.entrySet()) {
                   entry.getValue().clearQueue();
               }
           }
       }
    }
}
