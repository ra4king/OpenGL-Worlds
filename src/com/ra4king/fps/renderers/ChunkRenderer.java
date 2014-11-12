package com.ra4king.fps.renderers;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import com.ra4king.fps.renderers.WorldRenderer.DrawElementsIndirectCommand;
import com.ra4king.fps.world.Chunk;
import com.ra4king.fps.world.Chunk.Block;
import com.ra4king.fps.world.Chunk.BlockType;
import com.ra4king.fps.world.Chunk.ChunkModifiedCallback;
import com.ra4king.opengl.util.Stopwatch;
import com.ra4king.opengl.util.buffers.GLBuffer;

import net.indiespot.struct.cp.Struct;
import net.indiespot.struct.cp.TakeStruct;

public class ChunkRenderer implements ChunkModifiedCallback {
	private Chunk chunk;
	private GLBuffer glBuffer;
	private int chunkNumOffset;
	
	private ByteBuffer buffer;
	private Block[] compact;
	private int blockCount;
	
	private boolean chunkModified = true;
	
	public static final int CHUNK_DATA_SIZE = Chunk.TOTAL_BLOCKS * Struct.sizeof(Block.class);
	
	public ChunkRenderer(Chunk chunk, GLBuffer glBuffer, int chunkNumOffset) {
		this.chunk = chunk;
		this.glBuffer = glBuffer;
		this.chunkNumOffset = chunkNumOffset;
		
		// map the Block array to a ByteBuffer
		buffer = BufferUtils.createByteBuffer(CHUNK_DATA_SIZE);
		compact = Struct.map(Block.class, buffer);
		
		chunk.setCallback(this);
	}
	
	public Chunk getChunk() {
		return chunk;
	}
	
	@Override
	@TakeStruct
	public Block chunkInit(int x, int y, int z, BlockType type) {
		chunkModified = true;
		
		if(blockCount >= Chunk.TOTAL_BLOCKS) {
			throw new IllegalArgumentException("Already initialized all Blocks.");
		}
		
		Block b = compact[blockCount++].init(chunk, x, y, z, type);
		return b;
	}
	
	@Override
	public void chunkRemoved(Block block) {
		chunkModified = true;
	}
	
	@Override
	public void chunkModified(Block block) {
		chunkModified = true;
	}
	
	// private static Block[] updatedBlocks = Struct.emptyArray(Block.class, 100);
	//
	// private void updateBlocks(Block[] blocks, int count) {
	// System.out.println("Update blocks: " + count);
	//
	// int updatedBlocksIndex = 0;
	//
	// int extras = 0;
	//
	// for(int a = 0; a < count; a++) {
	// Block block = blocks[a];
	//
	// if(!arrayContains(updatedBlocks, updatedBlocksIndex, block)) {
	// updateBlock(block);
	// updatedBlocks = add(updatedBlocks, updatedBlocksIndex++, block);
	// } else {
	// extras++;
	// }
	//
	// Block[] neighbors = chunk.getNeighbors(block);
	// for(Block n : neighbors) {
	// if(!arrayContains(updatedBlocks, updatedBlocksIndex, n)) {
	// updateBlock(n);
	// updatedBlocks = add(updatedBlocks, updatedBlocksIndex++, n);
	// } else {
	// extras++;
	// }
	// }
	// }
	//
	// System.out.println("Updated changes: " + updatedBlocksIndex);
	// System.out.println("Update extra blocks: " + extras);
	// }
	//
	// private void updateBlock(Block b) {
	// int idx = getBlockIndex(b);
	//
	// if(idx == -1) {
	// throw new IllegalArgumentException(String.format("Invalid block: (%d,%d,%d) of type %s", b.getX(), b.getY(), b.getZ(), b.getType()));
	// }
	//
	// if(idx >= blockCount && (b.getType() != BlockType.AIR && !b.isSurrounded(chunk))) {
	// Block src = compact[blockCount++];
	//
	// if(src == b) {
	// return;
	// }
	//
	// Struct.swap(Block.class, src, b);
	//
	// Block[] chunkBlocks = chunk.getBlocks();
	// chunkBlocks[chunk.posToArrayIndex(src.getX(), src.getY(), src.getZ())] = src;
	// chunkBlocks[chunk.posToArrayIndex(b.getX(), b.getY(), b.getZ())] = b;
	// } else if(idx < blockCount && (b.getType() == BlockType.AIR || b.isSurrounded(chunk))) {
	// Block src = compact[--blockCount];
	//
	// if(src == b) {
	// return;
	// }
	//
	// Struct.swap(Block.class, src, b);
	//
	// Block[] chunkBlocks = chunk.getBlocks();
	// chunkBlocks[chunk.posToArrayIndex(src.getX(), src.getY(), src.getZ())] = src;
	// chunkBlocks[chunk.posToArrayIndex(b.getX(), b.getY(), b.getZ())] = b;
	// }
	// }
	//
	// private boolean arrayContains(Block[] blocks, int blockCount, Block block) {
	// for(int a = 0; a < blockCount; a++) {
	// if(blocks[a] == block)
	// return true;
	// }
	//
	// return false;
	// }
	//
	// private Block[] add(Block[] blocks, int i, Block block) {
	// if(i >= blocks.length) {
	// Block[] temp = Struct.emptyArray(Block.class, blocks.length * 2);
	// System.arraycopy(blocks, 0, temp, 0, blocks.length);
	// blocks = temp;
	// }
	//
	// blocks[i] = block;
	//
	// return blocks;
	// }
	//
	// private int getBlockIndex(Block b) {
	// for(int i = 0; i < compact.length; i++) {
	// if(compact[i] == b) {
	// return i;
	// }
	// }
	//
	// return -1;
	// }
	
	private boolean isBlockOnChunkSurface(Block b) {
		return b.getX() == chunk.getCornerX() || b.getX() == chunk.getCornerX() + Chunk.CHUNK_BLOCK_WIDTH - 1 ||
				b.getY() == chunk.getCornerY() || b.getY() == chunk.getCornerY() + Chunk.CHUNK_BLOCK_HEIGHT - 1 ||
				b.getZ() == chunk.getCornerZ() || b.getZ() == chunk.getCornerZ() + Chunk.CHUNK_BLOCK_DEPTH - 1;
	}
	
	private void updateCompactArray() {
		Stopwatch.start("Loop 1");
		for(int a = compact.length - 1; a >= blockCount; a--) {
			if(compact[a].getType() != BlockType.AIR && !compact[a].isSurrounded(chunk)) {
				if(a > blockCount) {
					Block src = compact[blockCount++];
					Block dest = compact[a++];
					Struct.swap(Block.class, src, dest);
					
					Block[] blocks = chunk.getBlocks();
					blocks[chunk.posToArrayIndex(src)] = src;
					blocks[chunk.posToArrayIndex(dest)] = dest;
					
					testForSurface(dest);
				} else {
					blockCount++;
				}
			}
		}
		Stopwatch.stop();
		
		Stopwatch.start("Loop 2");
		for(int a = blockCount - 1; a >= 0; a--) {
			if(compact[a].getType() == BlockType.AIR || compact[a].isSurrounded(chunk)) {
				if(a < blockCount - 1) {
					Block src = compact[--blockCount];
					Block dest = compact[a++];
					Struct.swap(Block.class, src, dest);
					
					Block[] blocks = chunk.getBlocks();
					blocks[chunk.posToArrayIndex(src.getX(), src.getY(), src.getZ())] = src;
					blocks[chunk.posToArrayIndex(dest.getX(), dest.getY(), dest.getZ())] = dest;
					
					testForSurface(dest);
				} else {
					blockCount--;
				}
			}
		}
		Stopwatch.stop();
	}
	
	private void testForSurface(Block block) {
		Chunk neighbor;
		if(block.getX() == chunk.getCornerX()) {
			neighbor = chunk.getChunkManager().getChunkContaining(chunk.getCornerX() - 1, chunk.getCornerY(), chunk.getCornerZ());
			if(neighbor != null)
				neighbor.getCallback().chunkModified(block);
		} else if(block.getX() == chunk.getCornerX() + Chunk.CHUNK_BLOCK_WIDTH - 1) {
			neighbor = chunk.getChunkManager().getChunkContaining(chunk.getCornerX() + 1, chunk.getCornerY(), chunk.getCornerZ());
			if(neighbor != null)
				neighbor.getCallback().chunkModified(block);
		} else if(block.getY() == chunk.getCornerY()) {
			neighbor = chunk.getChunkManager().getChunkContaining(chunk.getCornerX(), chunk.getCornerY() - 1, chunk.getCornerZ());
			if(neighbor != null)
				neighbor.getCallback().chunkModified(block);
		} else if(block.getY() == chunk.getCornerY() + Chunk.CHUNK_BLOCK_HEIGHT - 1) {
			neighbor = chunk.getChunkManager().getChunkContaining(chunk.getCornerX(), chunk.getCornerY() + 1, chunk.getCornerZ());
			if(neighbor != null)
				neighbor.getCallback().chunkModified(block);
		} else if(block.getZ() == chunk.getCornerZ()) {
			neighbor = chunk.getChunkManager().getChunkContaining(chunk.getCornerX(), chunk.getCornerY(), chunk.getCornerZ() - 1);
			if(neighbor != null)
				neighbor.getCallback().chunkModified(block);
		} else if(block.getZ() == chunk.getCornerZ() + Chunk.CHUNK_BLOCK_DEPTH - 1) {
			neighbor = chunk.getChunkManager().getChunkContaining(chunk.getCornerX(), chunk.getCornerY(), chunk.getCornerZ() + 1);
			if(neighbor != null)
				neighbor.getCallback().chunkModified(block);
		}
	}
	
	private void updateVBO() {
		Stopwatch.start("Update Compact Array");
		updateCompactArray();
		Stopwatch.stop();
		
		chunkModified = false;
		
		if(blockCount == 0) {
			return;
		}
		
		Stopwatch.start("UpdateVBO");
		
		final int DATA_SIZE = blockCount * Struct.sizeof(Block.class);
		
		ByteBuffer uploadBuffer = glBuffer.bind(chunkNumOffset * CHUNK_DATA_SIZE, DATA_SIZE);
		buffer.position(0).limit(DATA_SIZE);
		uploadBuffer.put(buffer);
		glBuffer.unbind();
		
		Stopwatch.stop();
	}
	
	public int getLastCubeRenderCount() {
		return blockCount;
	}
	
	public boolean render(DrawElementsIndirectCommand command) {
		if(chunkModified) {
			updateVBO();
		}
		
		if(blockCount == 0) {
			return false;
		}
		
		command.instanceCount = blockCount;
		command.baseInstance = chunkNumOffset * Chunk.TOTAL_BLOCKS;
		
		return true;
	}
}
