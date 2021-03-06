/*
 * DirectoryMenu.java - File list menu
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001, 2002 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.gui;

//{{{ Imports
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.*;
//}}}

public class DirectoryMenu extends EnhancedMenu implements MenuListener
{
	//{{{ DirectoryMenu constructor
	public DirectoryMenu(String name, String dir)
	{
		super(name);
		this.dir = dir;
		addMenuListener(this);
	} //}}}

	//{{{ menuSelected() method
	public void menuSelected(MenuEvent evt)
	{
		final View view = GUIUtilities.getView(this);

		if(getMenuComponentCount() != 0)
			removeAll();

		final String path;
		if(dir == null)
		{
			path = MiscUtilities.getParentOfPath(
				view.getBuffer().getPath());
		}
		else
			path = dir;

		JMenuItem mi = new JMenuItem(path + ":");
		mi.setActionCommand(path);
		mi.setIcon(FileCellRenderer.openDirIcon);

		//{{{ ActionListeners
		ActionListener fileListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				jEdit.openFile(view,evt.getActionCommand());
			}
		};

		ActionListener dirListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				VFSBrowser.browseDirectory(view,
					evt.getActionCommand());
			}
		}; //}}}

		mi.addActionListener(dirListener);

		add(mi);
		addSeparator();

		if(dir == null && view.getBuffer().getFile() == null)
		{
			mi = new JMenuItem(jEdit.getProperty(
				"directory.not-local"));
			mi.setEnabled(false);
			add(mi);
			return;
		}

		File directory = new File(path);

		JMenu current = this;

		// for filtering out backups
		String backupPrefix = jEdit.getProperty("backup.prefix");
		String backupSuffix = jEdit.getProperty("backup.suffix");

		File[] list = directory.listFiles();
		if(list == null || list.length == 0)
		{
			mi = new JMenuItem(jEdit.getProperty(
				"directory.no-files"));
			mi.setEnabled(false);
			add(mi);
		}
		else
		{
			int maxItems = jEdit.getIntegerProperty("menu.spillover",20);

			MiscUtilities.quicksort(list,
				new MiscUtilities.StringICaseCompare());
			for(int i = 0; i < list.length; i++)
			{
				File file = list[i];

				String name = file.getName();

				// skip marker files
				if(name.endsWith(".marks"))
					continue;

				// skip autosave files
				if(name.startsWith("#") && name.endsWith("#"))
					continue;

				// skip backup files
				if((backupPrefix.length() != 0
					&& name.startsWith(backupPrefix))
					|| (backupSuffix.length() != 0
					&& name.endsWith(backupSuffix)))
					continue;

				// skip directories
				//if(file.isDirectory())
				//	continue;

				mi = new JMenuItem(name);
				mi.setActionCommand(file.getPath());
				mi.addActionListener(file.isDirectory()
					? dirListener
					: fileListener);
				mi.setIcon(file.isDirectory()
					? FileCellRenderer.dirIcon
					: FileCellRenderer.fileIcon);

				if(current.getItemCount() >= maxItems && i != list.length - 1)
				{
					//current.addSeparator();
					JMenu newCurrent = new JMenu(
						jEdit.getProperty(
						"common.more"));
					current.add(newCurrent);
					current = newCurrent;
				}
				current.add(mi);
			}
		}
	} //}}}

	public void menuDeselected(MenuEvent e) {}

	public void menuCanceled(MenuEvent e) {}

	//{{{ Private members
	private String dir;
	//}}}
}
