/*
 * StatusListener.java - Text area scroll listener
 * Copyright (C) 2005 Slava Pestov
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

package org.gjt.sp.jedit.textarea;

/**
 * Fired when the text area needs to display a status message.
 * @author Slava Pestov
 * @version $Id: StatusListener.java 12504 2008-04-22 23:12:43Z ezust $
 * @since jEdit 4.3pre2
 */
public interface StatusListener extends java.util.EventListener
{
	int OVERWRITE_CHANGED = 0;
	int MULTI_SELECT_CHANGED = 1;
	int RECT_SELECT_CHANGED = 2;

	void statusChanged(TextArea textArea, int flag, boolean value);
	
	void bracketSelected(TextArea textArea, int line, String text);
	
	void narrowActive(TextArea textArea);
}
