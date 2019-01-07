package com.projectstation.client.network.entity;

import com.projectstation.network.IServerVisit;
import com.projectstation.network.WorldVisit;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.EntityNetworkAdapterException;
import com.projectstation.network.entity.IEntityNetworkAdapter;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.util.ArrayList;
import java.util.List;

public class SimpleEntityNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IClientEntityNetworkAdapter, IEntity> {

    @Override
    public IClientEntityNetworkAdapter create(IEntity e, EntityConfigurationDetails config, IEntityNetworlAdapterHost pr) {
        return new SimpleEntityNetworkAdapter(e, config, pr);
    }
}

class SimpleEntityNetworkAdapter implements IClientEntityNetworkAdapter {
    private final IEntity entity;
    private final EntityConfigurationDetails config;

    public SimpleEntityNetworkAdapter(IEntity entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost pr) {
        this.entity = entity;
        this.config = config;
    }

    @Override
    public List<WorldVisit> createInitializeSteps() throws EntityNetworkAdapterException {
        return new ArrayList<>();
    }

    @Override
    public List<IServerVisit> poll(int deltaTime) {
        return new ArrayList<>();
    }

    @Override
    public void setSteeringBehaviour(ISteeringBehavior behaviour) {

    }

    @Override
    public void setSpeed(float speed) {

    }

    @Override
    public boolean isOwner() {
        return false;
    }
}
