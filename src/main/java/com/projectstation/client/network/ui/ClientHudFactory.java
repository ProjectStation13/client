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
import com.jevaengine.spacestation.ui.HudFactory;
import com.jevaengine.spacestation.ui.SimpleItemContainer;
import com.jevaengine.spacestation.ui.ToggleIcon;
import com.projectstation.client.network.WorldClient;
import com.projectstation.network.command.server.ServerWorldVisit;
import com.projectstation.network.command.world.UnequipItemCommand;
import com.projectstation.network.command.world.UseItemInHandsCommand;
import io.github.jevaengine.rpg.entity.character.ILoadout;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemSlot;
import io.github.jevaengine.rpg.item.IItemStore;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Observers;

import java.net.URI;

public final class ClientHudFactory {

	private static final URI HUD_WINDOW = URI.create("file:///ui/windows/hud/layout.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public ClientHudFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public HudFactory.Hud create(WorldClient client, IRpgCharacter owner, IItemStore invetory, ILoadout loadout) throws WindowConstructionException {
		Observers observers = new Observers();
		
		Window window = m_windowFactory.create(HUD_WINDOW, new HudBehaviourInjector(client, observers, loadout, invetory, owner));
		m_windowManager.addWindow(window);

		window.center();

		return new HudFactory.Hud(window, observers);
	}

	private class HudBehaviourInjector extends WindowBehaviourInjector {

		private final WorldClient m_client;
		private final ILoadout m_loadout;
		private final IItemStore m_inventory;
		private final IRpgCharacter m_owner;

		private final Observers m_observers;

		public HudBehaviourInjector(WorldClient client, final Observers observers, final ILoadout loadout, final IItemStore inventory, final IRpgCharacter owner) {
			m_observers = observers;
			m_inventory = inventory;
			m_loadout = loadout;
			m_owner = owner;
			m_client = client;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final ToggleIcon toggleInventory = getControl(ToggleIcon.class, "toggleInventory");

			final SimpleItemContainer hand = getControl(SimpleItemContainer.class, "toggleHand");

			IItemSlot handSlot = m_loadout.getSlot(SpaceCharacterWieldTarget.LeftHand);
			hand.setSlot(handSlot);
			hand.getObservers().add(new HandUse(handSlot, SpaceCharacterWieldTarget.LeftHand));
			toggleInventory.getObservers().add(new ToggleIcon.IToggleIconObserver() {
				@Override
				public void toggled() {
					m_observers.raise(HudFactory.IHudObserver.class).inventoryViewChanged(toggleInventory.isActive());
				}
			});
		}


		private class HandUse implements SimpleItemContainer.ISimpleItemContainerObserver {
			private final IItemSlot m_slot;
			private final IItem.IWieldTarget m_wieldTarget;

			public HandUse(IItemSlot slot, IItem.IWieldTarget wieldTarget) {
				m_slot = slot;
				m_wieldTarget = wieldTarget;
			}

			private void tryUseItem() {
				IItem item = m_slot.getItem();
				IItem.ItemUseAbilityTestResults result = item.getFunction().testUseAbility(m_owner, new IItem.ItemTarget(m_owner), item.getAttributes());

				if (result.isUseable()) {
					m_client.send(new ServerWorldVisit(new UseItemInHandsCommand(m_owner.getInstanceName())));
				}
			}


			@Override
			public void selected() {
				if (m_slot.isEmpty())
					return;

				tryUseItem();
			}

			@Override
			public void alternateSelected() {
				if (m_slot.isEmpty() || m_owner.getWorld() == null || m_inventory.isFull())
					return;

				m_client.send(new ServerWorldVisit(new UnequipItemCommand(m_owner.getInstanceName(), m_wieldTarget)));
			}
		}
	}
}
