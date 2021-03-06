/*
 * PluginList.java - Plugin list downloaded from server
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
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
import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.gjt.sp.util.Log;
import org.gjt.sp.jedit.*;
//}}}

/**
 * Plugin list downloaded from server.
 * @since jEdit 3.2pre2
 * @version $Id: PluginList.java 5445 2006-06-19 01:09:51Z vanza $
 */
class PluginList
{
	/**
	 * Magic numbers used for auto-detecting GZIP files.
	 */
	public static final int GZIP_MAGIC_1 = 0x1f;
	public static final int GZIP_MAGIC_2 = 0x8b;

	Vector plugins;
	Hashtable pluginHash;
	Vector pluginSets;

	/**
	 * The mirror id.
	 * @since jEdit 4.3pre3
	 */
	private final String id;

	//{{{ PluginList constructor
	PluginList() throws Exception
	{
		plugins = new Vector();
		pluginHash = new Hashtable();
		pluginSets = new Vector();

		String path = jEdit.getProperty("plugin-manager.export-url");
		id = jEdit.getProperty("plugin-manager.mirror.id");
		if (!id.equals(MirrorList.Mirror.NONE))
			path += "?mirror="+id;
		PluginListHandler handler = new PluginListHandler(this,path);
		XMLReader parser = XMLReaderFactory.createXMLReader();
		InputStream in = new BufferedInputStream(new URL(path).openStream());
		try
		{
			if(in.markSupported())
			{
				in.mark(2);
				int b1 = in.read();
				int b2 = in.read();
				in.reset();

				if(b1 == GZIP_MAGIC_1 && b2 == GZIP_MAGIC_2)
					in = new GZIPInputStream(in);
			}

			InputSource isrc = new InputSource(new InputStreamReader(in,"UTF8"));
			isrc.setSystemId("jedit.jar");
			parser.setContentHandler(handler);
			parser.setDTDHandler(handler);
			parser.setEntityResolver(handler);
			parser.setErrorHandler(handler);
			parser.parse(isrc);
		}
		finally
		{
			in.close();
		}
	} //}}}

	//{{{ addPlugin() method
	void addPlugin(Plugin plugin)
	{
		plugin.checkIfInstalled();
		plugins.addElement(plugin);
		pluginHash.put(plugin.name,plugin);
	} //}}}

	//{{{ addPluginSet() method
	void addPluginSet(PluginSet set)
	{
		pluginSets.addElement(set);
	} //}}}

	//{{{ finished() method
	void finished()
	{
		// after the entire list is loaded, fill out plugin field
		// in dependencies
		for(int i = 0; i < plugins.size(); i++)
		{
			Plugin plugin = (Plugin)plugins.elementAt(i);
			for(int j = 0; j < plugin.branches.size(); j++)
			{
				Branch branch = (Branch)plugin.branches.elementAt(j);
				for(int k = 0; k < branch.deps.size(); k++)
				{
					Dependency dep = (Dependency)branch.deps.elementAt(k);
					if(dep.what.equals("plugin"))
						dep.plugin = (Plugin)pluginHash.get(dep.pluginName);
				}
			}
		}
	} //}}}

	//{{{ dump() method
	void dump()
	{
		for(int i = 0; i < plugins.size(); i++)
		{
			System.err.println(plugins.elementAt(i));
			System.err.println();
		}
	} //}}}

	//{{{ getMirrorId() method
	/**
	 * Returns the mirror ID.
	 *
	 * @return the mirror ID
	 * @since jEdit 4.3pre3
	 */
	String getMirrorId()
	{
		return id;
	} //}}}

	//{{{ PluginSet class
	static class PluginSet
	{
		String name;
		String description;
		Vector plugins = new Vector();

		public String toString()
		{
			return plugins.toString();
		}
	} //}}}

	//{{{ Plugin class
	static public class Plugin
	{
		String jar;
		String name;
		String description;
		String author;
		Vector branches = new Vector();
		//String installed;
		//String installedVersion;

		void checkIfInstalled()
		{
			/* // check if the plugin is already installed.
			// this is a bit of hack
			PluginJAR[] jars = jEdit.getPluginJARs();
			for(int i = 0; i < jars.length; i++)
			{
				String path = jars[i].getPath();
				if(!new File(path).exists())
					continue;

				if(MiscUtilities.getFileName(path).equals(jar))
				{
					installed = path;

					EditPlugin plugin = jars[i].getPlugin();
					if(plugin != null)
					{
						installedVersion = jEdit.getProperty(
							"plugin." + plugin.getClassName()
							+ ".version");
					}
					break;
				}
			}

			String[] notLoaded = jEdit.getNotLoadedPluginJARs();
			for(int i = 0; i < notLoaded.length; i++)
			{
				String path = notLoaded[i];

				if(MiscUtilities.getFileName(path).equals(jar))
				{
					installed = path;
					break;
				}
			} */
		}

		String getInstalledVersion()
		{
			PluginJAR[] jars = jEdit.getPluginJARs();
			for(int i = 0; i < jars.length; i++)
			{
				String path = jars[i].getPath();

				if(MiscUtilities.getFileName(path).equals(jar))
				{
					EditPlugin plugin = jars[i].getPlugin();
					if(plugin != null)
					{
						return jEdit.getProperty(
							"plugin." + plugin.getClassName()
							+ ".version");
					}
					else
						return null;
				}
			}

			return null;
		}

		String getInstalledPath()
		{
			PluginJAR[] jars = jEdit.getPluginJARs();
			for(int i = 0; i < jars.length; i++)
			{
				String path = jars[i].getPath();

				if(MiscUtilities.getFileName(path).equals(jar))
					return path;
			}

			return null;
		}

		/**
		 * Find the first branch compatible with the running jEdit release.
		 */
		Branch getCompatibleBranch()
		{
			for(int i = 0; i < branches.size(); i++)
			{
				Branch branch = (Branch)branches.elementAt(i);
				if(branch.canSatisfyDependencies())
					return branch;
			}

			return null;
		}

		boolean canBeInstalled()
		{
			Branch branch = getCompatibleBranch();
			return branch != null && !branch.obsolete
				&& branch.canSatisfyDependencies();
		}

		void install(Roster roster, String installDirectory, boolean downloadSource)
		{
			String installed = getInstalledPath();

			Branch branch = getCompatibleBranch();
			if(branch.obsolete)
			{
				if(installed != null)
					roster.addRemove(installed);
				return;
			}

			//branch.satisfyDependencies(roster,installDirectory,
			//	downloadSource);

			if(installed != null)
			{
				installDirectory = MiscUtilities.getParentOfPath(
					installed);
			}

			roster.addInstall(
				installed,
				(downloadSource ? branch.downloadSource : branch.download),
				installDirectory,
				(downloadSource ? branch.downloadSourceSize : branch.downloadSize));

		}

		public String toString()
		{
			return name;
		}
	} //}}}

	//{{{ Branch class
	static class Branch
	{
		String version;
		String date;
		int downloadSize;
		String download;
		int downloadSourceSize;
		String downloadSource;
		boolean obsolete;
		Vector deps = new Vector();

		boolean canSatisfyDependencies()
		{
			for(int i = 0; i < deps.size(); i++)
			{
				Dependency dep = (Dependency)deps.elementAt(i);
				if(!dep.canSatisfy())
					return false;
			}

			return true;
		}

		void satisfyDependencies(Roster roster, String installDirectory,
			boolean downloadSource)
		{
			for(int i = 0; i < deps.size(); i++)
			{
				Dependency dep = (Dependency)deps.elementAt(i);
				dep.satisfy(roster,installDirectory,downloadSource);
			}
		}

		public String toString()
		{
			return "[version=" + version + ",download=" + download
				+ ",obsolete=" + obsolete + ",deps=" + deps + "]";
		}
	} //}}}

	//{{{ Dependency class
	static class Dependency
	{
		String what;
		String from;
		String to;
		// only used if what is "plugin"
		String pluginName;
		Plugin plugin;

		Dependency(String what, String from, String to, String pluginName)
		{
			this.what = what;
			this.from = from;
			this.to = to;
			this.pluginName = pluginName;
		}

		boolean isSatisfied()
		{
			if(what.equals("plugin"))
			{
				for(int i = 0; i < plugin.branches.size(); i++)
				{
					String installedVersion = plugin.getInstalledVersion();
					if(installedVersion != null
						&&
					(from == null || MiscUtilities.compareStrings(
						installedVersion,from,false) >= 0)
						&&
					   (to == null || MiscUtilities.compareStrings(
					   	installedVersion,to,false) <= 0))
					{
						return true;
					}
				}

				return false;
			}
			else if(what.equals("jdk"))
			{
				String javaVersion = System.getProperty("java.version").substring(0,3);

				if((from == null || MiscUtilities.compareStrings(
					javaVersion,from,false) >= 0)
					&&
				   (to == null || MiscUtilities.compareStrings(
				   	javaVersion,to,false) <= 0))
					return true;
				else
					return false;
			}
			else if(what.equals("jedit"))
			{
				String build = jEdit.getBuild();

				if((from == null || MiscUtilities.compareStrings(
					build,from,false) >= 0)
					&&
				   (to == null || MiscUtilities.compareStrings(
				   	build,to,false) <= 0))
					return true;
				else
					return false;
			}
			else
			{
				Log.log(Log.ERROR,this,"Invalid dependency: " + what);
				return false;
			}
		}

		boolean canSatisfy()
		{
			if(isSatisfied())
				return true;
			else if(what.equals("plugin"))
			{
				return plugin.canBeInstalled();
			}
			else
				return false;
		}

		void satisfy(Roster roster, String installDirectory,
			boolean downloadSource)
		{
			if(what.equals("plugin"))
			{
				String installedVersion = plugin.getInstalledVersion();
				for(int i = 0; i < plugin.branches.size(); i++)
				{
					Branch branch = (Branch)plugin.branches
						.elementAt(i);
					if((installedVersion == null
						||
					MiscUtilities.compareStrings(
						installedVersion,branch.version,false) < 0)
						&&
					(from == null || MiscUtilities.compareStrings(
						branch.version,from,false) >= 0)
						&&
					   (to == null || MiscUtilities.compareStrings(
					   	branch.version,to,false) <= 0))
					{
						plugin.install(roster,installDirectory,
							downloadSource);
						return;
					}
				}
			}
		}

		public String toString()
		{
			return "[what=" + what + ",from=" + from
				+ ",to=" + to + ",plugin=" + plugin + "]";
		}
	} //}}}
}
