package com.ra4king.fps.renderers;

import static org.lwjgl.opengl.GL32.*;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GLSync;

import com.ra4king.fps.renderers.WorldRenderer.DrawElementsIndirectCommand;
import com.ra4king.fps.world.Chunk;
import com.ra4king.fps.world.Chunk.Block;
import com.ra4king.fps.world.Chunk.BlockType;
import com.ra4king.fps.world.Chunk.ChunkModifiedCallback;
import com.ra4king.opengl.util.Stopwatch;
import com.ra4king.opengl.util.Utils;
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
	
	public static final int CHUNK_DATA_SIZE = Chunk.TOTAL_CUBES * Struct.sizeof(Block.class);
	
	public ChunkRenderer(Chunk chunk, GLBuffer glBuffer, int chunkNumOffset) {
		this.chunk = chunk;
		this.glBuffer = glBuffer;
		this.chunkNumOffset = chunkNumOffset;
		
		buffer = BufferUtils.createByteBuffer(CHUNK_DATA_SIZE);
		compact = Struct.map(Block.class, buffer);
		
		for(Block b : compact)
			b.setType(BlockType.AIR);
		
		chunk.setCallback(this);
	}
	
	public Chunk getChunk() {
		return chunk;
	}
	
	@Override
	@TakeStruct
	public Block chunkAdd(int x, int y, int z, BlockType type) {
		return compact[blockCount++].init(chunk, x, y, z, type);
	}
	
	@Override
	public void chunkModified() {
		chunkModified = true;
	}
	
	private void updateCompactArray() {
		for(int a = compact.length - 1; a >= blockCount; a--) {
			if(compact[a].getType() != BlockType.AIR || !compact[a].isSurrounded(chunk)) {
				if(a > blockCount) {
					Block src = compact[blockCount++];
					Block dest = compact[a++];
					Struct.swap(Block.class, src, dest);
					
					Block[] blocks = chunk.getBlocks();
					blocks[chunk.posToArrayIndex(src.getX() % Chunk.CHUNK_CUBE_WIDTH, src.getY() % Chunk.CHUNK_CUBE_HEIGHT, src.getZ() % Chunk.CHUNK_CUBE_DEPTH)] = src;
					blocks[chunk.posToArrayIndex(dest.getX() % Chunk.CHUNK_CUBE_WIDTH, dest.getY() % Chunk.CHUNK_CUBE_HEIGHT, dest.getZ() % Chunk.CHUNK_CUBE_DEPTH)] = dest;
				}
				else
					blockCount++;
			}
		}
		
		for(int a = blockCount - 1; a >= 0; a--) {
			if(compact[a].getType() == BlockType.AIR || compact[a].isSurrounded(chunk)) {
				if(a < blockCount - 1) {
					Block src = compact[--blockCount];
					Block dest = compact[a++];
					Struct.swap(Block.class, src, dest);
					
					Block[] blocks = chunk.getBlocks();
					blocks[chunk.posToArrayIndex(src.getX() % Chunk.CHUNK_CUBE_WIDTH, src.getY() % Chunk.CHUNK_CUBE_HEIGHT, src.getZ() % Chunk.CHUNK_CUBE_DEPTH)] = src;
					blocks[chunk.posToArrayIndex(dest.getX() % Chunk.CHUNK_CUBE_WIDTH, dest.getY() % Chunk.CHUNK_CUBE_HEIGHT, dest.getZ() % Chunk.CHUNK_CUBE_DEPTH)] = dest;
				}
				else
					blockCount--;
			}
		}
	}
	
	private void updateVBO(GLSync memoryFence) {
		Stopwatch.start("Update Compact Array");
		updateCompactArray();
		Stopwatch.stop();
		
		chunkModified = false;
		
		if(blockCount == 0)
			return;
		
		Stopwatch.start("UpdateVBO");
		
		final int DATA_SIZE = blockCount * Struct.sizeof(Block.class);
		
		if(memoryFence != null) {
			Stopwatch.start("ClientWaitSync");
			int status = glClientWaitSync(memoryFence, GL_SYNC_FLUSH_COMMANDS_BIT, 0);
			Stopwatch.stop();
			
			switch(status) {
				case GL_ALREADY_SIGNALED:
				case GL_TIMEOUT_EXPIRED:
				case GL_CONDITION_SATISFIED:
					break;
				case GL_WAIT_FAILED:
					Utils.checkGLError("ClientWaitSync");
			}
		}
		
		ByteBuffer uploadBuffer = glBuffer.bind(chunkNumOffset * CHUNK_DATA_SIZE, DATA_SIZE);
		buffer.position(0).limit(DATA_SIZE);
		uploadBuffer.put(buffer);
		glBuffer.unbind();
		
		Stopwatch.stop();
	}
	
	public int getLastCubeRenderCount() {
		return blockCount;
	}
	
	public boolean render(DrawElementsIndirectCommand command, GLSync memoryFence) {
		if(chunkModified) {
			updateVBO(memoryFence);
		}
		
		if(blockCount == 0)
			return false;
		
		command.instanceCount = blockCount;
		command.baseInstance = chunkNumOffset * Chunk.TOTAL_CUBES;
		
		return true;
	}
}
