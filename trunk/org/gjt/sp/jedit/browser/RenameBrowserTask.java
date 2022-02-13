package org.gjt.sp.jedit.browser;

import org.experiment.editor.OperatingSystem;
import org.experiment.editor.io.VFS;
import org.experiment.editor.io.VFSFile;
import org.experiment.editor.io.VFSManager;
import org.experiment.editor.Editor;
import org.experiment.util.Log;

import java.io.IOException;

/**
 * @author John Doe
 */
class RenameBrowserTask extends AbstractBrowserTask{
    /**
	 * Creates a new browser I/O request.
	 *
	 * @param browser  The VFS browser instance
	 * @param path1    The first path name to operate on
	 * @param path2    The second path name to operate on
	 */
	RenameBrowserTask(VFSBrowser browser,
    Object session, VFS vfs, String path1, String path2,
   Runnable awtRunnable)
    {
        super(browser, session, vfs, path1, awtRunnable);
        this.path2 = path1;
    }

    @Override
    public void _run()
    {
        try
        {
            setCancellable(true);
            String[] args = {path, path2};
            setStatus(Editor.getProperty("vfs.status.renaming", args));

            path = vfs._canonPath(session, path, browser);
            path2 = vfs._canonPath(session, path2, browser);

            VFSFile file = vfs._getFile(session, path2, browser);
            if (file != null)
            {
                if ((OperatingSystem.isCaseInsensitiveFS())
                && path.equalsIgnoreCase(path2))
                {
                    // allow user to change name
                    // case
                }
                else
                {
                    VFSManager.error(browser, path,
                    "ioerror.rename-exists",
                    new String[]{path2});
                    return;
                }
            }

            if (!vfs._rename(session, path, path2, browser))
                VFSManager.error(browser, path, "ioerror.rename-error",
                new String[]{path2});
        }
        catch (IOException io)
        {
            setCancellable(false);
            Log.log(Log.ERROR, this, io);
            String[] pp = {io.toString()};
            VFSManager.error(browser, path, "ioerror.directory-error", pp);
        }
        finally
        {
            try
            {
                vfs._endVFSSession(session, browser);
            }
            catch (IOException io)
            {
                setCancellable(false);
                Log.log(Log.ERROR, this, io);
                String[] pp = {io.toString()};
                VFSManager.error(browser, path, "ioerror.directory-error", pp);
            }
        }
    }

    public String toString()
    {
        return getClass().getName() + "[type=RENAME"
            + ",vfs=" + vfs + ",path=" + path
            + ",path2=" + path2 + ']';
    }

    private String path2;
}
