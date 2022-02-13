package org.gjt.sp.jedit.browser;

import org.experiment.editor.io.VFS;
import org.experiment.editor.io.VFSManager;
import org.experiment.editor.Editor;
import org.experiment.util.Log;

import java.io.IOException;

/**
 * @author John Doe
 */
class MkDirBrowserTask extends AbstractBrowserTask{
    /**
	 * Creates a new browser I/O request.
	 * @param browser The VFS browser instance
	 * @param path The first path name to operate on
	 */
	MkDirBrowserTask(VFSBrowser browser,
    Object session, VFS vfs, String path,
    Runnable awtRunnable)
    {
        super(browser, session, vfs, path, awtRunnable);
    }

    @Override
    public void _run()
    {
        String[] args = {path};
        try
        {
            setCancellable(true);
            setStatus(Editor.getProperty("vfs.status.mkdir",args));

            path = vfs._canonPath(session, path,browser);

            if(!vfs._mkdir(session, path,browser))
                VFSManager.error(browser, path,"ioerror.mkdir-error",null);
        }
        catch(IOException io)
        {
            setCancellable(false);
            Log.log(Log.ERROR,this,io);
            args[0] = io.toString();
            VFSManager.error(browser, path,"ioerror",args);
        }
        finally
        {
            try
            {
                vfs._endVFSSession(session,browser);
            }
            catch(IOException io)
            {
                setCancellable(false);
                Log.log(Log.ERROR,this,io);
                args[0] = io.toString();
                VFSManager.error(browser, path,"ioerror",args);
            }
        }
    }

    public String toString()
    {
        return getClass().getName() + "[type=DELETE"
            + ",vfs=" + vfs + ",path=" + path + ']';
    }
}