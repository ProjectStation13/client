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

import com.jevaengine.spacestation.gas.GasSimulationEntity;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import io.github.jevaengine.IDisposable;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.Label;
import io.github.jevaengine.ui.TextArea;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.entity.IEntity;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.net.URI;
import java.util.ArrayList;

public final class ChatHudFactory {

	private static final URI HUD_WINDOW = URI.create("file:///ui/windows/chathud/layout.jwl");

	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;

	public ChatHudFactory(WindowManager windowManager, IWindowFactory windowFactory) {
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}

	public ChatHud create() throws WindowConstructionException {
		Observers observers = new Observers();
		ArrayList<String> messageQueue = new ArrayList<>();
		Window window = m_windowFactory.create(HUD_WINDOW, new ChatHudBehaviorInjector(observers, messageQueue));
		m_windowManager.addWindow(window);

		window.center();

		return new ChatHud(window, observers, messageQueue);
	}

	public static class ChatHud implements IDisposable {

		private final Window m_window;
		private final Observers m_observers;
		private final ArrayList<String> m_messageQueue;

		private ChatHud (Window window, Observers observers, ArrayList<String> messageQueue) {
			m_window = window;
			m_observers = observers;
			m_messageQueue = messageQueue;
		}

		public void appendMessage(String message) {
			m_messageQueue.add(message);
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
			m_window.setMovable(isMovable);
		}

		public void setTopMost(boolean isTopMost) {
			m_window.setTopMost(isTopMost);
		}

		public Rect2D getBounds() {
			return m_window.getBounds();
		}
	}

	private class ChatHudBehaviorInjector extends WindowBehaviourInjector {

		private final Observers observers;
		private final ArrayList<String> messageQueue;

		public ChatHudBehaviorInjector(Observers observers, ArrayList<String> messageQueue) {
			this.observers = observers;
			this.messageQueue = messageQueue;
		}

		@Override
		protected void doInject() throws NoSuchControlException {
			final TextArea inputMessage = getControl(TextArea.class, "message");
			final TextArea log = getControl(TextArea.class, "log");
			final Timer timer = new Timer();

			timer.getObservers().add(new Timer.ITimerObserver() {
				@Override
				public void update(int deltaTime) {
					while(!messageQueue.isEmpty())
					{
						String newText = log.getText() + messageQueue.remove(0) + "\n";
						log.setText(newText);
						log.scrollToEnd();
					}

				}
			});

			addControl(timer);

			getControl(Button.class, "send").getObservers().add(new Button.IButtonPressObserver() {
				@Override
				public void onPress() {
					String message = inputMessage.getText();
					message.trim();
					inputMessage.setText("");

					if(message.length() > 0)
						observers.raise(IChatHudObserver.class).sendMessage(message);
				}
			});

			getObservers().add(new Window.IWindowInputObserver() {
				@Override
				public void onKeyEvent(InputKeyEvent event) {
					if(event.keyCode == KeyEvent.VK_ENTER) {
						String message = inputMessage.getText();
						message.trim();
						inputMessage.setText("");

						if(message.length() > 0)
							observers.raise(IChatHudObserver.class).sendMessage(message);
					}
				}

				@Override
				public void onMouseEvent(InputMouseEvent event) {

				}
			});
		}
	}

	public interface IChatHudObserver {
		void sendMessage(String message);
	}
}
