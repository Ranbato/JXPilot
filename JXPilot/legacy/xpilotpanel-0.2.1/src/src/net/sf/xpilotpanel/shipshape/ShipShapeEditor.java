// $Id: ShipShapeEditor.java,v 1.2 2008/07/14 03:00:47 taraskostiak Exp $

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

package net.sf.xpilotpanel.shipshape;

/**
 * This abstract class should be base of any shipshape editor, that is built-in
 * into XPilotPanel.
 * 
 * @author Taras Kostiak
 * 
 */
public abstract class ShipShapeEditor {

    /**
     * Launches shipshape editor that creates new shipshape.
     * 
     * @return Created shipshape.
     */
    public final String createNewShipShape() {
        return editShipShape(null);
    }

    /**
     * Launches shipshape editor that edits given shipshape.
     * 
     * @param ss
     *            Shipshape to edit.
     * @return Modified shipshape.
     */
    public final String editShipShape(String ss) {
        return editShipShapeImpl(ss);
    }

    /**
     * This abstract method should be overriden by any shipshape editor, that is
     * built-in into XPilotPanel. Implementation should start GUI of editor,
     * edit(or create) given shipshape and return modified one.
     * 
     * @param ss
     *            Shipshape to edit(if null - then create new).
     * @return Modified or created shipshape.
     */
    public abstract String editShipShapeImpl(String ss);

}
