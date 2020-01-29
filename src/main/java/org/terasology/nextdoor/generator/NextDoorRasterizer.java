/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.nextdoor.generator;

import org.terasology.math.ChunkMath;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.chunks.CoreChunk;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldRasterizer;

public class NextDoorRasterizer implements WorldRasterizer {
    private Block deadGrass;
    private Block brownLand;
    private Block lamp;
    private Block lava;
    private Block light;
    private Block blackTree;
    private Block skeleton;
    private Block air;

    @Override
    public void initialize() {
        deadGrass = CoreRegistry.get(BlockManager.class).getBlock("CoreBlocks:deadBush");
        brownLand= CoreRegistry.get(BlockManager.class).getBlock("NextDoor:ground");
        lamp= CoreRegistry.get(BlockManager.class).getBlock("NextDoor:lampPost");
        lava= CoreRegistry.get(BlockManager.class).getBlock("CoreBlocks:Lava");
        light= CoreRegistry.get(BlockManager.class).getBlock("NextDoor:lightLamp");
        blackTree= CoreRegistry.get(BlockManager.class).getBlock("NextDoor:hauntedTree");
        skeleton= CoreRegistry.get(BlockManager.class).getBlock("NextDoor:skeleton");
        air= CoreRegistry.get(BlockManager.class).getBlock("engine:air");
    }

    @Override
    public void generateChunk(CoreChunk chunk, Region chunkRegion) {
        NextDoorFacet surfaceFacet = chunkRegion.getFacet(NextDoorFacet.class);
        if (chunkRegion.getRegion().maxY() > 10000 && chunkRegion.getRegion().minY()<10000) {
            for (Vector3i position : chunkRegion.getRegion()) {
                position = new Vector3i(position.x, position.y, position.z);
                float surfaceHeight = surfaceFacet.getWorld(position.x, position.z);
                surfaceHeight = ChunkMath.calcBlockPosY((int) surfaceHeight);
                position.y = (int)surfaceHeight;

                //main land
                chunk.setBlock(ChunkMath.calcBlockPos(position.x, position.y - 1, position.z), brownLand);
                chunk.setBlock(ChunkMath.calcBlockPos(position.x, position.y + 80, position.z), brownLand);

                //random ghosts and lamp will be created
                java.util.Random rand = new java.util.Random();
                java.util.Random rand2 = new java.util.Random();
                int randomGrass = rand.nextInt(10000);
                int randomLamp = rand.nextInt(20);
                if(randomGrass > 700 && randomGrass < 724)
                {
                    chunk.setBlock(ChunkMath.calcBlockPos(position.x,position.y,position.z), deadGrass);
                }
                else if(randomGrass == 100 && randomLamp == 7)
                {
                    for(int x=-1;x<2;x++)
                    {
                        for(int y=1;y<6;y++) {
                            for (int z = -1; z < 2; z++) {
                                chunk.setBlock(ChunkMath.calcBlockPos(position.x + x, position.y + y, position.z + z), air);
                            }
                        }
                    }
                    chunk.setBlock(ChunkMath.calcBlockPos(position.x,position.y,position.z), lamp);
                }
                else if(randomGrass == 8960 )
                {
                    for(int x=-3;x<4;x++)
                    {
                        for(int y=1;y<8;y++) {
                            for (int z = -3; z < 6; z++) {
                                chunk.setBlock(ChunkMath.calcBlockPos(position.x + x, position.y + y, position.z + z), air);
                            }
                        }
                    }
                    chunk.setBlock(ChunkMath.calcBlockPos(position.x,position.y,position.z), blackTree);
                    chunk.setBlock(ChunkMath.calcBlockPos(position.x,position.y+7,position.z), light);
                }
                else if(randomGrass == 4210 )
                {
                    for(int x=-3;x<4;x++)
                    {
                        for(int y=1;y<8;y++) {
                            for (int z = -3; z < 6; z++) {
                                chunk.setBlock(ChunkMath.calcBlockPos(position.x + x, position.y + y, position.z + z), air);
                            }
                        }
                    }
                    chunk.setBlock(ChunkMath.calcBlockPos(position.x,position.y,position.z), blackTree);
                    chunk.setBlock(ChunkMath.calcBlockPos(position.x,position.y+7,position.z), light);
                }
                else if(randomGrass == 6110 && randomLamp == 13 )
                {
                    for(int x=-4;x<5;x++)
                    {
                        for(int y=1;y<10;y++) {
                            for (int z = -3; z < 6; z++) {
                                chunk.setBlock(ChunkMath.calcBlockPos(position.x + x, position.y + y, position.z + z), air);
                            }
                        }
                    }
                    chunk.setBlock(ChunkMath.calcBlockPos(position.x,position.y,position.z-2), skeleton);
                    for(int i=0;i<8;i++) {
                        chunk.setBlock(ChunkMath.calcBlockPos(position.x-4, position.y + i , position.z), lava);
                        chunk.setBlock(ChunkMath.calcBlockPos(position.x+4, position.y + i , position.z), lava);
                    }
                    for(int i=-4;i<5;i++) {
                        chunk.setBlock(ChunkMath.calcBlockPos(position.x+i, position.y + 7 , position.z), lava);
                    }
                }
            }
        }
    }
}