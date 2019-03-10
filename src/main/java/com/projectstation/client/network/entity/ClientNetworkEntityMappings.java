package com.projectstation.client.network.entity;

import com.jevaengine.spacestation.entity.Infrastructure;
import com.jevaengine.spacestation.entity.ItemDrop;
import com.jevaengine.spacestation.entity.character.SpaceCharacter;
import com.jevaengine.spacestation.entity.network.NetworkWire;
import com.jevaengine.spacestation.entity.power.Dcpu;
import com.jevaengine.spacestation.entity.power.PowerWire;
import com.projectstation.network.entity.EntityNetworkAdapterMapping;
import com.projectstation.network.entity.IClientEntityNetworkAdapter;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.rpg.entity.character.DefaultRpgCharacter;
import io.github.jevaengine.world.entity.SceneArtifact;

public class ClientNetworkEntityMappings extends EntityNetworkAdapterMapping<IClientEntityNetworkAdapter> {
    public ClientNetworkEntityMappings() {
        register(Infrastructure.class, new SimpleEntityNetworkAdapterFactory());
        register(SpaceCharacter.class, new CharacterNetworkAdapterFactory());
        register(Door.class, new SimpleEntityNetworkAdapterFactory());
        /*
        register(ItemDrop.class, new SimpleEntityNetworkAdapterFactory());

        register(NetworkWire.class, new SimpleEntityNetworkAdapterFactory());
        register(PowerWire.class, new SimpleEntityNetworkAdapterFactory());*/
        register(SceneArtifact.class, new SimpleEntityNetworkAdapterFactory());
        register(Dcpu.class, new DcpuNetworkAdapterFactory());
    }
}
