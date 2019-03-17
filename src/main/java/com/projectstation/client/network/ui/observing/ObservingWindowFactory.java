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
package com.projectstation.client.network.ui.observing;

import com.jevaengine.spacestation.ui.playing.CameraBehaviorInjector;
import com.jevaengine.spacestation.ui.playing.WorldInteractionBehaviorInjector;
import com.jevaengine.spacestation.ui.playing.WorldInteractionBehaviorInjector.IInteractionHandler;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.scene.camera.ControlledCamera;
import io.github.jevaengine.world.scene.camera.ICamera;

import java.net.URI;

public final class ObservingWindowFactory {

	private static final URI OBSERVING_VIEW_WINDOW = URI.create("file:///ui/windows/observing.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public ObservingWindowFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public ObservingWindow create(ControlledCamera camera) throws WindowConstructionException {
			Observers observers = new Observers();
		
		Window window = m_windowFactory.create(OBSERVING_VIEW_WINDOW);
		m_windowManager.addWindow(window);

		try {
			new FreeCameraBehaviorInjector(camera).inject(window);
		} catch (NoSuchControlException ex) {
			throw new WindowConstructionException(OBSERVING_VIEW_WINDOW, ex);
		}

		window.center();

		return new ObservingWindow(window, observers);
	}

	public static class ObservingWindow implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;

		private ObservingWindow(Window window, Observers observers) {
			m_window = window;
			m_observers = observers;
		}

		@Override
		public void dispose() {
			m_window.dispose();
		}

		public Observers getObservers() {
			return m_observers;
		}
		
		public void setVisible(boolean isVisible) {
			m_window.setVisible(isVisible);
		}

		public boolean isVisible() {
			return m_window.isVisible();
		}

		public void setLocation(Vector2D location) {
			m_window.setLocation(location);
		}
		
		public Vector2D getLocation() {
			return m_window.getLocation();
		}

		public void center() {
			m_window.center();
		}

		public void focus() {
			m_window.focus();
		}

		public void setMovable(boolean isMovable) {
			m_window.setMovable(false);
		}

		public void setTopMost(boolean isTopMost) {
			m_window.setTopMost(isTopMost);
		}

		public Rect2D getBounds() {
			return m_window.getBounds();
		}
	}
}
