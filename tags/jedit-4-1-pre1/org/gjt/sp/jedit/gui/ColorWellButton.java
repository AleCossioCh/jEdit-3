/*
 * ColorWellButton.java - Shows color chooser when clicked
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Slava Pestov
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
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.OperatingSystem;
//}}}

/**
 * A button that, when clicked, shows a color chooser.
 *
 * You can get and set the currently selected color using
 * <code>getSelectedColor()</code> and <code>setSelectedColor().
 * @author Slava Pestov
 * @version $Id: ColorWellButton.java 4209 2002-05-30 10:17:45Z spestov $
 */
public class ColorWellButton extends JButton
{
	//{{{ ColorWellButton constructor
	public ColorWellButton(Color color)
	{
		setIcon(new ColorWell(color));
		setMargin(new Insets(2,2,2,2));
		addActionListener(new ActionHandler());

		// according to krisk this looks better on OS X...
		if(OperatingSystem.isMacOSLF())
			putClientProperty("JButton.buttonType","toolbar");
	} //}}}

	//{{{ getSelectedColor() method
	public Color getSelectedColor()
	{
		return ((ColorWell)getIcon()).color;
	} //}}}

	//{{{ setSelectedColor() method
	public void setSelectedColor(Color color)
	{
		((ColorWell)getIcon()).color = color;
		repaint();
	} //}}}

	//{{{ ColorWell class
	static class ColorWell implements Icon
	{
		Color color;

		ColorWell(Color color)
		{
			this.color = color;
		}

		public int getIconWidth()
		{
			return 25;
		}

		public int getIconHeight()
		{
			return 13;
		}

		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			if(color == null)
				return;

			g.setColor(color);
			g.fillRect(x,y,getIconWidth(),getIconHeight());
			g.setColor(color.darker());
			g.drawRect(x,y,getIconWidth()-1,getIconHeight()-1);
		}
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Color color = JColorChooser.showDialog(
				ColorWellButton.this,
				jEdit.getProperty("colorChooser.title"),
				getSelectedColor());
			if(color != null)
				setSelectedColor(color);
		}
	} //}}}
}
