package de.mrnotsoevil.simplephysics;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CommandPhysics extends CommandBase {
    @Override
    public String getName() {
        return "physics";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "physics <status|clear|reload>";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if(args == null || args.length == 0)
            return Arrays.asList("status", "clear", "reload");
        else
            return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
       if(args.length >= 1) {
           if(args[0].equals("status")) {
               if( SimplePhysics.worldPhysicsMap.isEmpty())
                   sender.sendMessage(new TextComponentString("[SimplePhysics] No physics operations running."));
               for(Map.Entry<World, WorldPhysics> entry : SimplePhysics.worldPhysicsMap.entrySet()) {
                   sender.sendMessage(new TextComponentString("[SimplePhysics] " + entry.getKey().getWorldInfo().getWorldName() + "_" +
                           entry.getKey().provider.getDimension() + ": " + entry.getValue().getQueueSize() + " operations running"));
               }
           }
           else if(args[0].equals("clear")) {
               for(Map.Entry<World, WorldPhysics> entry : SimplePhysics.worldPhysicsMap.entrySet()) {
                   entry.getValue().clearQueue();
               }
               sender.sendMessage(new TextComponentString("[SimplePhysics] All physics operations removed."));
           }
           else if(args[0].equals("reload")) {
               try {
                   SimplePhysics.configuration.load();
               }
               catch (Exception e) {
                   e.printStackTrace();
               }
               for(Map.Entry<World, WorldPhysics> entry : SimplePhysics.worldPhysicsMap.entrySet()) {
                   entry.getValue().loadConfiguration();
               }
               sender.sendMessage(new TextComponentString("[SimplePhysics] Configuration reloaded."));
           }
       }
    }
}
