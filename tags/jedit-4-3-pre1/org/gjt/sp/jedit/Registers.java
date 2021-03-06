/*
 * Registers.java - Register manager
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
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
import com.microstar.xml.*;
import java.awt.datatransfer.*;
import java.awt.Toolkit;
import java.io.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.RegisterChanged;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * jEdit's registers are an extension of the clipboard metaphor.<p>
 *
 * A {@link Registers.Register} is string of text indexed by a
 * single character. Typically the text is taken from selected buffer text
 * and the index character is a keyboard character selected by the user.<p>
 *
 * This class defines a number of static methods
 * that give each register the properties of a virtual clipboard.<p>
 *
 * Two classes implement the {@link Registers.Register} interface. A
 * {@link Registers.ClipboardRegister} is tied to the contents of the
 * system clipboard. jEdit assigns a
 * {@link Registers.ClipboardRegister} to the register indexed under
 * the character <code>$</code>. A
 * {@link Registers.StringRegister} is created for registers assigned
 * by the user. In addition, jEdit assigns <code>%</code> to
 * the last text segment selected in the text area. On Windows this is a
 * {@link Registers.StringRegister}, on Unix under Java 2 version 1.4, a
 * {@link Registers.ClipboardRegister}.
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id: Registers.java 5154 2004-12-15 04:09:03Z spestov $
 */
public class Registers
{
	//{{{ copy() method
	/**
	 * Copies the text selected in the text area into the specified register.
	 * This will replace the existing contents of the designated register.
	 *
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void copy(JEditTextArea textArea, char register)
	{
		String selection = textArea.getSelectedText();
		if(selection == null)
			return;

		setRegister(register,selection);
		HistoryModel.getModel("clipboard").addItem(selection);
		EditBus.send(new RegisterChanged(null,register));
	
	} //}}}

	//{{{ cut() method
	/**
	 * Copies the text selected in the text area into the specified
	 * register, and then removes it from the buffer.
	 *
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void cut(JEditTextArea textArea, char register)
	{
		if(textArea.isEditable())
		{
			String selection = textArea.getSelectedText();
			if(selection == null)
				return;

			setRegister(register,selection);
			HistoryModel.getModel("clipboard").addItem(selection);

			textArea.setSelectedText("");
			EditBus.send(new RegisterChanged(null,register));
		}
		else
			textArea.getToolkit().beep();
	} //}}}

	//{{{ append() method
	/**
	 * Appends the text selected in the text area to the specified register,
	 * with a newline between the old and new text.
	 * @param textArea The text area
	 * @param register The register
	 */
	public static void append(JEditTextArea textArea, char register)
	{
		append(textArea,register,"\n",false);
	} //}}}

	//{{{ append() method
	/**
	 * Appends the text selected in the text area to the specified register.
	 * @param textArea The text area
	 * @param register The register
	 * @param separator The separator to insert between the old and new text
	 */
	public static void append(JEditTextArea textArea, char register,
		String separator)
	{
		append(textArea,register,separator,false);
	} //}}}

	//{{{ append() method
	/**
	 * Appends the text selected in the  text area to the specified register.
	 * @param textArea The text area
	 * @param register The register
	 * @param separator The text to insert between the old and new text
	 * @param cut Should the current selection be removed?
	 * @since jEdit 3.2pre1
	 */
	public static void append(JEditTextArea textArea, char register,
		String separator, boolean cut)
	{
		if(cut && !textArea.isEditable())
		{
			textArea.getToolkit().beep();
			return;
		}

		String selection = textArea.getSelectedText();
		if(selection == null)
			return;

		Register reg = getRegister(register);

		if(reg != null)
		{
			String registerContents = reg.toString();
			if(registerContents != null)
			{
				if(registerContents.endsWith(separator))
					selection = registerContents + selection;
				else
					selection = registerContents + separator + selection;
			}
		}

		setRegister(register,selection);
		HistoryModel.getModel("clipboard").addItem(selection);

		if(cut)
			textArea.setSelectedText("");
	} //}}}

	//{{{ paste() method
	/**
	 * Insets the contents of the specified register into the text area.
	 * @param textArea The text area
	 * @param register The register
	 * @since jEdit 2.7pre2
	 */
	public static void paste(JEditTextArea textArea, char register)
	{
		paste(textArea,register,false);
	} //}}}

	//{{{ paste() method
	/**
	 * Inserts the contents of the specified register into the text area.
	 * @param textArea The text area
	 * @param register The register
	 * @param vertical Vertical (columnar) paste
	 * @since jEdit 4.1pre1
	 */
	public static void paste(JEditTextArea textArea, char register,
		boolean vertical)
	{
		if(!textArea.isEditable())
		{
			textArea.getToolkit().beep();
			return;
		}

		Register reg = getRegister(register);

		if(reg == null)
		{
			textArea.getToolkit().beep();
			return;
		}
		else
		{
			String selection = reg.toString();
			if(selection == null)
			{
				textArea.getToolkit().beep();
				return;
			}

			if(vertical && textArea.getSelectionCount() == 0)
			{
				Buffer buffer = textArea.getBuffer();

				try
				{
					buffer.beginCompoundEdit();

					int caret = textArea.getCaretPosition();
					int caretLine = textArea.getCaretLine();
					Selection.Rect rect = new Selection.Rect(
						caretLine,caret,caretLine,caret);
					textArea.setSelectedText(rect,selection);
					caretLine = textArea.getCaretLine();

					if(caretLine != textArea.getLineCount() - 1)
					{
						int startColumn = rect.getStartColumn(
							buffer);
						int offset = buffer
							.getOffsetOfVirtualColumn(
							caretLine + 1,startColumn,null);
						if(offset == -1)
						{
							buffer.insertAtColumn(caretLine + 1,startColumn,"");
							textArea.setCaretPosition(
								buffer.getLineEndOffset(
								caretLine + 1) - 1);
						}
						else
						{
							textArea.setCaretPosition(
								buffer.getLineStartOffset(
								caretLine + 1) + offset);
						}
					}
				}
				finally
				{
					buffer.endCompoundEdit();
				}
			}
			else
				textArea.setSelectedText(selection);

			HistoryModel.getModel("clipboard").addItem(selection);
		}
	} //}}}

	//{{{ getRegister() method
	/**
	 * Returns the specified register.
	 * @param name The name
	 */
	public static Register getRegister(char name)
	{
		if(name != '$' && name != '%')
		{
			if(!loaded)
				loadRegisters();
		}

		if(registers == null || name >= registers.length)
			return null;
		else
			return registers[name];
	} //}}}

	//{{{ setRegister() method
	/**
	 * Sets the specified register.
	 * @param name The name
	 * @param newRegister The new value
	 */
	public static void setRegister(char name, Register newRegister)
	{
		touchRegister(name);

		if(name >= registers.length)
		{
			Register[] newRegisters = new Register[
				Math.min(1<<16,name * 2)];
			System.arraycopy(registers,0,newRegisters,0,
				registers.length);
			registers = newRegisters;
		}

		registers[name] = newRegister;
		EditBus.send(new RegisterChanged(null,name));
	} //}}}

	//{{{ setRegister() method
	/**
	 * Sets the specified register.
	 * @param name The name
	 * @param value The new value
	 */
	public static void setRegister(char name, String value)
	{
		touchRegister(name);
		Register register = getRegister(name);
		if(register != null)
			register.setValue(value);
		else
			setRegister(name,new StringRegister(value));
	} //}}}

	//{{{ clearRegister() method
	/**
	 * Sets the value of the specified register to <code>null</code>.
	 * @param name The register name
	 */
	public static void clearRegister(char name)
	{
		if(name >= registers.length)
			return;

		Register register = registers[name];
		if(name == '$' || name == '%')
			register.setValue("");
		else
			registers[name] = null;
	} //}}}

	//{{{ getRegisters() method
	/**
	 * Returns an array of all available registers. Some of the elements
	 * of this array might be <code>null</code>.
	 */
	public static Register[] getRegisters()
	{
		if(!loaded)
			loadRegisters();
		return registers;
	} //}}}

	//{{{ getRegisterStatusPrompt() method
	/**
	 * Returns the status prompt for the given register action. Only
	 * intended to be called from <code>actions.xml</code>.
	 * @since jEdit 4.2pre2
	 */
	public static String getRegisterStatusPrompt(String action)
	{
		return jEdit.getProperty("view.status." + action,
			new String[] { getRegisterNameString() });
	} //}}}

	//{{{ getRegisterNameString() method
	/**
	 * Returns a string of all defined registers, used by the status bar
	 * (eg, "a b $ % ^").
	 * @since jEdit 4.2pre2
	 */
	public static String getRegisterNameString()
	{
		if(!loaded)
			loadRegisters();

		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < registers.length; i++)
		{
			if(registers[i] != null)
			{
				if(buf.length() != 0)
					buf.append(' ');
				buf.append((char)i);
			}
		}

		if(buf.length() == 0)
			return jEdit.getProperty("view.status.no-registers");
		else
			return buf.toString();
	} //}}}

	//{{{ saveRegisters() method
	public static void saveRegisters()
	{
		if(!loaded || !modified)
			return;

		Log.log(Log.MESSAGE,Registers.class,"Saving registers.xml");
		File file1 = new File(MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(), "#registers.xml#save#"));
		File file2 = new File(MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(), "registers.xml"));
		if(file2.exists() && file2.lastModified() != registersModTime)
		{
			Log.log(Log.WARNING,Registers.class,file2 + " changed"
				+ " on disk; will not save registers");
			return;
		}

		jEdit.backupSettingsFile(file2);

		String lineSep = System.getProperty("line.separator");

		BufferedWriter out = null;

		boolean ok = false;

		try
		{
			out = new BufferedWriter(new FileWriter(file1));

			out.write("<?xml version=\"1.0\"?>");
			out.write(lineSep);
			out.write("<!DOCTYPE REGISTERS SYSTEM \"registers.dtd\">");
			out.write(lineSep);
			out.write("<REGISTERS>");
			out.write(lineSep);

			Register[] registers = getRegisters();
			for(int i = 0; i < registers.length; i++)
			{
				Register register = registers[i];
				if(register == null || i == '$' || i == '%')
					continue;

				out.write("<REGISTER NAME=\"");
				if(i == '"')
					out.write("&quot;");
				else
					out.write((char)i);
				out.write("\">");

				out.write(MiscUtilities.charsToEntities(
					register.toString()));

				out.write("</REGISTER>");
				out.write(lineSep);
			}

			out.write("</REGISTERS>");
			out.write(lineSep);

			ok = true;
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,Registers.class,e);
		}
		finally
		{
			try
			{
				if(out != null)
					out.close();
			}
			catch(IOException e)
			{
			}
		}

		if(ok)
		{
			/* to avoid data loss, only do this if the above
			 * completed successfully */
			file2.delete();
			file1.renameTo(file2);
		}

		registersModTime = file2.lastModified();
		modified = false;
	} //}}}

	//{{{ Private members
	private static Register[] registers;
	private static long registersModTime;
	private static boolean loaded, loading, modified;

	private Registers() {}

	static
	{
		registers = new Register[256];
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		registers['$'] = new ClipboardRegister(
			toolkit.getSystemClipboard());
		Clipboard selection = toolkit.getSystemSelection();
		if(selection != null)
			registers['%'] = new ClipboardRegister(selection);
	}

	//{{{ touchRegister() method
	private static void touchRegister(char name)
	{
		if(name == '%' || name == '$')
			return;

		if(!loaded)
			loadRegisters();

		if(!loading)
			modified = true;
	} //}}}
	
	//{{{ loadRegisters() method
	private static void loadRegisters()
	{
		loaded = true;

		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			return;

		File registerFile = new File(MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(),"registers.xml"));
		if(!registerFile.exists())
			return;

		registersModTime = registerFile.lastModified();

		Log.log(Log.MESSAGE,jEdit.class,"Loading registers.xml");

		RegistersHandler handler = new RegistersHandler();
		XmlParser parser = new XmlParser();
		parser.setHandler(handler);
		Reader in = null;
		try
		{
			loading = true;
			in = new BufferedReader(new FileReader(registerFile));
			parser.parse(null, null, in);
		}
		catch(XmlException xe)
		{
			int line = xe.getLine();
			String message = xe.getMessage();
			Log.log(Log.ERROR,Registers.class,registerFile + ":"
				+ line + ": " + message);
		}
		catch(FileNotFoundException fnf)
		{
			//Log.log(Log.DEBUG,Registers.class,fnf);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,Registers.class,e);
		}
		finally
		{
			loading = false;
			try
			{
				if(in != null)
					in.close();
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,Registers.class,io);
			}
		}
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ Register interface
	/**
	 * A register.
	 */
	public interface Register
	{
		/**
		 * Converts to a string.
		 */
		String toString();

		/**
		 * Sets the register contents.
		 */
		void setValue(String value);
	} //}}}

	//{{{ ClipboardRegister class
	/**
	 * A clipboard register. Register "$" should always be an
	 * instance of this.
	 */
	public static class ClipboardRegister implements Register
	{
		Clipboard clipboard;

		public ClipboardRegister(Clipboard clipboard)
		{
			this.clipboard = clipboard;
		}

		/**
		 * Sets the clipboard contents.
		 */
		public void setValue(String value)
		{
			StringSelection selection = new StringSelection(value);
			clipboard.setContents(selection,null);
		}

		/**
		 * Returns the clipboard contents.
		 */
		public String toString()
		{
			try
			{
				String selection = (String)(clipboard
					.getContents(this).getTransferData(
					DataFlavor.stringFlavor));

				boolean trailingEOL = (selection.endsWith("\n")
					|| selection.endsWith(System.getProperty(
					"line.separator")));

				// Some Java versions return the clipboard
				// contents using the native line separator,
				// so have to convert it here
				BufferedReader in = new BufferedReader(
					new StringReader(selection));
				StringBuffer buf = new StringBuffer();
				String line;
				while((line = in.readLine()) != null)
				{
					// broken Eclipse workaround!
					// 24 Febuary 2004
					if(line.endsWith("\0"))
					{
						line = line.substring(0,
							line.length() - 1);
					}
					buf.append(line);
					buf.append('\n');
				}
				// remove trailing \n
				if(!trailingEOL && buf.length() != 0)
					buf.setLength(buf.length() - 1);
				return buf.toString();
			}
			catch(Exception e)
			{
				Log.log(Log.NOTICE,this,e);
				return null;
			}
		}
	} //}}}

	//{{{ StringRegister class
	/**
	 * Register that stores a string.
	 */
	public static class StringRegister implements Register
	{
		private String value;

		/**
		 * Creates a new string register.
		 * @param value The contents
		 */
		public StringRegister(String value)
		{
			this.value = value;
		}

		/**
		 * Sets the register contents.
		 */
		public void setValue(String value)
		{
			this.value = value;
		}

		/**
		 * Converts to a string.
		 */
		public String toString()
		{
			return value;
		}

		/**
		 * Called when this register is no longer available. This
		 * implementation does nothing.
		 */
		public void dispose() {}
	} //}}}

	//{{{ RegistersHandler class
	static class RegistersHandler extends HandlerBase
	{
		//{{{ resolveEntity() method
		public Object resolveEntity(String publicId, String systemId)
		{
			if("registers.dtd".equals(systemId))
			{
				// this will result in a slight speed up, since we
				// don't need to read the DTD anyway, as AElfred is
				// non-validating
				return new StringReader("<!-- -->");

				/* try
				{
					return new BufferedReader(new InputStreamReader(
						getClass().getResourceAsStream("registers.dtd")));
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,this,"Error while opening"
						+ " recent.dtd:");
					Log.log(Log.ERROR,this,e);
				} */
			}

			return null;
		} //}}}

		//{{{ attribute() method
		public void attribute(String aname, String value, boolean isSpecified)
		{
			if(aname.equals("NAME"))
				registerName = value;
		} //}}}

		//{{{ doctypeDecl() method
		public void doctypeDecl(String name, String publicId,
			String systemId) throws Exception
		{
			if("REGISTERS".equals(name))
				return;

			Log.log(Log.ERROR,this,"registers.xml: DOCTYPE must be REGISTERS");
		} //}}}

		//{{{ endElement() method
		public void endElement(String name)
		{
			if(name.equals("REGISTER"))
			{
				if(registerName == null || registerName.length() != 1)
					Log.log(Log.ERROR,this,"Malformed NAME: " + registerName);
				else
					setRegister(registerName.charAt(0),charData);
			}
		} //}}}

		//{{{ charData() method
		public void charData(char[] ch, int start, int length)
		{
			charData = new String(ch,start,length);
		} //}}}

		//{{{ Private members
		private String registerName;
		private String charData;
		//}}}
	} //}}}

	//}}}
}
