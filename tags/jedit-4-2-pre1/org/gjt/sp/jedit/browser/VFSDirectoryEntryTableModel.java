/*
 * VFSDirectoryEntryTableModel.java - VFS directory entry table model
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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

package org.gjt.sp.jedit.browser;

import javax.swing.table.*;
import javax.swing.*;
import java.util.*;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.*;

/**
 * @author Slava Pestov
 * @version $Id: VFSDirectoryEntryTableModel.java 4648 2003-04-25 06:09:49Z spestov $
 * @since jEdit 4.2pre1
 */
public class VFSDirectoryEntryTableModel extends AbstractTableModel
{
	//{{{ VFSDirectoryEntryTableModel constructor
	public VFSDirectoryEntryTableModel()
	{
		extAttrs = new String[] {
			VFS.EA_SIZE_OR_TYPE,
			VFS.EA_STATUS,
			VFS.EA_MODIFIED
		};
	} //}}}

	//{{{ setRoot() method
	public void setRoot(ArrayList list)
	{
		if(files != null && files.length != 0)
			fireTableRowsDeleted(0,files.length - 1);

		files = new Entry[list.size()];
		for(int i = 0; i < files.length; i++)
		{
			files[i] = new Entry((VFS.DirectoryEntry)list.get(i),0);
		}

		if(files.length != 0)
			fireTableRowsInserted(0,files.length - 1);
	} //}}}

	//{{{ expand() method
	public int expand(Entry entry, ArrayList list)
	{
		int startIndex = -1;
		for(int i = 0; i < files.length; i++)
		{
			if(files[i] == entry)
				startIndex = i;
		}

		collapse(startIndex);

		entry.expanded = true;

		if(list != null)
		{
			Entry[] newFiles = new Entry[files.length + list.size()];
			System.arraycopy(files,0,newFiles,0,startIndex + 1);
			for(int i = 0; i < list.size(); i++)
			{
				newFiles[startIndex + i + 1] = new Entry(
					(VFS.DirectoryEntry)list.get(i),
					entry.level + 1);
			}
			System.arraycopy(files,startIndex + 1,
				newFiles,startIndex + list.size() + 1,
				files.length - startIndex - 1);
			this.files = newFiles;

			fireTableRowsInserted(startIndex + 1,
				startIndex + list.size() + 1);
		}

		fireTableRowsUpdated(startIndex,startIndex);

		return startIndex;
	} //}}}

	//{{{ collapse() method
	public void collapse(int index)
	{
		Entry entry = files[index];
		if(!entry.expanded)
			return;

		entry.expanded = false;

		int lastIndex = index + 1;
		for(;;)
		{
			Entry e = files[lastIndex];
			if(e.level <= entry.level)
				break;
			else
				lastIndex++;
		}

		Entry[] newFiles = new Entry[files.length - lastIndex + index + 1];
		System.arraycopy(files,0,newFiles,0,index + 1);
		System.arraycopy(files,lastIndex,newFiles,index + 1,
			files.length - lastIndex);

		files = newFiles;

		fireTableRowsUpdated(index,index);
		fireTableRowsDeleted(index + 1,lastIndex);
	} //}}}

	//{{{ getColumnCount() method
	public int getColumnCount()
	{
		return 2 + extAttrs.length;
	} //}}}

	//{{{ getRowCount() method
	public int getRowCount()
	{
		if(files == null)
			return 0;
		else
			return files.length;
	} //}}}

	//{{{ getColumnName() method
	public String getColumnName(int col)
	{
		if(col == 0)
			return null;
		else if(col == 1)
			return jEdit.getProperty("vfs.browser.name");
		else
			return jEdit.getProperty("vfs.browser." + getExtendedAttribute(col));
	} //}}}

	//{{{ getColumnClass() method
	public Class getColumnClass(int col)
	{
		return Entry.class;
	} //}}}

	//{{{ getValueAt() method
	public Object getValueAt(int row, int col)
	{
		if(files == null)
			return null;
		else
			return files[row];
	} //}}}

	//{{{ getExtendedAttribute() method
	public String getExtendedAttribute(int index)
	{
		return extAttrs[index - 2];
	} //}}}

	//{{{ Package-private members
	Entry[] files;
	//}}}

	//{{{ Private members
	private String[] extAttrs;
	//}}}

	//{{{ Entry class
	static class Entry
	{
		VFS.DirectoryEntry dirEntry;
		boolean expanded;
		// how deeply we are nested
		int level;

		Entry(VFS.DirectoryEntry dirEntry, int level)
		{
			this.dirEntry = dirEntry;
			this.level = level;
		}
	} //}}}
}
