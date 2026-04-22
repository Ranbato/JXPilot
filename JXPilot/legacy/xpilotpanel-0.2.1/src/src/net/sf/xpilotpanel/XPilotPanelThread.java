// $Id: XPilotPanelThread.java,v 1.7 2008/08/04 06:29:19 taraskostiak Exp $

// XPilotPanel.
// Copyright (C) 2007-2008 by: Taras Kostiak and others(see PEOPLE file).
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA

package net.sf.xpilotpanel;

/**
 * All lightwetght processes in XPilotPanel should be created using
 * <code>XPilotPanelThread</code>. <br>
 * When thread fail program won't just exit, it will generate bug report. But
 * insted of traditional overriding run() method you should override safeRun().
 * <br>
 * By the way, you can't override run().
 * 
 * @see #safeRun()
 * 
 * @author Taras Kostiak
 * 
 */
public abstract class XPilotPanelThread extends Thread {

    /**
     * This method make thread "crash safe"(i.e. any exception that occurs will
     * force bug report).
     */
    public final void run() {
        try {
            safeRun();
        }
        catch (XPilotPanelException e) {
            // TODO: Make bug report.
        }
        catch (Exception e) {
            // TODO: Make bug report.
        }
    }

    /**
     * Implement this method to define new lightweightprocess. <br>
     * Analog of <code>java.lang.Runnable#run()</code>.
     * 
     * @see Runnable#run()
     * 
     * @throws XPilotPanelException
     *             When some known exeption that crashed thread occurs.
     * @throws Exception
     *             When other exeption that crashed thread occurs.
     */
    abstract public void safeRun() throws XPilotPanelException, Exception;

    /**
     * Finishes execution of <code>XPilotPanelThread</code> or is used as
     * destructor for data that contains finished thread.
     */
    abstract public void finish();

}
