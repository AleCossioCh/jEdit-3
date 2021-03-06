/*
 * ManagePanel.java - Manages plugins
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Kris Kopicki
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

package org.gjt.sp.jedit.pluginmgr;

//{{{ Imports
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.File;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import javax.swing.border.EmptyBorder;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import org.gjt.sp.jedit.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.gjt.sp.jedit.gui.*;

import org.gjt.sp.jedit.help.*;

import org.gjt.sp.util.Log;
//}}}

public class ManagePanel extends JPanel
{

	//{{{ Private members
	private JCheckBox hideLibraries;
	private JTable table;
	private JScrollPane scrollpane;
	private PluginTableModel pluginModel;
	private PluginManager window;
	//}}}
	
	//{{{ ManagePanel constructor
	public ManagePanel(PluginManager window)
	{
		super(new BorderLayout(12,12));

		this.window = window;

		setBorder(new EmptyBorder(12,12,12,12));

		Box topBox = new Box(BoxLayout.X_AXIS);
		topBox.add(hideLibraries = new HideLibrariesButton());
		add(BorderLayout.NORTH,topBox);

		/* Create the plugin table */
		table = new JTable(pluginModel = new PluginTableModel());
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0,0));
		table.setRowHeight(table.getRowHeight() + 2);
		table.setPreferredScrollableViewportSize(new Dimension(500,300));
		table.setDefaultRenderer(Object.class, new TextRenderer(
			(DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)));
		table.addFocusListener(new TableFocusHandler());
		InputMap tableInputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap tableActionMap = table.getActionMap();
		tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0),"tabOutForward");
		tableActionMap.put("tabOutForward",new KeyboardAction(KeyboardCommand.TAB_OUT_FORWARD));
		tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,InputEvent.SHIFT_MASK),"tabOutBack");
		tableActionMap.put("tabOutBack",new KeyboardAction(KeyboardCommand.TAB_OUT_BACK));
		tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0),"editPlugin");
		tableActionMap.put("editPlugin",new KeyboardAction(KeyboardCommand.EDIT_PLUGIN));
		tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"closePluginManager");
		tableActionMap.put("closePluginManager",new KeyboardAction(KeyboardCommand.CLOSE_PLUGIN_MANAGER));

		TableColumn col1 = table.getColumnModel().getColumn(0);
		TableColumn col2 = table.getColumnModel().getColumn(1);
		TableColumn col3 = table.getColumnModel().getColumn(2);
		TableColumn col4 = table.getColumnModel().getColumn(3);

		col1.setPreferredWidth(30);
		col1.setMinWidth(30);
		col1.setMaxWidth(30);
		col1.setResizable(false);

		col2.setPreferredWidth(300);
		col3.setPreferredWidth(100);
		col4.setPreferredWidth(100);

		JTableHeader header = table.getTableHeader();
		header.setReorderingAllowed(false);
		header.addMouseListener(new HeaderMouseHandler());

		scrollpane = new JScrollPane(table);
		scrollpane.getViewport().setBackground(table.getBackground());
		add(BorderLayout.CENTER,scrollpane);

		/* Create button panel */
		Box buttons = new Box(BoxLayout.X_AXIS);

		buttons.add(new RemoveButton());
		buttons.add(Box.createGlue());
		buttons.add(new HelpButton());

		add(BorderLayout.SOUTH,buttons);
	} //}}}

	//{{{ update() method
	public void update()
	{
		pluginModel.update();
	} //}}}

	//{{{ Inner classes

	//{{{ KeyboardCommand enum
	public enum KeyboardCommand
	{
		NONE,
		TAB_OUT_FORWARD,
		TAB_OUT_BACK,
		EDIT_PLUGIN,
		CLOSE_PLUGIN_MANAGER
	} //}}}

	//{{{ Entry class
	class Entry
	{
		static final String ERROR = "error";
		static final String LOADED = "loaded";
		static final String NOT_LOADED = "not-loaded";

		String status;
		String jar;

		String clazz, name, version, author, docs;
		List<String> jars;

		Entry(String jar)
		{
			jars = new LinkedList<String>();
			this.jar = jar;
			jars.add(this.jar);
			status = NOT_LOADED;
		}

		Entry(PluginJAR jar)
		{
			jars = new LinkedList<String>();
			this.jar = jar.getPath();
			jars.add(this.jar);

			EditPlugin plugin = jar.getPlugin();
			if(plugin != null)
			{
				status = (plugin instanceof EditPlugin.Broken
					? ERROR : LOADED);
				clazz = plugin.getClassName();
				name = jEdit.getProperty("plugin."+clazz+".name");
				version = jEdit.getProperty("plugin."+clazz+".version");
				author = jEdit.getProperty("plugin."+clazz+".author");
				docs = jEdit.getProperty("plugin."+clazz+".docs");

				String jarsProp = jEdit.getProperty("plugin."+clazz+".jars");

				if(jarsProp != null)
				{
					String directory = MiscUtilities.getParentOfPath(this.jar);

					StringTokenizer st = new StringTokenizer(jarsProp);
					while(st.hasMoreElements())
					{
						jars.add(MiscUtilities.constructPath(
							directory,st.nextToken()));
					}
				}
			}
			else
				status = LOADED;
		}
	} //}}}

	//{{{ PluginTableModel class
	class PluginTableModel extends AbstractTableModel
	{
		private List<Entry> entries;
		private int sortType = EntryCompare.NAME;
		private ConcurrentHashMap<String, Object> unloaded;
		// private HashSet<String> unloaded;

		//{{{ Constructor
		public PluginTableModel()
		{
			entries = new ArrayList<Entry>();
			update();
		} //}}}

		//{{{ getColumnCount() method
		public int getColumnCount()
		{
			return 4;
		} //}}}

		//{{{ getColumnClass() method
		public Class getColumnClass(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0: return Boolean.class;
				default: return Object.class;
			}
		} //}}}

		//{{{ getColumnName() method
		public String getColumnName(int column)
		{
			switch (column)
			{
				case 0:
					return " ";
				case 1:
					return jEdit.getProperty("manage-plugins.info.name");
				case 2:
					return jEdit.getProperty("manage-plugins.info.version");
				case 3:
					return jEdit.getProperty("manage-plugins.info.status");
				default:
					throw new Error("Column out of range");
			}
		} //}}}

		//{{{ getEntry() method
		public Entry getEntry(int rowIndex)
		{
			return entries.get(rowIndex);
		} //}}}

		//{{{ getRowCount() method
		public int getRowCount()
		{
			return entries.size();
		} //}}}

		//{{{ getValueAt() method
		public Object getValueAt(int rowIndex,int columnIndex)
		{
			Entry entry = entries.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
					return new Boolean(!entry.status.equals(Entry.NOT_LOADED));
				case 1:
					if(entry.name == null)
					{
						return MiscUtilities.getFileName(entry.jar);
					}
					else
						return entry.name;
				case 2:
					return entry.version;
				case 3:
					return jEdit.getProperty("plugin-manager.status." + entry.status);
				default:
					throw new Error("Column out of range");
			}
		} //}}}

		//{{{ isCellEditable() method
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == 0;
		} //}}}

		//{{{ setValueAt() method
		public void setValueAt(Object value, int rowIndex,
			int columnIndex)
		{
			Entry entry = entries.get(rowIndex);
			if(columnIndex == 0)
			{
				PluginJAR jar = jEdit.getPluginJAR(entry.jar);
				if(jar == null)
				{
					if(value.equals(Boolean.FALSE))
						return;

					PluginJAR.load(entry.jar, true);
				}
				else
				{
					if(value.equals(Boolean.TRUE))
						return;

					unloadPluginJARWithDialog(jar);
				}
			}

			update();
		} //}}}

		//{{{ setSortType() method
		public void setSortType(int type)
		{
			sortType = type;
			sort(type);
		} //}}}

		//{{{ sort() method
		public void sort(int type)
		{
			ArrayList<String> savedSelection = new ArrayList<String>();
			saveSelection(savedSelection);
			Collections.sort(entries,new EntryCompare(type));
			fireTableChanged(new TableModelEvent(this));
			restoreSelection(savedSelection);
		}
		//}}}

		//{{{ update() method
		public void update()
		{
			ArrayList<String> savedSelection = new ArrayList<String>();
			saveSelection(savedSelection);
			entries.clear();

			String systemJarDir = MiscUtilities.constructPath(
				jEdit.getJEditHome(),"jars");
			String userJarDir;
			if(jEdit.getSettingsDirectory() == null)
				userJarDir = null;
			else
			{
				userJarDir = MiscUtilities.constructPath(
					jEdit.getSettingsDirectory(),"jars");
			}

			PluginJAR[] plugins = jEdit.getPluginJARs();
			for(int i = 0; i < plugins.length; i++)
			{
				String path = plugins[i].getPath();
				if(path.startsWith(systemJarDir)
					|| (userJarDir != null
					&& path.startsWith(userJarDir)))
				{
					Entry e = new Entry(plugins[i]);
					if(!hideLibraries.isSelected()
						|| e.clazz != null)
					{
						entries.add(e);
					}
				}
			}

			String[] newPlugins = jEdit.getNotLoadedPluginJARs();
			for(int i = 0; i < newPlugins.length; i++)
			{
				Entry e = new Entry(newPlugins[i]);
				entries.add(e);
			}

			sort(sortType);
			restoreSelection(savedSelection);
		} //}}}

		//{{{ unloadPluginJARWithDialog() method
		// Perhaps this should also be moved to PluginJAR class?
		private void unloadPluginJARWithDialog(PluginJAR jar)
		{
			// unloaded = new HashSet<String>();
			unloaded = new ConcurrentHashMap<String, Object>();
			String[] dependents = jar.getDependentPlugins();
			if(dependents.length == 0)
				unloadPluginJAR(jar);
			else
			{
				List<String> closureSet = new LinkedList<String>();
				PluginJAR.transitiveClosure(dependents, closureSet);
				ArrayList<String> listModel = new ArrayList<String>();
				listModel.addAll(closureSet);
				Collections.sort(listModel, new MiscUtilities.StringICaseCompare());

				int button = GUIUtilities.listConfirm(window,"plugin-manager.dependency",
					new String[] { jar.getFile().getName() }, listModel.toArray());
				if(button == JOptionPane.YES_OPTION)
					unloadPluginJAR(jar);
			}
		} //}}}

		//{{{ unloadPluginJAR() method
		private void unloadPluginJAR(PluginJAR jar)
		{
			String[] dependents = jar.getDependentPlugins();
			for (String dependent : dependents) 
			{
				if (!unloaded.containsKey(dependent)) 
				{
					unloaded.put(dependent, Boolean.TRUE);
					PluginJAR _jar = jEdit.getPluginJAR(dependent);
					if(_jar != null)
						unloadPluginJAR(_jar);
				}
			}
			jEdit.removePluginJAR(jar,false);
			jEdit.setBooleanProperty("plugin-blacklist."+MiscUtilities.getFileName(jar.getPath()),true);
		} //}}}
		
		//{{{ saveSelection() method
		public void saveSelection(ArrayList<String> savedSelection)
		{
			if (null != table)
			{
				int[] rows = table.getSelectedRows();
				for (int i=0 ; i<rows.length ; i++)
				{
					savedSelection.add(new String((entries.get(rows[i])).jar));
				}
			}
		} //}}}
		
		//{{{ restoreSelection() method
		public void restoreSelection(ArrayList<String> savedSelection)
		{
			if (null != table)
			{
				table.setColumnSelectionInterval(0,0);
				if (0 < savedSelection.size())
				{
					int i = 0;
					int rowCount = getRowCount();
					for ( ; i<rowCount ; i++)
					{
						if (savedSelection.contains((entries.get(i)).jar))
						{
							table.setRowSelectionInterval(i,i);
							break;
						}
					}
					ListSelectionModel lsm = table.getSelectionModel();
					for ( ; i<rowCount ; i++)
					{
						if (savedSelection.contains((entries.get(i)).jar))
						{
							lsm.addSelectionInterval(i,i);
						}
					}
				}
				else
				{
					table.setRowSelectionInterval(0,0);
					JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
					scrollbar.setValue(scrollbar.getMinimum());
				}
			}
		} //}}}
	} //}}}

	//{{{ TextRenderer class
	class TextRenderer extends DefaultTableCellRenderer
	{
		private DefaultTableCellRenderer tcr;

		public TextRenderer(DefaultTableCellRenderer tcr)
		{
			this.tcr = tcr;
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			Entry entry = pluginModel.getEntry(row);
			if (entry.status.equals(Entry.ERROR))
				tcr.setForeground(Color.red);
			else
				tcr.setForeground(UIManager.getColor("Table.foreground"));
			return tcr.getTableCellRendererComponent(table,value,isSelected,false,row,column);
		}
	} //}}}

	//{{{ HideLibrariesButton class
	class HideLibrariesButton extends JCheckBox implements ActionListener
	{
		HideLibrariesButton()
		{
			super(jEdit.getProperty("plugin-manager.hide-libraries"));
			setSelected(jEdit.getBooleanProperty(
				"plugin-manager.hide-libraries.toggle"));
			addActionListener(this);
		}

		public void actionPerformed(ActionEvent evt)
		{
			jEdit.setBooleanProperty(
				"plugin-manager.hide-libraries.toggle",
				isSelected());
			ManagePanel.this.update();
		}
	} //}}}

	//{{{ RemoveButton class
	class RemoveButton extends JButton implements ListSelectionListener, ActionListener
	{
		public RemoveButton()
		{
			super(jEdit.getProperty("manage-plugins.remove"));
			table.getSelectionModel().addListSelectionListener(this);
			addActionListener(this);
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent evt)
		{
			int[] selected = table.getSelectedRows();

			List listModel = new LinkedList();
			Roster roster = new Roster();
			for(int i = 0; i < selected.length; i++)
			{
				Entry entry = pluginModel.getEntry(selected[i]);
				Iterator iter = entry.jars.iterator();
				while(iter.hasNext())
				{
					String jar = (String)iter.next();
					listModel.add(jar);
					roster.addRemove(jar);
				}
			}

			int button = GUIUtilities.listConfirm(window,
				"plugin-manager.remove-confirm",
				null,listModel.toArray());
			if(button == JOptionPane.YES_OPTION)
			{
				roster.performOperationsInAWTThread(window);
				pluginModel.update();
				table.setRowSelectionInterval(0,0);
				table.setColumnSelectionInterval(0,0);
				JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
				scrollbar.setValue(scrollbar.getMinimum());
			}
		}

		public void valueChanged(ListSelectionEvent e)
		{
			if (table.getSelectedRowCount() == 0)
				setEnabled(false);
			else
				setEnabled(true);
		}
	} //}}}

	//{{{ HelpButton class
	class HelpButton extends JButton implements ListSelectionListener, ActionListener
	{
		private URL docURL;

		public HelpButton()
		{
			super(jEdit.getProperty("manage-plugins.help"));
			table.getSelectionModel().addListSelectionListener(this);
			addActionListener(this);
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent evt)
		{
			new HelpViewer(docURL);
		}

		public void valueChanged(ListSelectionEvent e)
		{
			if (table.getSelectedRowCount() == 1)
			{
				try
				{
					Entry entry = pluginModel.getEntry(table.getSelectedRow());
					String label = entry.clazz;
					String docs = entry.docs;
					if (label != null) {
						EditPlugin plug = jEdit.getPlugin(label, false);
						PluginJAR jar = null;
						if (plug != null) jar = plug.getPluginJAR();
						if(jar != null && label != null && docs != null)
						{
							URL url = jar.getClassLoader().getResource(docs);
							if(url != null)
							{
								docURL = url;
								setEnabled(true);
								return;
							}
						}
					}
				}
				catch (Exception ex) {
					Log.log(Log.ERROR, this, "ManagePanel HelpButton Update", ex);
				}
			}
			setEnabled(false);
		}
	} //}}}

	//{{{ EntryCompare class
	static class EntryCompare implements Comparator
	{
		public static final int NAME = 1;
		public static final int STATUS = 2;

		private int type;

		public EntryCompare(int type)
		{
			this.type = type;
		}

		public int compare(Object o1, Object o2)
		{
			ManagePanel.Entry e1 = (ManagePanel.Entry)o1;
			ManagePanel.Entry e2 = (ManagePanel.Entry)o2;

			if (type == NAME)
				return compareNames(e1,e2);
			else
			{
				int result;
				if ((result = e1.status.compareToIgnoreCase(e2.status)) == 0)
					return compareNames(e1,e2);
				return result;
			}
		}

		private int compareNames(ManagePanel.Entry e1, ManagePanel.Entry e2)
		{
			String s1, s2;
			if(e1.name == null)
				s1 = MiscUtilities.getFileName(e1.jar);
			else
				s1 = e1.name;
			if(e2.name == null)
				s2 = MiscUtilities.getFileName(e2.jar);
			else
				s2 = e2.name;

			return s1.compareToIgnoreCase(s2);
		}
	} //}}}

	//{{{ HeaderMouseHandler class
	class HeaderMouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			switch(table.getTableHeader().columnAtPoint(evt.getPoint()))
			{
				case 1:
					pluginModel.setSortType(EntryCompare.NAME);
					break;
				case 3:
					pluginModel.setSortType(EntryCompare.STATUS);
					break;
				default:
					break;
			}
		}
	} //}}}

	//{{{ KeyboardAction class
	class KeyboardAction extends AbstractAction
	{
		private KeyboardCommand command = KeyboardCommand.NONE;
		
		public KeyboardAction(KeyboardCommand command)
		{
			super();
			this.command = command;
		}
		
		public void actionPerformed(ActionEvent evt)
		{
			switch (command)
			{
			case TAB_OUT_FORWARD:
				KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
				break;
			case TAB_OUT_BACK:
				KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
				break;
			case EDIT_PLUGIN:
				ArrayList<String> savedSelection = new ArrayList<String>();
				pluginModel.saveSelection(savedSelection);
				int[] rows = table.getSelectedRows();
				Object[] state = new Object[rows.length];
				for (int i=0 ; i<rows.length ; i++)
				{
					state[i] = pluginModel.getValueAt(rows[i],0);
				}
				for (int i=0 ; i<rows.length ; i++)
				{
					for (int j=0, c=pluginModel.getRowCount() ; j<c ; j++)
					{
						if ((pluginModel.entries.get(j)).jar.equals(savedSelection.get(i)))
						{
							pluginModel.setValueAt(state[i].equals(Boolean.FALSE),j,0);
							break;
						}
					}
				}
				pluginModel.restoreSelection(savedSelection);
				break;
			case CLOSE_PLUGIN_MANAGER:
				window.ok();
				break;
			default:
				throw new InternalError();
			}
		}
	} //}}}

	//{{{ TableFocusHandler class
	class TableFocusHandler extends FocusAdapter
	{
		public void focusGained(FocusEvent fe)
		{
			if (-1 == table.getSelectedRow())
			{
				table.setRowSelectionInterval(0,0);
				JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
				scrollbar.setValue(scrollbar.getMinimum());
			}
			if (-1 == table.getSelectedColumn())
			{
				table.setColumnSelectionInterval(0,0);
			}
		}
	} //}}}

	//}}}
}
