package de.mrnotsoevil.simplephysics;

import de.mrnotsoevil.simplephysics.networking.MessageBlockCollapse;
import de.mrnotsoevil.simplephysics.networking.NetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

public class WorldPhysics {

    private static PriorityQueue<PhysicsPath> currentPaths = new PriorityQueue<>(Comparator.comparingInt(a -> a.getCheckedPosition().getY()));

    private static Set<BlockPos> currentChecks = new HashSet<>();

    private World world;

    /**
     * Limit for looking for a way wher ethe force has to go
     */
    private int maxHorizontalSearch = 8;

    /**
     * How many steps the physics calculates per server tick (20 Server ticks = 1s)
     */
    private int calculationSpeed = 1;

    /**
     * If an anchor is found, the connecting structure is determined as stable
     */
    private Block[] anchorBlocks = new Block[] {
            Blocks.BEDROCK
    };

    /**
     * Blocks that do not contribute to physics
     */
    private Block[] ignoredBlocks = getDefaultIgnoredBlocks();

    /**
     * Include diagonal blocks in calculations
     */
    private boolean includeDiagonalBlocks = true;

    public WorldPhysics(World world) {
        this.world = world;
    }

    public static Block[] getDefaultIgnoredBlocks() {
        ArrayList<Block> result = new ArrayList<>();
        result.add(Blocks.CAKE);
        result.add(Blocks.VINE);
        result.add(Blocks.BROWN_MUSHROOM);
        result.add(Blocks.RED_MUSHROOM);
        result.add(Blocks.LADDER);
        result.add(Blocks.CARPET);
        for(Block block : ForgeRegistries.BLOCKS.getValues()) {
            if(block != Blocks.LEAVES && block != Blocks.LEAVES2) {
                if(block.getSoundType() == SoundType.PLANT)
                    result.add(block);
            }
        }
        return result.toArray(new Block[0]);
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
     * Checks the physics of given block pos
     * @param pos
     */
    public void queuePhysicsCheck(BlockPos pos) {
        if(world.getBlockState(pos).getBlock() != Blocks.AIR &&
                !isIgnoredBlock(world.getBlockState(pos).getBlock())) {
            if(!currentChecks.contains(pos)) {
//                System.out.println("Queued physics check at " + pos);
                currentPaths.add(new PhysicsPath(this, pos));
                currentChecks.add(pos);
            }
        }
    }

    public void queuePhysicsCheckAfterRemovalOf(BlockPos pos) {
        queuePhysicsCheck(pos.up());
        queuePhysicsCheck(pos.north());
        queuePhysicsCheck(pos.south());
        queuePhysicsCheck(pos.east());
        queuePhysicsCheck(pos.west());

        if(includeDiagonalBlocks) {
            queuePhysicsCheck(pos.up().north());
            queuePhysicsCheck(pos.up().south());
            queuePhysicsCheck(pos.up().east());
            queuePhysicsCheck(pos.up().west());
        }
    }

    private void breakBlock(BlockPos pos) {
        IBlockState srcBlock = world.getBlockState(pos);
//        if(!world.isRemote) {
//            WorldServer ws = (WorldServer)world;
//            ws.spawnParticle(EnumParticleTypes.BLOCK_DUST, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.0, 0.0, Block.getStateId(srcBlock));
//            ws.spawnParticle(EnumParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0 ,0);
//        }
        NetworkHandler.channel.sendToAll(new MessageBlockCollapse(world, pos, srcBlock));
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }

    public void tick(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.END) {
            if(!currentPaths.isEmpty()) {
                PhysicsPath path = currentPaths.peek();
                for(int i = 0; i < calculationSpeed - 1; ++i) {
                    path.iterate();
                }
                if (path.iterate()) {
//                System.out.println("Physics calculation finished and returned " + path.getResult());
                    currentPaths.poll(); // Remove the path
                    currentChecks.remove(path.getCheckedPosition());

                    // Take a look at the result
                    if (path.getResult() == PhysicsPath.Result.IsUnsupported) {
                        breakBlock(path.getCheckedPosition());
                        queuePhysicsCheckAfterRemovalOf(path.getCheckedPosition());
                    }
                }
            }
        }
    }

    public boolean isIgnoredBlock(Block block) {
        for(Block b : ignoredBlocks) {
            if(b == block)
                return true;
        }
        return false;
    }

    public boolean isAnchorBlock(Block block) {
        for(Block b : anchorBlocks) {
            if(b == block)
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
}
