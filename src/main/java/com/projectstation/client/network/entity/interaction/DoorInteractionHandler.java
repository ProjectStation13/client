package com.projectstation.client.network.entity.interaction;

import com.jevaengine.spacestation.entity.InteractableDoor;
import com.jevaengine.spacestation.ui.playing.WorldInteractionBehaviorInjector;
import com.projectstation.client.network.WorldClient;
import com.projectstation.network.command.server.ServerWorldVisit;
import com.projectstation.network.command.world.InteractDoorCommand;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.world.entity.IEntity;

public class DoorInteractionHandler implements WorldInteractionBehaviorInjector.IInteractionHandler {
    private final WorldClient client;

    public DoorInteractionHandler(WorldClient client) {
        this.client = client;
    }

    @Override
    public Class<?> getHandleSubject() {
        return InteractableDoor.class;
    }

    @Override
    public void handle(IEntity subject, boolean isSecondary, float interactionReach) {
        this.client.send(new ServerWorldVisit(new InteractDoorCommand(subject.getInstanceName())));
    }

    @Override
    public IEntity getActiveInteraction() {
        return null;
    }

    @Override
    public void outOfReach() {

    }
}
