/*
 * Copyright (C) 2010-2014 Laurent CLOUET
 * Author Laurent CLOUET <laurent.clouet@nopnop.net>
 *
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.sheepit.client.standalone;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import com.sheepit.client.Client;
import com.sheepit.client.Configuration;
import com.sheepit.client.Gui;
import com.sheepit.client.standalone.swing.activity.Settings;
import com.sheepit.client.standalone.swing.activity.Working;

public class GuiSwing extends JFrame implements Gui {
	public static final String type = "swing";
	public enum ActivityType {
		WORKING, SETTINGS
	}
	
	private JPanel panel;
	private Working activityWorking;
	private Settings activitySettings;
	
	private int framesRendered;
	
	private boolean waitingForAuthentication;
	private Client client;
	
	private ThreadClient threadClient;
	
	private ResourceBundle exceptionResources;
	
	public GuiSwing() {
		framesRendered = 0;
		
		waitingForAuthentication = true;
		
		exceptionResources = ResourceBundle.getBundle("ExceptionResources", Locale.getDefault());
	}
	
	@Override
	public void start() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
		
		URL iconUrl = getClass().getResource("/icon.png");
		if (iconUrl != null) {
			ImageIcon img = new ImageIcon(iconUrl);
			setIconImage(img.getImage());
		}
		
		setTitle("SheepIt Render Farm");
		setSize(600, 600);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		setContentPane(this.panel);
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		activityWorking = new Working(this);
		activitySettings = new Settings(this);
		
		this.showActivity(ActivityType.SETTINGS);
		
		while (waitingForAuthentication) {
			try {
				synchronized (this) {
					wait();
				}
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void stop() {
		System.out.println("GuiSwing::stop()");
	}
	
	@Override
	public void status(String msg_) {
		if (activityWorking != null) {
			this.activityWorking.setStatus(msg_);
		}
	}
	
	@Override
	public void error(String msg_) {
		status(msg_);
	}
	
	@Override
	public void AddFrameRendered() {
		framesRendered++;
		
		if (activityWorking != null) {
			this.activityWorking.setRenderedFrame(framesRendered);
		}
		else {
			System.out.println(this.exceptionResources.getString("AddFrameRenderedNoActivity"));
		}
	}
	
	@Override
	public void framesRemaining(int n) {
		if (activityWorking != null) {
			this.activityWorking.setRemainingFrame(n);
		}
		else {
			MessageFormat formatter = new MessageFormat(this.exceptionResources.getString("FramesRemainingNoActivity"), this.exceptionResources.getLocale());
			System.out.println(formatter.format(new Object[]{n}));
		}
	}
	
	@Override
	public Client getClient() {
		return client;
	}
	
	@Override
	public void setClient(Client cli) {
		client = cli;
		exceptionResources = ResourceBundle.getBundle("ExceptionResources", cli.getConfiguration().getLocale());
	}
	
	public void addPadding(int x, int y, int width, int height) {
		GridBagConstraints constraints = new GridBagConstraints();
		JLabel label = new JLabel("");
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.gridwidth = width;
		constraints.gridheight = height;
		constraints.gridx = x;
		constraints.gridy = y;
		getContentPane().add(label, constraints);
	}
	
	public Configuration getConfiguration() {
		return client.getConfiguration();
	}
	
	public void setCredentials(String contentLogin, String contentPassword) {
		client.getConfiguration().setLogin(contentLogin);
		client.getConfiguration().setPassword(contentPassword);
		
		waitingForAuthentication = false;
		synchronized (this) {
			notifyAll();
		}
		
		if (threadClient == null || threadClient.isAlive() == false) {
			threadClient = new ThreadClient();
			threadClient.start();
		}
		
		showActivity(ActivityType.WORKING);
	}
	
	public void showActivity(ActivityType type) {
		panel.removeAll();
		panel.doLayout();
		
		if (type == ActivityType.WORKING) {
			activityWorking.show();
		}
		else if (type == ActivityType.SETTINGS) {
			activitySettings.show();
		}
		
		setVisible(true);
		panel.repaint();
	}
	
	public class ThreadClient extends Thread {
		@Override
		public void run() {
			if (GuiSwing.this.client != null) {
				GuiSwing.this.client.run();
			}
		}
	}
	
}
