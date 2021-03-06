/*
 * GUIUtilities.java - Various GUI utility functions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2002 Slava Pestov
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

package org.gjt.sp.jedit;

//{{{ Imports
import gnu.regexp.REException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.*;
import java.util.*;
import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.util.Log;
//}}}

/**
 * Class with several useful GUI functions.<p>
 *
 * It provides methods for:
 * <ul>
 * <li>Loading menu bars, menus and menu items from the properties
 * <li>Loading popup menus from the properties
 * <li>Loading tool bars and tool bar buttons from the properties
 * <li>Displaying various common dialog boxes
 * <li>Converting string representations of colors to color objects
 * <li>Loading and saving window geometry from the properties
 * <li>Displaying file open and save dialog boxes
 * <li>Loading images and caching them
 * </ul>
 *
 * @author Slava Pestov
 * @version $Id: GUIUtilities.java 4421 2003-01-09 02:55:19Z spestov $
 */
public class GUIUtilities
{
	//{{{ Some predefined icons
	public static final Icon NEW_BUFFER_ICON = loadIcon("new.gif");
	public static final Icon DIRTY_BUFFER_ICON = loadIcon("dirty.gif");
	public static final Icon READ_ONLY_BUFFER_ICON = loadIcon("readonly.gif");
	public static final Icon NORMAL_BUFFER_ICON = loadIcon("normal.gif");
	public static final Icon WINDOW_ICON = loadIcon("jedit-icon.gif");
	//}}}

	//{{{ Icon methods

	//{{{ loadIcon() method
	/**
	 * Loads an icon.
	 * @param iconName The icon name
	 * @since jEdit 2.6pre7
	 */
	public static Icon loadIcon(String iconName)
	{
		if(icons == null)
			icons = new Hashtable();

		// check if there is a cached version first
		Icon icon = (Icon)icons.get(iconName);
		if(icon != null)
			return icon;

		// get the icon
		if(iconName.startsWith("file:"))
		{
			icon = new ImageIcon(iconName.substring(5));
		}
		else
		{
			URL url = GUIUtilities.class.getResource(
				"/org/gjt/sp/jedit/icons/" + iconName);
			if(url == null)
			{
				Log.log(Log.ERROR,GUIUtilities.class,
					"Icon not found: " + iconName);
				return null;
			}

			icon = new ImageIcon(url);
		}

		icons.put(iconName,icon);
		return icon;
	} //}}}

	//{{{ getEditorIcon() method
	/**
	 * Returns the default editor window image.
	 */
	public static Image getEditorIcon()
	{
		return ((ImageIcon)WINDOW_ICON).getImage();
	} //}}}

	//{{{ getPluginIcon() method
	/**
	 * Returns the default plugin window image.
	 */
	public static Image getPluginIcon()
	{
		return ((ImageIcon)WINDOW_ICON).getImage();
	} //}}}

	//}}}

	//{{{ Menus, tool bars

	//{{{ loadMenuBar() method
	/**
	 * Creates a menubar. Plugins should not need to call this method.
	 * @param name The menu bar name
	 * @since jEdit 3.2pre5
	 */
	public static JMenuBar loadMenuBar(String name)
	{
		String menus = jEdit.getProperty(name);
		StringTokenizer st = new StringTokenizer(menus);

		JMenuBar mbar = new JMenuBar();

		while(st.hasMoreTokens())
		{
			String menu = st.nextToken();
			if(menu.equals("plugins"))
				loadPluginsMenu(mbar);
			else
				mbar.add(loadMenu(menu));
		}

		return mbar;
	} //}}}

	//{{{ loadMenu() method
	/**
	 * @deprecated Use loadMenu(name) instead
	 */
	public static JMenu loadMenu(View view, String name)
	{
		return loadMenu(name);
	} //}}}

	//{{{ loadMenu() method
	/**
	 * Creates a menu.
	 * @param view The view to load the menu for
	 * @param name The menu name
	 * @since jEdit 2.6pre2
	 */
	public static JMenu loadMenu(String name)
	{
		if(name.equals("recent-files"))
			return new RecentFilesMenu();
		else if(name.equals("recent-directories"))
			return new RecentDirectoriesMenu();
		else if(name.equals("current-directory"))
			return new DirectoryMenu("current-directory",null);
		else if(name.equals("markers"))
			return new MarkersMenu();
		else if(name.equals("jedit-directory"))
			return new DirectoryMenu("jedit-directory",jEdit.getJEditHome());
		else if(name.equals("settings-directory"))
		{
			String settings = jEdit.getSettingsDirectory();
			if(settings == null)
				settings = jEdit.getJEditHome();
			return new DirectoryMenu("settings-directory",settings);
		}
		else if(name.equals("macros"))
			return new MacrosMenu();
		else
			return new EnhancedMenu(name);
	} //}}}

	//{{{ loadPopupMenu() method
	/**
	 * Creates a popup menu.
	 * @param name The menu name
	 * @since jEdit 2.6pre2
	 */
	public static JPopupMenu loadPopupMenu(String name)
	{
		JPopupMenu menu = new JPopupMenu();

		String menuItems = jEdit.getProperty(name);
		if(menuItems != null)
		{
			StringTokenizer st = new StringTokenizer(menuItems);
			while(st.hasMoreTokens())
			{
				String menuItemName = st.nextToken();
				if(menuItemName.equals("-"))
					menu.addSeparator();
				else
				{
					if(menuItemName.startsWith("%"))
						menu.add(loadMenu(menuItemName.substring(1)));
					else
						menu.add(loadMenuItem(menuItemName,false));
				}
			}
		}

		return menu;
	} //}}}

	//{{{ loadMenuItem() method
	/**
	 * Creates a menu item.
	 * @param name The menu item name
	 * @since jEdit 2.6pre1
	 */
	public static JMenuItem loadMenuItem(String name)
	{
		return loadMenuItem(name,true);
	} //}}}

	//{{{ loadMenuItem() method
	/**
	 * Creates a menu item.
	 * @param name The menu item name
	 * @param setMnemonic True if the menu item should have a mnemonic
	 * @since jEdit 3.1pre1
	 */
	public static JMenuItem loadMenuItem(String name, boolean setMnemonic)
	{
		EditAction action = jEdit.getAction(name);
		String label = (action == null ?
			jEdit.getProperty(name + ".label")
			: action.getLabel());
		if(label == null)
			label = name;

		char mnemonic;
		int index = label.indexOf('$');
		if(index != -1 && label.length() - index > 1)
		{
			mnemonic = Character.toLowerCase(label.charAt(index + 1));
			label = label.substring(0,index).concat(label.substring(++index));
		}
		else
			mnemonic = '\0';

		JMenuItem mi;
		if(action != null && action.isToggle())
			mi = new EnhancedCheckBoxMenuItem(label,action);
		else
			mi = new EnhancedMenuItem(label,action);

		if(!OperatingSystem.isMacOS() && setMnemonic && mnemonic != '\0')
			mi.setMnemonic(mnemonic);

		return mi;
	} //}}}

	//{{{ loadToolBar() method
	/**
	 * Creates a toolbar.
	 * @param name The toolbar name
	 */
	public static JToolBar loadToolBar(String name)
	{
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		String buttons = jEdit.getProperty(name);
		if(buttons != null)
		{
			StringTokenizer st = new StringTokenizer(buttons);
			while(st.hasMoreTokens())
			{
				String button = st.nextToken();
				if(button.equals("-"))
					toolBar.addSeparator();
				else
				{
					JButton b = loadToolButton(button);
					if(b != null)
						toolBar.add(b);
				}
			}
		}

		return toolBar;
	} //}}}

	//{{{ loadToolButton() method
	/**
	 * Loads a tool bar button. The tooltip is constructed from
	 * the <code><i>name</i>.label</code> and
	 * <code><i>name</i>.shortcut</code> properties and the icon is loaded
	 * from the resource named '/org/gjt/sp/jedit/icons/' suffixed
	 * with the value of the <code><i>name</i>.icon</code> property.
	 * @param name The name of the button
	 */
	public static EnhancedButton loadToolButton(String name)
	{
		EditAction action = jEdit.getAction(name);
		String label = (action == null
			? jEdit.getProperty(name + ".label")
			: action.getLabel());

		if(label == null)
			label = name;

		Icon icon;
		String iconName = jEdit.getProperty(name + ".icon");
		if(iconName == null)
			icon = loadIcon("BrokenImage.png");
		else
		{
			icon = loadIcon(iconName);
			if(icon == null)
				icon = loadIcon("BrokenImage.png");
		}

		String toolTip = prettifyMenuLabel(label);
		String shortcut1 = jEdit.getProperty(name + ".shortcut");
		String shortcut2 = jEdit.getProperty(name + ".shortcut2");
		if(shortcut1 != null || shortcut2 != null)
		{
			toolTip = toolTip + " ("
				+ (shortcut1 != null
				? shortcut1 : "")
				+ ((shortcut1 != null && shortcut2 != null)
				? " or " : "")
				+ (shortcut2 != null
				? shortcut2
				: "") + ")";
		}

		return new EnhancedButton(icon,toolTip,action);
	} //}}}

	//{{{ prettifyMenuLabel() method
	/**
	 * `Prettifies' a menu item label by removing the `$' sign. This
	 * can be used to process the contents of an <i>action</i>.label
	 * property.
	 */
	public static String prettifyMenuLabel(String label)
	{
		int index = label.indexOf('$');
		if(index != -1)
		{
			label = label.substring(0,index)
				.concat(label.substring(index + 1));
		}
		return label;
	} //}}}

	//}}}

	//{{{ Canned dialog boxes

	//{{{ message() method
	/**
	 * Displays a dialog box.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property. The message
	 * is formatted by the property manager with <code>args</code> as
	 * positional parameters.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 */
	public static void message(Component comp, String name, Object[] args)
	{
		hideSplashScreen();

		JOptionPane.showMessageDialog(comp,
			jEdit.getProperty(name.concat(".message"),args),
			jEdit.getProperty(name.concat(".title"),args),
			JOptionPane.INFORMATION_MESSAGE);
	} //}}}

	//{{{ error() method
	/**
	 * Displays an error dialog box.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property. The message
	 * is formatted by the property manager with <code>args</code> as
	 * positional parameters.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 */
	public static void error(Component comp, String name, Object[] args)
	{
		hideSplashScreen();

		JOptionPane.showMessageDialog(comp,
			jEdit.getProperty(name.concat(".message"),args),
			jEdit.getProperty(name.concat(".title"),args),
			JOptionPane.ERROR_MESSAGE);
	} //}}}

	//{{{ input() method
	/**
	 * Displays an input dialog box and returns any text the user entered.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param def The text to display by default in the input field
	 */
	public static String input(Component comp, String name, Object def)
	{
		return input(comp,name,null,def);
	} //}}}

	//{{{ inputProperty() method
	/**
	 * Displays an input dialog box and returns any text the user entered.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param def The property whose text to display in the input field
	 */
	public static String inputProperty(Component comp, String name,
		String def)
	{
		return inputProperty(comp,name,null,def);
	} //}}}

	//{{{ input() method
	/**
	 * Displays an input dialog box and returns any text the user entered.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param def The text to display by default in the input field
	 * @param args Positional parameters to be substituted into the
	 * message text
	 * @since jEdit 3.1pre3
	 */
	public static String input(Component comp, String name,
		Object[] args, Object def)
	{
		hideSplashScreen();

		String retVal = (String)JOptionPane.showInputDialog(comp,
			jEdit.getProperty(name.concat(".message"),args),
			jEdit.getProperty(name.concat(".title")),
			JOptionPane.QUESTION_MESSAGE,null,null,def);
		return retVal;
	} //}}}

	//{{{ inputProperty() method
	/**
	 * Displays an input dialog box and returns any text the user entered.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 * @param def The property whose text to display in the input field
	 * @since jEdit 3.1pre3
	 */
	public static String inputProperty(Component comp, String name,
		Object[] args, String def)
	{
		hideSplashScreen();

		String retVal = (String)JOptionPane.showInputDialog(comp,
			jEdit.getProperty(name.concat(".message"),args),
			jEdit.getProperty(name.concat(".title")),
			JOptionPane.QUESTION_MESSAGE,
			null,null,jEdit.getProperty(def));
		if(retVal != null)
			jEdit.setProperty(def,retVal);
		return retVal;
	} //}}}

	//{{{ confirm() method
	/**
	 * Displays a confirm dialog box and returns the button pushed by the
	 * user. The title of the dialog is fetched from the
	 * <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 * @param buttons The buttons to display - for example,
	 * JOptionPane.YES_NO_CANCEL_OPTION
	 * @param type The dialog type - for example,
	 * JOptionPane.WARNING_MESSAGE
	 * @since jEdit 3.1pre3
	 */
	public static int confirm(Component comp, String name,
		Object[] args, int buttons, int type)
	{
		hideSplashScreen();

		return JOptionPane.showConfirmDialog(comp,
			jEdit.getProperty(name + ".message",args),
			jEdit.getProperty(name + ".title"),buttons,type);
	} //}}}

	//{{{ showVFSFileDialog() method
	/**
	 * Displays a VFS file selection dialog box.
	 * @param view The view
	 * @param path The initial directory to display. May be null
	 * @param type The dialog type. One of
	 * <code>VFSBrowser.OPEN_DIALOG</code>,
	 * <code>VFSBrowser.SAVE_DIALOG</code>, or
	 * <code>VFSBrowser.CHOOSE_DIRECTORY_DIALOG</code>.
	 * @param multipleSelection True if multiple selection should be allowed
	 * @return The selected file(s)
	 * @since jEdit 2.6pre2
	 */
	public static String[] showVFSFileDialog(View view, String path,
		int type, boolean multipleSelection)
	{
		hideSplashScreen();

		VFSFileChooserDialog fileChooser = new VFSFileChooserDialog(
			view,path,type,multipleSelection);
		String[] selectedFiles = fileChooser.getSelectedFiles();
		if(selectedFiles == null)
			return null;

		return selectedFiles;
	} //}}}

	//}}}

	//{{{ Colors and styles

	//{{{ parseColor() method
	/**
	 * Converts a color name to a color object. The name must either be
	 * a known string, such as `red', `green', etc (complete list is in
	 * the <code>java.awt.Color</code> class) or a hex color value
	 * prefixed with `#', for example `#ff0088'.
	 * @param name The color name
	 */
	public static Color parseColor(String name)
	{
		return parseColor(name, Color.black);
	} //}}}

	//{{{ parseColor() method
	public static Color parseColor(String name, Color defaultColor)
	{
		if(name == null)
			return defaultColor;
		else if(name.startsWith("#"))
		{
			try
			{
				return Color.decode(name);
			}
			catch(NumberFormatException nf)
			{
				return defaultColor;
			}
		}
		else if("red".equals(name))
			return Color.red;
		else if("green".equals(name))
			return Color.green;
		else if("blue".equals(name))
			return Color.blue;
		else if("yellow".equals(name))
			return Color.yellow;
		else if("orange".equals(name))
			return Color.orange;
		else if("white".equals(name))
			return Color.white;
		else if("lightGray".equals(name))
			return Color.lightGray;
		else if("gray".equals(name))
			return Color.gray;
		else if("darkGray".equals(name))
			return Color.darkGray;
		else if("black".equals(name))
			return Color.black;
		else if("cyan".equals(name))
			return Color.cyan;
		else if("magenta".equals(name))
			return Color.magenta;
		else if("pink".equals(name))
			return Color.pink;
		else
			return defaultColor;
	} //}}}

	//{{{ getColorHexString() method
	/**
	 * Converts a color object to its hex value. The hex value
	 * prefixed is with `#', for example `#ff0088'.
	 * @param c The color object
	 */
	public static String getColorHexString(Color c)
	{
		String colString = Integer.toHexString(c.getRGB() & 0xffffff);
		return "#000000".substring(0,7 - colString.length()).concat(colString);
	} //}}}

	//{{{ parseStyle() method
	/**
	 * Converts a style string to a style object.
	 * @param str The style string
	 * @param family Style strings only specify font style, not font family
	 * @param size Style strings only specify font style, not font family
	 * @exception IllegalArgumentException if the style is invalid
	 * @since jEdit 3.2pre6
	 */
	public static SyntaxStyle parseStyle(String str, String family, int size)
		throws IllegalArgumentException
	{
		return parseStyle(str,family,size,true);
	} //}}}

	//{{{ parseStyle() method
	/**
	 * Converts a style string to a style object.
	 * @param str The style string
	 * @param family Style strings only specify font style, not font family
	 * @param size Style strings only specify font style, not font family
	 * @param color If false, the styles will be monochrome
	 * @exception IllegalArgumentException if the style is invalid
	 * @since jEdit 4.0pre4
	 */
	public static SyntaxStyle parseStyle(String str, String family, int size,
		boolean color)
		throws IllegalArgumentException
	{
		Color fgColor = Color.black;
		Color bgColor = null;
		boolean italic = false;
		boolean bold = false;
		StringTokenizer st = new StringTokenizer(str);
		while(st.hasMoreTokens())
		{
			String s = st.nextToken();
			if(s.startsWith("color:"))
			{
				if(color)
					fgColor = GUIUtilities.parseColor(s.substring(6), Color.black);
			}
			else if(s.startsWith("bgColor:"))
			{
				if(color)
					bgColor = GUIUtilities.parseColor(s.substring(8), null);
			}
			else if(s.startsWith("style:"))
			{
				for(int i = 6; i < s.length(); i++)
				{
					if(s.charAt(i) == 'i')
						italic = true;
					else if(s.charAt(i) == 'b')
						bold = true;
					else
						throw new IllegalArgumentException(
							"Invalid style: " + s);
				}
			}
			else
				throw new IllegalArgumentException(
					"Invalid directive: " + s);
		}
		return new SyntaxStyle(fgColor,bgColor,
			new Font(family,
			(italic ? Font.ITALIC : 0) | (bold ? Font.BOLD : 0),
			size));
	} //}}}

	//{{{ getStyleString() method
	/**
	 * Converts a style into it's string representation.
	 * @param style The style
	 */
	public static String getStyleString(SyntaxStyle style)
	{
		StringBuffer buf = new StringBuffer();

		if(style.getForegroundColor() != null)
		{
			buf.append("color:" + getColorHexString(style.getForegroundColor()));
		}

		if(style.getBackgroundColor() != null) 
		{
			buf.append(" bgColor:" + getColorHexString(style.getBackgroundColor()));
		}
		if(!style.getFont().isPlain())
		{
			buf.append(" style:" + (style.getFont().isItalic() ? "i" : "")
				+ (style.getFont().isBold() ? "b" : ""));
		}

		return buf.toString();
	} //}}}

	//{{{ loadStyles() method
	/**
	 * Loads the syntax styles from the properties, giving them the specified
	 * base font family and size.
	 * @param family The font family
	 * @param size The font size
	 * @since jEdit 3.2pre6
	 */
	public static SyntaxStyle[] loadStyles(String family, int size)
	{
		return loadStyles(family,size,true);
	} //}}}

	//{{{ loadStyles() method
	/**
	 * Loads the syntax styles from the properties, giving them the specified
	 * base font family and size.
	 * @param family The font family
	 * @param size The font size
	 * @param color If false, the styles will be monochrome
	 * @since jEdit 4.0pre4
	 */
	public static SyntaxStyle[] loadStyles(String family, int size, boolean color)
	{
		SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

		try
		{
			styles[Token.COMMENT1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.comment1"),
				family,size,color);
			styles[Token.COMMENT2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.comment2"),
				family, size,color);
			styles[Token.LITERAL1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.literal1"),
				family,size,color);
			styles[Token.LITERAL2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.literal2"),
				family,size,color);
			styles[Token.LABEL] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.label"),
				family,size,color);
			styles[Token.KEYWORD1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword1"),
				family,size,color);
			styles[Token.KEYWORD2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword2"),
				family,size,color);
			styles[Token.KEYWORD3] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword3"),
				family,size,color);
			styles[Token.FUNCTION] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.function"),
				family,size,color);
			styles[Token.MARKUP] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.markup"),
				family,size,color);
			styles[Token.OPERATOR] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.operator"),
				family,size,color);
			styles[Token.DIGIT] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.digit"),
				family,size,color);
			styles[Token.INVALID] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.invalid"),
				family,size,color);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,GUIUtilities.class,e);
		}

		return styles;
	} //}}}

	//}}}

	//{{{ Loading, saving window geometry

	//{{{ loadGeometry() method
	/**
	 * Loads a windows's geometry from the properties.
	 * The geometry is loaded from the <code><i>name</i>.x</code>,
	 * <code><i>name</i>.y</code>, <code><i>name</i>.width</code> and
	 * <code><i>name</i>.height</code> properties.
	 *
	 * @param win The window
	 * @param name The window name
	 */
	public static void loadGeometry(Window win, String name)
	{
		int x, y, width, height;

		Dimension size = win.getSize();
		Dimension screen = win.getToolkit().getScreenSize();

		width = jEdit.getIntegerProperty(name + ".width",size.width);
		height = jEdit.getIntegerProperty(name + ".height",size.height);

		Component parent = win.getParent();
		if(parent == null)
		{
			x = (screen.width - width) / 2;
			y = (screen.height - height) / 2;
		}
		else
		{
			Rectangle bounds = parent.getBounds();
			x = bounds.x + (bounds.width - width) / 2;
			y = bounds.y + (bounds.height - height) / 2;
		}

		x = jEdit.getIntegerProperty(name + ".x",x);
		y = jEdit.getIntegerProperty(name + ".y",y);

		// Make sure the window is displayed in visible region
		if ( x + width < 0 || x > screen.width)
		{
			if (width > screen.width)
				width = screen.width;
			x = (screen.width - width) / 2;
		}
		if ( y + height < 0 || y > screen.height)
		{
			if (height >= screen.height)
				height = screen.height;
			y = (screen.height - height) / 2;
		}

		Rectangle desired = new Rectangle(x,y,width,height);
		win.setBounds(desired);

		if((win instanceof Frame) && OperatingSystem.hasJava14())
		{
			int extState = jEdit.getIntegerProperty(name +
				".extendedState", Frame.NORMAL);

			try
			{
				java.lang.reflect.Method meth =
					Frame.class.getMethod("setExtendedState",
					new Class[] {int.class});

				meth.invoke(win, new Object[] {
					new Integer(extState)});
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,GUIUtilities.class,e);
			}
		}
	} //}}}

	//{{{ saveGeometry() method
	/**
	 * Saves a window's geometry to the properties.
	 * The geometry is saved to the <code><i>name</i>.x</code>,
	 * <code><i>name</i>.y</code>, <code><i>name</i>.width</code> and
	 * <code><i>name</i>.height</code> properties.
	 * @param win The window
	 * @param name The window name
	 */
	public static void saveGeometry(Window win, String name)
	{
		if((win instanceof Frame) && OperatingSystem.hasJava14())
		{
			try
			{
				java.lang.reflect.Method meth =
					Frame.class.getMethod("getExtendedState",
					new Class[0]);

				Integer extState = (Integer)meth.invoke(win,
					new Object[0]);

				jEdit.setIntegerProperty(name + ".extendedState",
					extState.intValue());

				if(extState.intValue() != Frame.NORMAL)
					return;
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,GUIUtilities.class,e);
			}
		}

		Rectangle bounds = win.getBounds();
		jEdit.setIntegerProperty(name + ".x",bounds.x);
		jEdit.setIntegerProperty(name + ".y",bounds.y);
		jEdit.setIntegerProperty(name + ".width",bounds.width);
		jEdit.setIntegerProperty(name + ".height",bounds.height);
	} //}}}

	//}}}

	//{{{ hideSplashScreen() method
	/**
	 * Ensures that the splash screen is not visible. This should be
	 * called before displaying any dialog boxes or windows at
	 * startup.
	 */
	public static void hideSplashScreen()
	{
		if(splash != null)
		{
			splash.dispose();
			splash = null;
		}
	} //}}}

	//{{{ createMultilineLabel() method
	/**
	 * Creates a component that displays a multiple line message. This
	 * is implemented by assembling a number of <code>JLabels</code> in
	 * a <code>JPanel</code>.
	 * @param str The string, with lines delimited by newline
	 * (<code>\n</code>) characters.
	 * @since jEdit 4.1pre3
	 */
	public static JComponent createMultilineLabel(String str)
	{
		JPanel panel = new JPanel(new VariableGridLayout(
			VariableGridLayout.FIXED_NUM_COLUMNS,1,1,1));
		int lastOffset = 0;
		for(;;)
		{
			int index = str.indexOf('\n',lastOffset);
			if(index == -1)
				break;
			else
			{
				panel.add(new JLabel(str.substring(lastOffset,index)));
				lastOffset = index + 1;
			}
		}

		if(lastOffset != str.length())
			panel.add(new JLabel(str.substring(lastOffset)));

		return panel;
	} //}}}

	//{{{ requestFocus() method
	/**
	 * Focuses on the specified component as soon as the window becomes
	 * active.
	 * @param win The window
	 * @param comp The component
	 */
	public static void requestFocus(final Window win, final Component comp)
	{
		win.addWindowListener(new WindowAdapter()
		{
			public void windowActivated(WindowEvent evt)
			{
				comp.requestFocus();
				win.removeWindowListener(this);
			}
		});
	} //}}}

	//{{{ isPopupTrigger() method
	/**
	 * Returns if the specified event is the popup trigger event.
	 * This implements precisely defined behavior, as opposed to
	 * MouseEvent.isPopupTrigger().
	 * @param evt The event
	 * @since jEdit 3.2pre8
	 */
	public static boolean isPopupTrigger(MouseEvent evt)
	{
		if(OperatingSystem.isMacOS())
		{
			return (evt.isControlDown() || ((evt.getModifiers()
				& InputEvent.BUTTON2_MASK) != 0));
		}
		else
			return ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0);
	} //}}}

	//{{{ showPopupMenu() method
	/**
	 * Shows the specified popup menu, ensuring it is displayed within
	 * the bounds of the screen.
	 * @param popup The popup menu
	 * @param comp The component to show it for
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @since jEdit 4.0pre1
	 */
	public static void showPopupMenu(JPopupMenu popup, Component comp,
		int x, int y)
	{
		showPopupMenu(popup,comp,x,y,true);
	} //}}}

	//{{{ showPopupMenu() method
	/**
	 * Shows the specified popup menu, ensuring it is displayed within
	 * the bounds of the screen.
	 * @param popup The popup menu
	 * @param comp The component to show it for
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @param point If true, then the popup originates from a single point;
	 * otherwise it will originate from the component itself. This affects
	 * positioning in the case where the popup does not fit onscreen.
	 *
	 * @since jEdit 4.1pre1
	 */
	public static void showPopupMenu(JPopupMenu popup, Component comp,
		int x, int y, boolean point)
	{
		int offsetX = 0;
		int offsetY = 0;

		int extraOffset = (point ? 1 : 0);

		Component win = comp;
		while(!(win instanceof Window || win == null))
		{
			offsetX += win.getX();
			offsetY += win.getY();
			win = win.getParent();
		}

		if(win != null)
		{
			Dimension size = popup.getPreferredSize();

			Rectangle screenSize = win.getGraphicsConfiguration()
				.getBounds();

			if(x + offsetX + size.width + win.getX() > screenSize.width
				&& x + offsetX + win.getX() >= size.width)
			{
				//System.err.println("x overflow");
				if(point)
					x -= (size.width + extraOffset);
				else
					x = (win.getWidth() - size.width - offsetX + extraOffset);
			}
			else
			{
				x += extraOffset;
			}

			//System.err.println("y=" + y + ",offsetY=" + offsetY
			//	+ ",size.height=" + size.height
			//	+ ",win.height=" + win.getHeight());
			if(y + offsetY + size.height + win.getY() > screenSize.height
				&& y + offsetY + win.getY() >= size.height)
			{
				//System.err.println("y overflow");
				if(point)
					y = (win.getHeight() - size.height - offsetY + extraOffset);
				else
					y = comp.getY() - size.height - 1;
			}
			else
			{
				y += extraOffset;
			}

			popup.show(comp,x,y);
		}
		else
			popup.show(comp,x + extraOffset,y + extraOffset);

	} //}}}

	//{{{ isAncestorOf() method
	/**
	 * Returns if the first component is an ancestor of the
	 * second by traversing up the component hierarchy.
	 *
	 * @param comp1 The ancestor
	 * @param comp2 The component to check
	 * @since jEdit 4.1pre5
	 */
	public static boolean isAncestorOf(Component comp1, Component comp2)
	{
		while(comp2 != null)
		{
			if(comp1 == comp2)
				return true;
			else
				comp2 = comp2.getParent();
		}

		return false;
	} //}}}

	//{{{ getParentDialog() method
	/**
	 * Traverses the given component's parent tree looking for an
	 * instance of JDialog, and return it. If not found, return null.
	 * @param c The component
	 */
	public static JDialog getParentDialog(Component c)
	{
		Component p = c.getParent();
		while (p != null && !(p instanceof JDialog))
			p = p.getParent();

		return (p instanceof JDialog) ? (JDialog) p : null;
	} //}}}

	//{{{ getView() method
	/**
	 * Finds the view parent of the specified component.
	 * @since jEdit 4.0pre2
	 */
	public static View getView(Component comp)
	{
		for(;;)
		{
			if(comp instanceof JComponent)
			{
				Component real = (Component)((JComponent)comp)
					.getClientProperty("KORTE_REAL_FRAME");
				if(real != null)
					comp = real;
			}

			if(comp instanceof View)
				return (View)comp;
			else if(comp instanceof JPopupMenu)
				comp = ((JPopupMenu)comp).getInvoker();
			else if(comp != null)
				comp = comp.getParent();
			else
				break;
		}
		return null;
	} //}}}

	//{{{ Package-private members

	//{{{ showSplashScreen() method
	static void showSplashScreen()
	{
		splash = new SplashScreen();
	} //}}}

	//{{{ advanceSplashProgress() method
	static void advanceSplashProgress()
	{
		if(splash != null)
			splash.advance();
	} //}}}

	//}}}

	//{{{ Private members
	private static SplashScreen splash;
	private static Hashtable icons;

	private GUIUtilities() {}

	//{{{ loadPluginsMenu() method
	private static void loadPluginsMenu(JMenuBar mbar)
	{
		// Query plugins for menu items
		Vector pluginMenuItems = new Vector();

		EditPlugin[] pluginArray = jEdit.getPlugins();
		for(int i = 0; i < pluginArray.length; i++)
		{
			try
			{
				EditPlugin plugin = pluginArray[i];
				plugin.createMenuItems(pluginMenuItems);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,GUIUtilities.class,
					"Error creating menu items"
					+ " for plugin");
				Log.log(Log.ERROR,GUIUtilities.class,t);
			}
		}

		JMenu menu = new EnhancedMenu("plugins");

		if(pluginMenuItems.isEmpty())
		{
			menu.add(GUIUtilities.loadMenuItem("no-plugins"));
			mbar.add(menu);
			return;
		}

		int maxItems = jEdit.getIntegerProperty("menu.spillover",20);

		// Sort them
		MiscUtilities.quicksort(pluginMenuItems,
			new MiscUtilities.MenuItemCompare());

		if(pluginMenuItems.size() < maxItems)
		{
			for(int i = 0; i < pluginMenuItems.size(); i++)
			{
				menu.add((JMenuItem)pluginMenuItems.get(i));
			}
			mbar.add(menu);
		}
		else
		{
			int menuCount = 1;

			menu.setText(menu.getText() + " " + menuCount);

			for(int i = 0; i < pluginMenuItems.size(); i++)
			{
				menu.add((JMenuItem)pluginMenuItems.get(i));
				if(menu.getMenuComponentCount() == maxItems)
				{
					mbar.add(menu);
					menu = new JMenu(String.valueOf(
						++menuCount));
				}
			}

			if(menu.getMenuComponentCount() != 0)
				mbar.add(menu);
		}
	} //}}}

	//}}}
}
