package de.mrnotsoevil.simplephysics;

import de.mrnotsoevil.simplephysics.networking.MessageBlockCollapse;
import de.mrnotsoevil.simplephysics.networking.NetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;
import java.util.stream.Stream;

public class WorldPhysics {

    private static final Block[] defaultAnchorBlocks = new Block[] { Blocks.BEDROCK };

    private static final Block[] defaultIgnoredBlocks = new Block[]{
            Blocks.CAKE,
            Blocks.CACTUS,
            Blocks.LEAVES2,
            Blocks.LEAVES,
            Blocks.BROWN_MUSHROOM,
            Blocks.RED_MUSHROOM,
            Blocks.VINE,
            Blocks.BEETROOTS,
            Blocks.BREWING_STAND,
            Blocks.CARROTS,
            Blocks.DAYLIGHT_DETECTOR,
            Blocks.DAYLIGHT_DETECTOR_INVERTED,
            Blocks.DEADBUSH,
            Blocks.DETECTOR_RAIL,
            Blocks.DOUBLE_PLANT,
            Blocks.DRAGON_EGG,
            Blocks.REDSTONE_WIRE,
            Blocks.TALLGRASS,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.FLOWING_WATER,
            Blocks.FLOWING_LAVA
    };

    private PriorityQueue<PhysicsPath> currentPaths;

    private Map<BlockPos, PhysicsPath> currentChecks = new HashMap<>();

    private World world;

    /**
     * Limit for looking for a way wher ethe force has to go
     */
    private int maxHorizontalSearch = 8;

    /**
     * How many steps the physics calculates per server tick (20 Server ticks = 1s)
     */
    private int calculationSpeed = 4;

    /**
     * If an anchor is found, the connecting structure is determined as stable
     */
    private Block[] anchorBlocks = defaultAnchorBlocks;

    /**
     * Blocks that do not contribute to physics
     */
    private Block[] ignoredBlocks;

    /**
     * Include diagonal blocks in calculations
     */
    private boolean includeDiagonalBlocks = true;

    /**
     * Physics stops working below this height.
     * It is assumed that all blocks are anchors
     */
    private int anchorHeight = 20;

    /**
     * Assume a block as physically supported if the search is this number of blocks deeper
     */
    private int maxVerticalSearch = 16;

    public WorldPhysics(World world) {
        this.currentPaths = new PriorityQueue<>(Comparator.comparingInt((PhysicsPath lhs) ->
                lhs.getCheckedPosition().getY()).
                thenComparingInt(lhs -> lhs.getCheckedPosition().getX() % 2).
                thenComparingInt(lhs -> ((lhs.getCheckedPosition().getZ() + 1) % 2)));
        this.world = world;
        this.ignoredBlocks = defaultIgnoredBlocks;

        // Load data from config
       loadConfiguration();

    }

    public void loadConfiguration() {
        final String configCategory = world.getWorldInfo().getWorldName() + "_" + world.provider.getDimension();
        boolean changed = false;
        try {
            {
                Property property = SimplePhysics.configuration.get(configCategory,
                        "MaxHorizontalSearch",
                        8,
                        "How far blocks look for stability in horizontal direction. Please note that higher values increase memory load.",
                        1,
                        64);
                maxHorizontalSearch = property.getInt();
                changed = changed || property.hasChanged();
            }
            {
                Property property = SimplePhysics.configuration.get(configCategory,
                        "MaxVerticalSearch",
                        16,
                        "How far blocks look for stability in vertical direction. Please note that higher values increase memory load.",
                        1,
                        256);
                maxVerticalSearch = property.getInt();
                changed = changed || property.hasChanged();
            }
            {
                Property property = SimplePhysics.configuration.get(configCategory,
                        "CalculationSpeed",
                        1,
                        "How many physic ticks should be calculated per server tick. Higher values may cause lag.",
                        1,
                        100);
                calculationSpeed = property.getInt();
                changed = changed || property.hasChanged();
            }
            {
                Property property = SimplePhysics.configuration.get(configCategory,
                        "IncludeDiagonalBlocks",
                        true,
                        "If a block is broken, trigger calculation of all 9 blocks above it (instead of only 5). Can increase server load, but works better with some structures.");
                includeDiagonalBlocks = property.getBoolean();
                changed = changed || property.hasChanged();
            }
            {
                int defaultHeight = 20;
                if (world.getWorldInfo().getTerrainType() == WorldType.FLAT) {
                    defaultHeight = 0;
                }
                Property property = SimplePhysics.configuration.get(configCategory,
                        "AnchorHeight",
                        defaultHeight,
                        "All blocks at this height or below are considered as stable.",
                        0,
                        256);
                anchorHeight = property.getInt();
                changed = changed || property.hasChanged();
            }
            {
                Property property = SimplePhysics.configuration.get(configCategory,
                        "AnchorBlocks",
                        Stream.of(defaultAnchorBlocks).map(block -> ForgeRegistries.BLOCKS.getKey(block).toString()).toArray(String[]::new),
                        "Blocks that are determined as stable.");
                anchorBlocks = Stream.of(property.getStringList()).map(name -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name))).toArray(Block[]::new);
                changed = changed || property.hasChanged();
            }
            {
                Property property = SimplePhysics.configuration.get(configCategory,
                        "IgnoredBlocks",
                        Stream.of(defaultIgnoredBlocks).map(block -> ForgeRegistries.BLOCKS.getKey(block).toString()).toArray(String[]::new),
                        "Blocks that are ignored by the physics.");
                ignoredBlocks = Stream.of(property.getStringList()).map(name -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name))).toArray(Block[]::new);
                changed = changed || property.hasChanged();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        if(changed) {
            try {
                SimplePhysics.configuration.save();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isAirOrIgnored(BlockPos pos) {
        return isAirOrIgnored(world.getBlockState(pos).getBlock());
    }

    public boolean isAirOrIgnored(Block block) {
        return block == Blocks.AIR || isIgnoredBlock(block);
    }

    public int getMaxHorizontalSearch() {
        return maxHorizontalSearch;
    }

    public void setMaxHorizontalSearch(int maxHorizontalSearch) {
        this.maxHorizontalSearch = maxHorizontalSearch;
    }

    public int getCalculationSpeed() {
        return calculationSpeed;
    }

    public void setCalculationSpeed(int calculationSpeed) {
        this.calculationSpeed = calculationSpeed;
    }

    public Block[] getAnchorBlocks() {
        return anchorBlocks;
    }

    public void setAnchorBlocks(Block[] anchorBlocks) {
        this.anchorBlocks = anchorBlocks;
    }

    public Block[] getIgnoredBlocks() {
        return ignoredBlocks;
    }

    public void setIgnoredBlocks(Block[] ignoredBlocks) {
        this.ignoredBlocks = ignoredBlocks;
    }

    public int getQueueSize() {
        return currentPaths.size();
    }

    public void clearQueue() {
        currentPaths.clear();
        currentChecks.clear();
    }

    /**
     * Triggers a physics check
     * Will clear any internal caches
     *
     * @param pos
     * @return
     */
    public PhysicsPath tryQueuePhysicsCheck(BlockPos pos) {

        Block block = world.getBlockState(pos).getBlock();

        if (isAirOrIgnored(block))
            return null;

        if (pos.getY() <= anchorHeight)
            return new PhysicsPath(this, pos, PhysicsPath.Result.IsSupported);

        PhysicsPath path = currentChecks.getOrDefault(pos, null);
        if (path == null) {
            path = new PhysicsPath(this, pos, maxVerticalSearch);
            currentPaths.add(path);
            currentChecks.put(pos, path);

            if (world.getBlockState(pos).getBlock() == Blocks.AIR)
                path.cancel(PhysicsPath.Result.IsUnsupported);
        }

        return path;
    }

    /**
     * Triggers an optimized version of a physics check
     *
     * @param pos
     * @return
     */
    public PhysicsPath tryQueueInternalPhysicsCheck(BlockPos pos, int verticalLimit) {

        if (pos.getY() <= anchorHeight)
            return new PhysicsPath(this, pos, PhysicsPath.Result.IsSupported);
        if (isAirOrIgnored(pos))
            return new PhysicsPath(this, pos, PhysicsPath.Result.IsUnsupported);

        PhysicsPath path = currentChecks.getOrDefault(pos, null);
        if (path == null) {
            path = new PhysicsPath(this, pos, verticalLimit);
            currentPaths.add(path);
            currentChecks.put(pos, path);
        }

        return path;
    }

    public void cancelPhysicsCheck(BlockPos pos) {
        PhysicsPath path = currentChecks.getOrDefault(pos, null);
        if (path != null) {
            if (isAirOrIgnored(pos))
                path.cancel(PhysicsPath.Result.IsUnsupported);
            else
                path.cancel(PhysicsPath.Result.IsSupported);
        }
    }

    public void queuePhysicsCheckAfterRemovalOf(BlockPos pos) {

        cancelPhysicsCheck(pos);

        tryQueuePhysicsCheck(pos.up());
        tryQueuePhysicsCheck(pos.north());
        tryQueuePhysicsCheck(pos.south());
        tryQueuePhysicsCheck(pos.east());
        tryQueuePhysicsCheck(pos.west());

        if (includeDiagonalBlocks) {
            tryQueuePhysicsCheck(pos.up().north());
            tryQueuePhysicsCheck(pos.up().south());
            tryQueuePhysicsCheck(pos.up().east());
            tryQueuePhysicsCheck(pos.up().west());
        }
    }

    private void breakBlock(BlockPos pos) {
        IBlockState srcBlock = world.getBlockState(pos);
        if (isAirOrIgnored(pos))
            return;
        PhysicsBreakBlockEvent event = new PhysicsBreakBlockEvent(this, pos);
        event.setDrops(new ItemStack[]{
                new ItemStack(srcBlock.getBlock().getItemDropped(srcBlock, SimplePhysics.random, 0),
                        srcBlock.getBlock().quantityDropped(srcBlock, 0, SimplePhysics.random))
        });
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            if (isAirOrIgnored(pos))
                return;
            NetworkHandler.channel.sendToAll(new MessageBlockCollapse(world, pos, srcBlock));
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            if (event.getDrops() != null) {
                for (ItemStack stack : event.getDrops()) {
                    if (stack != null)
                        world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
                }
            }
        }
        queuePhysicsCheckAfterRemovalOf(pos);
    }

    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            int iterations = 0;
            while (!currentPaths.isEmpty()) {
                PhysicsPath path = currentPaths.peek();
                if (path.iterate()) {
//                System.out.println("Physics calculation finished and returned " + path.getResult());
                    currentPaths.poll(); // Remove the path
                    currentChecks.remove(path.getCheckedPosition());

                    // Take a look at the result
                    if (path.getResult() == PhysicsPath.Result.IsUnsupported) {
                        breakBlock(path.getCheckedPosition());
                    }
                } else {
                    ++iterations;
                    if(iterations >= calculationSpeed)
                        break;
                }
            }
        }
    }

    public boolean isIgnoredBlock(Block block) {
        for (Block b : ignoredBlocks) {
            if (b == block)
                return true;
        }
        return false;
    }

    public boolean isAnchorBlock(Block block) {
        for (Block b : anchorBlocks) {
            if (b == block)
                return true;
        }
        return false;
    }

    public World getWorld() {
        return world;
    }


    public boolean isIncludeDiagonalBlocks() {
        return includeDiagonalBlocks;
    }

    public void setIncludeDiagonalBlocks(boolean includeDiagonalBlocks) {
        this.includeDiagonalBlocks = includeDiagonalBlocks;
    }

    public int getAnchorHeight() {
        return anchorHeight;
    }

    public void setAnchorHeight(int anchorHeight) {
        this.anchorHeight = anchorHeight;
    }

    public int getMaxVerticalSearch() {
        return maxVerticalSearch;
    }

    public void setMaxVerticalSearch(int maxVerticalSearch) {
        this.maxVerticalSearch = maxVerticalSearch;
    }
}
