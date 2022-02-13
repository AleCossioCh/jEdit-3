package org.gjt.sp.jedit.browser;

import org.experiment.editor.io.VFS;
import org.experiment.editor.io.VFSManager;
import org.experiment.editor.Editor;
import org.experiment.util.Log;

import java.io.IOException;

/**
 * @author John Doe
 */
class DeleteBrowserTask  extends AbstractBrowserTask{
    /**
	 * Creates a new browser I/O request.
	 *
	 * @param browser The VFS browser instance
	 * @param path    The first path name to operate on
	 */
	DeleteBrowserTask(VFSBrowser browser,
    Object session, VFS vfs, String path)
    {
        super(browser, session, vfs, path, null);
    }

    @Override
    public void _run()
    {
        try
        {
            setCancellable(false);
            String[] args = {path};
            setStatus(Editor.getProperty("vfs.status.deleting", args));

            path = vfs._canonPath(session, path, browser);


            if (!vfs._delete(session, path, browser))
                VFSManager.error(browser, path, "ioerror.delete-error", null);
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
        return getClass().getName() + "[type=DELETE"
            + ",vfs=" + vfs + ",path=" + path +
        ']';
    }
}
