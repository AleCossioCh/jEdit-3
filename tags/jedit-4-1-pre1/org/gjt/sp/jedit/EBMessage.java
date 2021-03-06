/*
 * EBMessage.java - An EditBus message
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

/**
 * The base class of all EditBus messages.
 *
 * @author Slava Pestov
 * @version $Id: EBMessage.java 4152 2002-05-14 07:55:49Z spestov $
 *
 * @since jEdit 2.2pre6
 */
public abstract class EBMessage
{
	//{{{ EBMessage constructor
	/**
	 * Creates a new message.
	 * @param source The message source
	 */
	public EBMessage(EBComponent source)
	{
		this.source = source;
	} //}}}

	//{{{ getSource() method
	/**
	 * Returns the sender of this message.
	 */
	public EBComponent getSource()
	{
		return source;
	} //}}}

	//{{{ toString() method
	/**
	 * Returns a string representation of this message.
	 */
	public String toString()
	{
		return getClass().getName() + "[" + paramString() + "]";
	} //}}}

	//{{{ paramString() method
	/**
	 * Returns a string representation of this message's parameters.
	 */
	public String paramString()
	{
		return "source=" + source;
	} //}}}

	//{{{ Private members
	private EBComponent source;
	//}}}

	//{{{ Deprecated methods
	/**
	 * @deprecated Does nothing.
	 */
	public void veto()
	{
	}

	/**
	 * @deprecated Returns false.
	 */
	public boolean isVetoed()
	{
		return false;
	}

	/**
	 * @deprecated Subclass <code>EBMessage</code> instead.
	 */
	public static abstract class NonVetoable extends EBMessage
	{
		/**
		 * Creates a new non-vetoable message.
		 * @param source The message source
		 */
		public NonVetoable(EBComponent source)
		{
			super(source);
		}

		/**
		 * Disallows this message from being vetoed.
		 */
		public void veto()
		{
			throw new InternalError("Can't veto this message");
		}
	} //}}}
}
