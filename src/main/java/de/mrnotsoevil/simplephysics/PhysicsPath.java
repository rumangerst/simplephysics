package de.mrnotsoevil.simplephysics;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class PhysicsPath {
    private WorldPhysics worldPhysics;
    private BlockPos checkedPosition;
    private boolean[] visited;
    private Stack<BlockPos> toVisit = new Stack<>();
    private int availableVerticalLimit = 0;

    private List<PhysicsPath> pathsBelow = new ArrayList<>();
    private Result result = Result.Undecided;


    public PhysicsPath(WorldPhysics world, BlockPos checkedPosition, int availableVerticalLimit) {
        this.worldPhysics = world;
        this.checkedPosition = checkedPosition;
        this.visited = null;
        this.toVisit.push(checkedPosition);
        this.availableVerticalLimit = availableVerticalLimit;
    }

    public PhysicsPath(WorldPhysics world, BlockPos checkedPosition, Result result) {
        if(result == Result.Undecided) {
            throw new RuntimeException("Cannot instantiate predetermined path with unknown result!");
        }
        this.worldPhysics = world;
        this.checkedPosition = checkedPosition;
        this.result = result;
    }

    private void visit(BlockPos other) {

        final int visited_w = 2 * worldPhysics.getMaxHorizontalSearch() + 1;
        final int visited_c = worldPhysics.getMaxHorizontalSearch();

        int x = other.getX() - getCheckedPosition().getX() + visited_c;
        int y = other.getZ() - getCheckedPosition().getZ() + visited_c;
        if(x >= 0 && x < visited_w && y >= 0 && y < visited_w)
            visited[x + y * visited_w] = true;
    }

    private boolean isVisited(BlockPos other) {

        final int visited_w = 2 * worldPhysics.getMaxHorizontalSearch() + 1;
        final int visited_c = worldPhysics.getMaxHorizontalSearch();

        int x = other.getX() - getCheckedPosition().getX() + visited_c;
        int y = other.getZ() - getCheckedPosition().getZ() + visited_c;
        if(x < 0 || x >= visited_w || y < 0 || y >= visited_w)
            return true;
        return visited[x + y * visited_w];
    }

    private void tryQueueVisit(BlockPos pos) {
        Block block = worldPhysics.getWorld().getBlockState(pos).getBlock();
        if(!isVisited(pos) && block != Blocks.AIR && !worldPhysics.isIgnoredBlock(block)) {
            visit(pos);
            toVisit.push(pos);
        }
    }

    public void cancel(Result result) {
        if(result == Result.Undecided) {
            throw new RuntimeException("Cannot cancel with unknown result!");
        }
        this.result = result;
    }

    /**
     * Iterates the path. Returns true if it is done
     * @return
     */
    public boolean iterate() {

        if(result != Result.Undecided)
            return true;
        Chunk chunk = worldPhysics.getWorld().getChunkFromBlockCoords(checkedPosition);
        if(chunk.unloadQueued || !chunk.isLoaded()) {
            result = Result.IsSupported;
            return true;
        }
        Block currentBlock = worldPhysics.getWorld().getBlockState(getCheckedPosition()).getBlock();
        if(currentBlock == Blocks.AIR) {
            result = Result.IsUnsupported;
            return true;
        }
        else if(worldPhysics.isIgnoredBlock(currentBlock)) {
            result = Result.IsSupported;
            return true;
        }
        else if(availableVerticalLimit == 0) {
            result = Result.IsSupported;
            return true;
        }

        if(!pathsBelow.isEmpty()) {
            for(int i = 0; i < pathsBelow.size(); ++i) {
                PhysicsPath p = pathsBelow.get(i);
                if(p.getResult() != Result.Undecided) {
                    if((getCheckedPosition().getY() - p.getLowestY()) >= worldPhysics.getMaxHorizontalSearch()) {
                        result = Result.IsSupported;
                        pathsBelow.clear();
                        return true;
                    }
                    if(p.getResult() == Result.IsSupported) {
                        result = Result.IsSupported;
                        pathsBelow.clear();
                        return true;
                    }
                    else {
                        // Quick-delete
                        if(pathsBelow.size() == 1) {
                            pathsBelow.clear();
                        }
                        else {
                            pathsBelow.set(0, pathsBelow.get(pathsBelow.size() - 1));
                            pathsBelow.remove(pathsBelow.size() - 1);
                        }
                    }
                }
            }
        }

        if(!toVisit.empty()) {
            BlockPos currentPos = toVisit.pop();
            Block block = worldPhysics.getWorld().getBlockState(currentPos).getBlock();

            // If current block is anchor -> Done
            // If it's air, cancel here
            if(worldPhysics.isAnchorBlock(block)) {
                result = Result.IsSupported;
                return true;
            }
            else if(worldPhysics.isAirOrIgnored(block)) {
                return false;
            }

            // Check the block below
            BlockPos posBelow = currentPos.down();
            Block blockBelow =  worldPhysics.getWorld().getBlockState(posBelow).getBlock();
            if(worldPhysics.isAnchorBlock(blockBelow)) {
                result = Result.IsSupported;
                return true;
            }
            else if(blockBelow != Blocks.AIR && !worldPhysics.isIgnoredBlock(blockBelow)) {
                pathsBelow.add(worldPhysics.tryQueueInternalPhysicsCheck(posBelow, availableVerticalLimit - 1));
            }

            // Check the blocks to the side (if not visited)
            if(visited == null) {
                // We are the starter block (center)
                final int visited_w = 2 * worldPhysics.getMaxHorizontalSearch() + 1;
                visited = new boolean[visited_w * visited_w];
            }

            visit(currentPos);

            tryQueueVisit(currentPos.north());
            tryQueueVisit(currentPos.south());
            tryQueueVisit(currentPos.west());
            tryQueueVisit(currentPos.east());

            return false;
        }
        else {
            result = Result.IsUnsupported;
            return true;
        }
    }

    public int getLowestY() {
        int y = getCheckedPosition().getY();
        for(PhysicsPath p : pathsBelow) {
            y = Math.min(p.getLowestY(), y);
        }
        return y;
    }

    public Result getResult() {
        return result;
    }

    public BlockPos getCheckedPosition() {
        return checkedPosition;
    }

    public WorldPhysics getWorldPhysics() {
        return worldPhysics;
    }

    public enum Result {
        Undecided,
        IsSupported,
        IsUnsupported
    }
}
