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

import com.jevaengine.spacestation.entity.ItemDrop;
import com.jevaengine.spacestation.ui.InventoryHudFactory;
import com.jevaengine.spacestation.ui.SimpleItemContainer;
import com.jevaengine.spacestation.ui.SimpleItemContainer.ISimpleItemContainerObserver;
import com.projectstation.client.network.WorldClient;
import com.projectstation.network.command.server.ServerWorldVisit;
import com.projectstation.network.command.world.DropInventoryItemCommand;
import com.projectstation.network.command.world.EquipItemCommand;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.character.ILoadout;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItem.IWieldTarget;
import io.github.jevaengine.rpg.item.IItemSlot;
import io.github.jevaengine.rpg.item.IItemStore;
import io.github.jevaengine.rpg.item.usr.UsrWieldTarget;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.entity.IEntity;

import java.net.URI;

public final class ClientInventoryHudFactory {
	private static final URI INVENTORY_WINDOW = URI.create("file:///ui/windows/inventory/layout.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public ClientInventoryHudFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public InventoryHudFactory.InventoryHud create(WorldClient client, ILoadout loadout, IItemStore inventory, IRpgCharacter owner) throws WindowConstructionException {
		Observers observers = new Observers();
		
		Window window = m_windowFactory.create(INVENTORY_WINDOW, new InventoryHudFactoryBehaviourInjector(client, observers, loadout, inventory, owner));
		m_windowManager.addWindow(window);

		window.center();

		return new InventoryHudFactory.InventoryHud(window, observers);
	}

	private class InventoryHudFactoryBehaviourInjector extends WindowBehaviourInjector {

		private final WorldClient m_client;
		private final MenuStrip m_menuStrip = new MenuStrip();
		private final Observers m_observers;
		private final ILoadout m_loadout;
		private final IItemStore m_inventory;
		private final IRpgCharacter m_owner;
		
		public InventoryHudFactoryBehaviourInjector(WorldClient client, final Observers observers, ILoadout loadout, IItemStore inventory, IRpgCharacter owner) {
			m_observers = observers;
			m_loadout = loadout;
			m_inventory = inventory;
			m_owner = owner;
			m_client = client;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			addControl(m_menuStrip);
			IItemSlot slots[] = m_inventory.getSlots();
			
			for(int i = 0; i < slots.length && hasControl(SimpleItemContainer.class, String.valueOf(i)); i++) {
				SimpleItemContainer container = getControl(SimpleItemContainer.class, String.valueOf(i));
				container.setSlot(slots[i]);
				Rect2D bounds = container.getBounds();
				Vector2D menuLocation = container.getLocation().add(new Vector2D(bounds.width / 2, bounds.height / 2));
				container.getObservers().add(new MoveItemToLoadout(i, slots[i], menuLocation));
			}

			getObservers().add(new Window.IWindowInputObserver() {
				@Override
				public void onKeyEvent(InputKeyEvent event) {

				}

				@Override
				public void onMouseEvent(InputMouseEvent event) {
					if(event.type != InputMouseEvent.MouseEventType.MouseClicked)
						return;

					Rect2D bounds = m_menuStrip.getBounds().add(m_menuStrip.getAbsoluteLocation());

					if(!bounds.contains(event.location))
						m_menuStrip.setVisible(false);
				}
			});
		}
		
		private class MoveItemToLoadout implements ISimpleItemContainerObserver {
			private final IItemSlot m_slot;
			private final Vector2D m_slotLocation;
			private final int m_index;
			public MoveItemToLoadout(int index, IItemSlot slot, Vector2D slotLocation) {
				m_slot = slot;
				m_slotLocation = slotLocation;
				m_index = index;
			}

			private void tryWieldItem() {
				if(m_slot.isEmpty())
					return;

				m_client.send(new ServerWorldVisit(new EquipItemCommand(m_owner.getInstanceName(), m_index)));
			}

			private void tryUseWithHandsItem() {
				IItemSlot inHands = m_loadout.getSlot(UsrWieldTarget.LeftHand);

				/*
				if(inHands.isEmpty())
					return;

				if(inHands.getItem().getFunction().testUseAbility(m_owner, new IItem.ItemTarget(m_slot.getItem()), inHands.getItem().getAttributes()).isUseable()) {
					inHands.getItem().getFunction().use(m_owner, new IItem.ItemTarget(m_slot.getItem()), inHands.getItem().getAttributes(), inHands.getItem());
				}*/
			}

			@Override
			public void selected() {
				IItemSlot inHands = m_loadout.getSlot(UsrWieldTarget.LeftHand);

				if(m_slot.isEmpty()) {
					if(!inHands.isEmpty())
						m_slot.setItem(inHands.clear());
				} else if(!inHands.isEmpty() && inHands.getItem().getFunction().testUseAbility(m_owner, new IItem.ItemTarget(m_slot.getItem()), inHands.getItem().getAttributes()).isUseable()) {
					IWieldTarget[] itemWieldTargets = m_slot.getItem().getFunction().getWieldTargets();

					if(itemWieldTargets.length == 0)
						tryUseWithHandsItem();
					else {
						m_menuStrip.setLocation(m_slotLocation);
						IItem handsItem = inHands.getItem();
						m_menuStrip.setContext(new String[] {"Wield Item", "Use with " + handsItem.getName()}, (String command) -> {
							if(command.compareTo("Wield Item") == 0) {
								tryWieldItem();
							} else
								tryUseWithHandsItem();
						});
					}
				} else {
					IWieldTarget[] itemWieldTargets = m_slot.getItem().getFunction().getWieldTargets();

					if(itemWieldTargets.length != 0)
						tryWieldItem();
				}
			}

			@Override
			public void alternateSelected() {
				if(m_slot.isEmpty() || m_owner.getWorld() == null)
					return;

				m_client.send(new ServerWorldVisit(new DropInventoryItemCommand(m_owner.getInstanceName(), m_index)));
				/*
				IItem item = m_slot.clear();
				
				IEntity itemDrop = new ItemDrop(item);
				m_owner.getWorld().addEntity(itemDrop);
				itemDrop.getBody().setLocation(m_owner.getBody().getLocation().add(new Vector3F(0, 0, -.01F)));
				*/
			}
		}
	}
}
