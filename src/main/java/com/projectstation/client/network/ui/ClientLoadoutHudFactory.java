/* 
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.projectstation.client.network.ui;

import com.jevaengine.spacestation.item.SpaceCharacterWieldTarget;
import com.jevaengine.spacestation.ui.LoadoutHudFactory;
import com.jevaengine.spacestation.ui.SimpleItemContainer;
import com.jevaengine.spacestation.ui.SimpleItemContainer.ISimpleItemContainerObserver;
import com.projectstation.client.network.WorldClient;
import com.projectstation.network.command.server.ServerWorldVisit;
import com.projectstation.network.command.world.UnequipItemCommand;
import io.github.jevaengine.rpg.entity.character.ILoadout;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem.IWieldTarget;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Observers;

import java.net.URI;

public final class ClientLoadoutHudFactory {

	private static final URI LOADOUT_WINDOW = URI.create("file:///ui/windows/loadout/layout.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public ClientLoadoutHudFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public LoadoutHudFactory.LoadoutHud create(WorldClient client, IRpgCharacter character) throws WindowConstructionException {
		Observers observers = new Observers();
		
		Window window = m_windowFactory.create(LOADOUT_WINDOW, new LoadoutFactoryBehaviourInjector(client, observers, character));
		m_windowManager.addWindow(window);

		window.center();

		return new LoadoutHudFactory.LoadoutHud(window, observers);
	}

	private class LoadoutFactoryBehaviourInjector extends WindowBehaviourInjector {

		private final Observers m_observers;
		private final WorldClient m_client;
		private final IRpgCharacter m_owner;
		
		public LoadoutFactoryBehaviourInjector(WorldClient client, final Observers observers, final IRpgCharacter owner) {
			m_observers = observers;
			m_client = client;
			m_owner = owner;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final SimpleItemContainer uniform = getControl(SimpleItemContainer.class, "uniform");
			final SimpleItemContainer shoes = getControl(SimpleItemContainer.class, "shoes");
			final SimpleItemContainer gloves = getControl(SimpleItemContainer.class, "gloves");
			final SimpleItemContainer glasses = getControl(SimpleItemContainer.class, "glasses");
			final SimpleItemContainer ears = getControl(SimpleItemContainer.class, "ears");
			final SimpleItemContainer head = getControl(SimpleItemContainer.class, "head");

			ILoadout loadout = m_owner.getLoadout();
			uniform.setSlot(loadout.getSlot(SpaceCharacterWieldTarget.Uniform));
			shoes.setSlot(loadout.getSlot(SpaceCharacterWieldTarget.Feet));
			gloves.setSlot(loadout.getSlot(SpaceCharacterWieldTarget.Hands));
			glasses.setSlot(loadout.getSlot(SpaceCharacterWieldTarget.Eyes));
			ears.setSlot(loadout.getSlot(SpaceCharacterWieldTarget.Ears));
			head.setSlot(loadout.getSlot(SpaceCharacterWieldTarget.Mask));
		
			uniform.getObservers().add(new MoveToInventoryObserver(SpaceCharacterWieldTarget.Uniform));
			shoes.getObservers().add(new MoveToInventoryObserver(SpaceCharacterWieldTarget.Feet));
			gloves.getObservers().add(new MoveToInventoryObserver(SpaceCharacterWieldTarget.Hands));
			glasses.getObservers().add(new MoveToInventoryObserver(SpaceCharacterWieldTarget.Eyes));
			ears.getObservers().add(new MoveToInventoryObserver(SpaceCharacterWieldTarget.Ears));
			head.getObservers().add(new MoveToInventoryObserver(SpaceCharacterWieldTarget.Mask));
		}
		
		private class MoveToInventoryObserver implements ISimpleItemContainerObserver {
			private final IWieldTarget m_wieldTarget;
			
			public MoveToInventoryObserver(IWieldTarget wieldTarget) {
				m_wieldTarget = wieldTarget;
			}
			
			@Override
			public void selected() {
				m_client.send(new ServerWorldVisit(new UnequipItemCommand(m_owner.getInstanceName(), m_wieldTarget)));
			}

			@Override
			public void alternateSelected() { }
		}
	}
}
