/*
 * SearchMatcher.java - Abstract string matcher interface
 * Copyright (C) 1999, 2001 Slava Pestov
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

package org.gjt.sp.jedit.search;

import javax.swing.text.Segment;

/**
 * An abstract interface for matching strings.
 * @author Slava Pestov
 * @version $Id: SearchMatcher.java 3827 2001-10-05 08:55:14Z spestov $
 */
public interface SearchMatcher
{
	/**
	 * Returns the offset of the first match of the specified text
	 * within this matcher.
	 * @param text The text to search in
	 * @param start True if the start of the segment is the beginning of the
	 * buffer
	 * @param end True if the end of the segment is the end of the buffer
	 * @return an array where the first element is the start offset
	 * of the match, and the second element is the end offset of
	 * the match
	 * @since jEdit 4.0pre1
	 */
	int[] nextMatch(Segment text, boolean start, boolean end);

	/**
	 * Returns the specified text, with any substitution specified
	 * within this matcher performed.
	 * @param text The text
	 * @return The changed string
	 */
	String substitute(String text) throws Exception;
}
