package de.mrnotsoevil.simplephysics;

import de.mrnotsoevil.simplephysics.networking.NetworkHandler;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod(modid = SimplePhysics.MODID, name = SimplePhysics.NAME, version = SimplePhysics.VERSION)
public class SimplePhysics
{
    public static final String MODID = "simplephysics";
    public static final String NAME = "Simple Physics";
    public static final String VERSION = "1.0";

    public static Logger logger;

    @SidedProxy(serverSide = "de.mrnotsoevil.simplephysics.CommonProxy", clientSide = "de.mrnotsoevil.simplephysics.ClientProxy")
    public static CommonProxy proxy;

    public static Map<World, WorldPhysics> worldPhysicsMap = new HashMap<>();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        NetworkHandler.init();
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandPhysics());
    }

}
