// $Id: XPilotPanelException.java,v 1.7 2008/07/14 03:00:08 taraskostiak Exp $

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
 * Base exception for the <code>net.sf.xpilotpanel.*</code>.
 * 
 * @author Taras Kostiak
 * 
 */
public class XPilotPanelException extends Exception {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7455352547992134042L;

    /**
     * Constructs new <code>XPilotPanelException</code>.
     */
    public XPilotPanelException() {
        super();
    }

    /**
     * Constructs new <code>XPilotPanelException</code>.
     * 
     * @param msg
     *            Exception message.
     */
    public XPilotPanelException(String msg) {
        super(msg);
    }

    /**
     * Constructs new <code>XPilotPanelException</code>.
     * 
     * @param c
     *            Exception cause.
     */
    public XPilotPanelException(Throwable c) {
        super(c);
    }

    /**
     * Constructs new <code>XPilotPanelException</code>.
     * 
     * @param msg
     *            Exception message.
     * @param c
     *            Exception cause.
     */
    public XPilotPanelException(String msg, Throwable c) {
        super(msg, c);
    }

    /**
     * Is used, when problems with reading/writing
     * <code>"user.home"/.xpilotpanel</code> directory and it's content.
     */
    public static final String XPILOTPANEL_DIRECTORY_ACCESS_ERROR = "Error, while accessing \".xpilotpanel\" directory.";
}
