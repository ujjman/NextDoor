/*
 * Copyright 2016 MovingBlocks
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
package org.terasology.nextdoor.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.management.AssetManager;
import org.terasology.audio.AudioManager;
import org.terasology.audio.events.PlaySoundEvent;
import org.terasology.behaviors.components.NPCMovementComponent;
import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.characters.CharacterTeleportEvent;
import org.terasology.logic.chat.ChatMessageEvent;
import org.terasology.logic.health.BeforeDestroyEvent;
import org.terasology.logic.health.event.RestoreFullHealthEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.Direction;
import org.terasology.math.Region3i;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.BaseVector2i;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.nextdoor.generator.NextDoorFacet;
import org.terasology.nextdoor.spawnGhost.GhostComponent;
import org.terasology.nextdoor.spawnGhost.GhostsQuestComponent;
import org.terasology.registry.In;
import org.terasology.utilities.Assets;
import org.terasology.world.WorldProvider;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.World;
import org.terasology.world.generator.WorldGenerator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

@RegisterSystem(RegisterMode.CLIENT)
public class NextDoorClientSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final Logger logger = LoggerFactory.getLogger(NextDoorClientSystem.class);
    @In
    private WorldProvider worldProvider;
    @In
    private LocalPlayer localPlayer;
    @In
    private WorldGenerator worldGenerator;
    @In
    private InventoryManager inventoryManager;
    @In
    private AudioManager audioManager;
    private static final float MAX_GHOST_SPAWN_OFFSET_ANGLE = (float) (Math.PI / 8.0f);
    private static final float MIN_GHOST_SPAWN_DISTANCE = 3;
    private static final float MAX_GHOST_SPAWN_DISTANCE = 5;
    private static final float SECONDS_BETWEEN_QUESTS = 3;
    private static boolean checkDeath=false;
    @In
    private EntityManager entityManager;
    @In
    private AssetManager assetManager;
    private Random random = new Random();
    private EntityRef questToSpawnGhostFor = EntityRef.NULL;
    private float nextQuestCooldown;
    private Map<EntityRef, Vector3f> teleportQueue = new HashMap<>();

    @Override
    public void initialise() {
        nextQuestCooldown = 10;
    }

    @Override
    public void update(float delta) {
        if (!teleportQueue.isEmpty()) {
            Iterator<Map.Entry<EntityRef, Vector3f>> teleportIt = teleportQueue.entrySet().iterator();
            while (teleportIt.hasNext()) {
                Map.Entry<EntityRef, Vector3f> entry = teleportIt.next();
                EntityRef character = entry.getKey();
                Vector3f targetPos = entry.getValue();
                character.send(new CharacterTeleportEvent(targetPos));
                teleportIt.remove();
            }
        }
        nextQuestCooldown -= delta;
        if (nextQuestCooldown <= 0) {
            nextQuestCooldown = 30;
            if(checkDeath) {
                tryToSpawnGhost();
            }
            return;
        }
    }

    @ReceiveEvent
    public void onEnterNextDoor(EnterNextDoorEvent event, EntityRef entity) {
        entity.send(new PlaySoundEvent(Assets.getSound("NextDoor:horrorMusic").get(), 0.7f));
    }

    @ReceiveEvent(priority = EventPriority.PRIORITY_HIGH)
    public void onDeath(BeforeDestroyEvent event, EntityRef entity, CharacterComponent characterComponent, LocationComponent locationComponent) {
        logger.info("death comp");
        checkDeath=true;
        EntityRef character = localPlayer.getCharacterEntity();
        EntityRef client = localPlayer.getClientEntity();
        EntityRef item = EntityRef.NULL;
        boolean resurrect = false;
        for (int i = 0; i < InventoryUtils.getSlotCount(character); i++) {
            if (InventoryUtils.getItemAt(character, i).hasComponent(NextDoorResurrectComponent.class)) {
                resurrect = true;
                item = InventoryUtils.getItemAt(character, i);
                break;
            }
        }
        if (resurrect) {
            event.consume();
            World world = worldGenerator.getWorld();
            Vector3f spawnPos = findPosition(locationComponent, world);
            if (spawnPos != null) {
                inventoryManager.removeItem(entity, entity, item, true);
                character.send(new RestoreFullHealthEvent(character));
                character.send(new EnterNextDoorEvent(client));
                teleportQueue.put(character, spawnPos);
            }
        }

    }

    //got some help here from ratmolerat's upperworld
    public Vector3f findPosition(LocationComponent locationComponent, World world) {
        Vector3i search = new Vector3i(100, 11000, 100);
        Vector3f center = locationComponent.getWorldPosition();
        Region3i area = Region3i.createFromCenterExtents(new Vector3i(center.x, center.y+200, center.z), search);
        Region worldRegion = world.getWorldData(area);
        logger.info(worldRegion.toString());
        for (int i=0; i<world.getAllFacets().toArray().length; i++) {
            logger.info(world.getAllFacets().toArray()[i].toString());
        }
        NextDoorFacet facet = worldRegion.getFacet(NextDoorFacet.class);
        if (facet != null) {
            logger.info("not null!");
            for (BaseVector2i pos : facet.getWorldRegion().contents()) {
                float surfaceHeight = facet.getWorld(pos);
                logger.info("surfaceheight: " + surfaceHeight);
                if (surfaceHeight > 9990) {
                    return new Vector3f(pos.x(), surfaceHeight + 2, pos.y());
                }
            }
        }
        return null;
    }

    private void tryToSpawnGhost() {
        int ghostCount = entityManager.getCountOfEntitiesWith(GhostComponent.class);
        if (ghostCount > 0) {
            for (EntityRef existingGhost : entityManager.getEntitiesWith(GhostComponent.class)) {
                existingGhost.destroy();
            }
        }
        EntityRef character = localPlayer.getCharacterEntity();
        if (character == null || !character.isActive()) {
            return;
        }
        LocationComponent characterLocation = character.getComponent(LocationComponent.class);
        if (characterLocation == null) {
            return;
        }
        Vector3f spawnPos = tryFindingGhostSpawnLocationInfrontOfCharacter(character);
        if (spawnPos == null) {
            return;
        }
        Vector3f spawnPosToCharacter = characterLocation.getWorldPosition().sub(spawnPos);
        Quat4f rotation = distanceDeltaToYAxisRotation(spawnPosToCharacter);
        Prefab ghostPrefab = assetManager.getAsset("NextDoor:femaleHuman", Prefab.class).get();
        EntityBuilder entityBuilder = entityManager.newBuilder(ghostPrefab);
        LocationComponent locationComponent = entityBuilder.getComponent(LocationComponent.class);
        locationComponent.setWorldPosition(spawnPos);
        locationComponent.setWorldRotation(rotation);
        NPCMovementComponent movementComponent = entityBuilder.getComponent(NPCMovementComponent.class);
        float yaw = (float) Math.atan2(spawnPosToCharacter.x, spawnPosToCharacter.z);
        movementComponent.yaw = 180f + yaw * TeraMath.RAD_TO_DEG;
        entityBuilder.addOrSaveComponent(movementComponent);
        GhostComponent ghostComponent = new GhostComponent();
        entityBuilder.addOrSaveComponent(ghostComponent);
        EntityRef ghost = entityBuilder.build();
        GhostsQuestComponent ghostsQuestComponent = questToSpawnGhostFor.getComponent(GhostsQuestComponent.class);
        if (ghostsQuestComponent == null) {
            ghostsQuestComponent = new GhostsQuestComponent();
        }
        character.getOwner().send(new ChatMessageEvent(ghostsQuestComponent.greetingText, ghost));
        questToSpawnGhostFor = EntityRef.NULL;
    }

    private Quat4f distanceDeltaToYAxisRotation(Vector3f direction) {
        direction.y = 0;
        if (direction.lengthSquared() > 0.001f) {
            direction.normalize();
        } else {
            direction.set(Direction.FORWARD.getVector3f());
        }
        return Quat4f.shortestArcQuat(Direction.FORWARD.getVector3f(), direction);
    }

    private Vector3f tryFindingGhostSpawnLocationInfrontOfCharacter(EntityRef character) {
        LocationComponent characterLocation = character.getComponent(LocationComponent.class);
        Vector3f spawnPos = locationInfrontOf(characterLocation, MIN_GHOST_SPAWN_DISTANCE, MAX_GHOST_SPAWN_DISTANCE,
                MAX_GHOST_SPAWN_OFFSET_ANGLE);
        return spawnPos;
    }

    private Vector3f locationInfrontOf(LocationComponent location, float minDistance, float maxDistance, float maxAngle) {
        Vector3f result = location.getWorldPosition();
        Vector3f offset = new Vector3f(location.getWorldDirection());
        Quat4f randomRot = randomYAxisRotation(maxAngle);
        offset = randomRot.rotate(offset);
        float distanceRangeDelta = maxDistance - minDistance;
        float randomDistance = minDistance + random.nextFloat() * distanceRangeDelta;
        offset.scale(randomDistance);
        result.add(offset);
        return result;
    }

    private Quat4f randomYAxisRotation(float maxAngle) {
        float randomAngle = random.nextFloat() * maxAngle;
        // chance to have a rotation in other diration:
        if (random.nextBoolean()) {
            randomAngle = ((float) Math.PI * 2) - randomAngle;
        }
        return new Quat4f(new Vector3f(0, 1, 0), randomAngle);
    }
}
