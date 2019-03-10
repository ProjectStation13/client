package com.projectstation.client.network.entity;

import com.jevaengine.spacestation.entity.power.Dcpu;
import com.projectstation.network.IServerVisit;
import com.projectstation.network.command.server.ServerWorldVisit;
import com.projectstation.network.command.world.SetEntityVelocityCommand;
import com.projectstation.network.command.world.SimulateKeyboardKeysCommand;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.IClientEntityNetworkAdapter;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.character.IMovementResolver;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DcpuNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IClientEntityNetworkAdapter, Dcpu> {

    @Override
    public IClientEntityNetworkAdapter create(Dcpu e, EntityConfigurationDetails config, IEntityNetworkAdapterHost pr) {
        return new DcpuNetworkAdapter(e, config, pr);
    }
}

class DcpuNetworkAdapter implements IClientEntityNetworkAdapter {
    private final Dcpu entity;
    private boolean locationChanged = true;
    private final EntityConfigurationDetails config;
    private final IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost host;
    private final List<SimulateKeyboardKeysCommand.SimulatedKey> simuatedKeys = new ArrayList<>();

    private static final byte[] HAMG_FIRMWARE = {
        ((byte)0x16), ((byte)0x00), ((byte)0x84), ((byte)0xb2), ((byte)0x7f), ((byte)0x81), ((byte)0x00), ((byte)0x1d), ((byte)0x88), ((byte)0xa3), ((byte)0x16), ((byte)0x20), ((byte)0x7c), ((byte)0x12), ((byte)0xf6), ((byte)0x15), ((byte)0x7c), ((byte)0x32), ((byte)0x73), ((byte)0x49), ((byte)0x7f), ((byte)0x81), ((byte)0x00), ((byte)0x0e), ((byte)0x7f), ((byte)0x81), ((byte)0x00), ((byte)0x01), ((byte)0x84), ((byte)0x01), ((byte)0x7c), ((byte)0x21), ((byte)0x80), ((byte)0x00), ((byte)0x16), ((byte)0x40), ((byte)0x7c), ((byte)0xc1), ((byte)0x00), ((byte)0x1f), ((byte)0x04), ((byte)0xe1), ((byte)0x85), ((byte)0xd2), ((byte)0x7f), ((byte)0x81), ((byte)0x00), ((byte)0x1d), ((byte)0x39), ((byte)0xe1), ((byte)0x7d), ((byte)0xeb), ((byte)0x70), ((byte)0x00), ((byte)0x7f), ((byte)0x9e), ((byte)0x00), ((byte)0x15), ((byte)0x7f), ((byte)0x81), ((byte)0x00), ((byte)0x1d), ((byte)0x00), ((byte)0x00),
    };

    public DcpuNetworkAdapter(Dcpu entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost pr) {
        this.host = pr;
        this.entity = entity;
        this.config = config;
        entity.loadFirmware(HAMG_FIRMWARE);
        //We need to give firmware some time to run to init vram
        entity.update(5000);
        entity.getObservers().add(new Dcpu.IDcpuObserver() {
            @Override
            public void screenChanged(HashMap<Integer, Integer> vramDelta, HashMap<Integer, Integer> paletteDelta, HashMap<Integer, Integer> fontDelta) {

            }

            @Override
            public void keySimulated(int keyCode, char keyChar) {
                simuatedKeys.add(new SimulateKeyboardKeysCommand.SimulatedKey(keyCode, keyChar));
                host.poll();
            }
        });
    }

    @Override
    public List<IServerVisit> poll(int deltaTime) {
        ArrayList<IServerVisit> response = new ArrayList<>();

        if(!simuatedKeys.isEmpty())
        {
            response.add(new ServerWorldVisit(new SimulateKeyboardKeysCommand(entity.getInstanceName(), simuatedKeys)));
            simuatedKeys.clear();
        }
        return response;
    }

    @Override
    public void setSteeringBehaviour(ISteeringBehavior behaviour) {
    }

    @Override
    public void setSpeed(float speed) {
    }

    public void resetSpeed() {

    }

    @Override
    public boolean isOwner() {
        return host.isOwner();
    }
}
