package com.projectstation.client.network.entity;

import com.jevaengine.spacestation.entity.Infrastructure;
import com.projectstation.network.entity.EntityNetworkAdapterMapping;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.rpg.entity.character.DefaultRpgCharacter;

public class ClientNetworkEntityMappings extends EntityNetworkAdapterMapping<IClientEntityNetworkAdapter> {
    public ClientNetworkEntityMappings() {
        register(Infrastructure.class, new SimpleEntityNetworkAdapterFactory());
        register(DefaultRpgCharacter.class, new CharacterNetworkAdapterFactory());
        register(Door.class, new SimpleEntityNetworkAdapterFactory());
    }
}
