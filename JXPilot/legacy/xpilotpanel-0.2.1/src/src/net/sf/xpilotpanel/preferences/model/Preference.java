// $Id: Preference.java,v 1.9 2008/07/30 02:48:14 taraskostiak Exp $

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

package net.sf.xpilotpanel.preferences.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * This class determines single preference, its type, default value, etc.
 * 
 * @author Taras Kostiak
 * 
 */
@XmlRootElement(name = "preference")
// @XmlType(propOrder = { "i18nKey", "type", "defaultValue", "name" })
@XmlType(propOrder = { "name", "i18nKey", "type", "defaultValue" })
public class Preference {

    /**
     * Type name, that represents a <code>JTextField</code>.
     */
    public static final String TYPE_TEXT_FIELD = "textField";

    /**
     * Type name, that represents a <code>JCheckBox</code>.
     */
    public static final String TYPE_BOOLEAN = "boolean";

    /**
     * Type name, that represents a <code>JComboBox</code>.
     */
    public static final String TYPE_COMBO_BOX = "comboBox";

    /**
     * Type name, that represents path to file(or dir) that can be selectable
     * with JFileChooser.
     */
    public static final String TYPE_FILE_PATH = "filePath";

    /**
     * Name of preference.<br>
     * Is required.
     */
    protected String name = null;

    /**
     * Default value.<br>
     * Is required.
     */
    protected String defaultValue = null;

    /**
     * Type(for building preferences-releted GUI's).<br>
     * If not specified than default will be used - text field.<br>
     * <br>
     * Types availible:<br>
     * <ul>
     * <li>textField</li>
     * <li>boolean</li>
     * <li>comboBox</li>
     * </ul>
     */
    protected String type = null;

    /**
     * Key for i18n.<br>
     * If not specified than <code>preference.NAME</code> will be used(where
     * NAME is name specified above).
     */
    protected String i18nKey = null;

    /**
     * Stores additional data for this <code>Preference</code>.
     */
    protected String value = null;

    /**
     * Default constructor.
     */
    public Preference() {
    }

    /**
     * Standart getter.
     * 
     * @see #defaultValue
     */
    @XmlAttribute
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Standart setter.
     * 
     * @see #defaultValue
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Standart getter.
     * 
     * @see #name
     */
    @XmlAttribute(required = true)
    public String getName() {
        return name;
    }

    /**
     * Standart setter.
     * 
     * @see #name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Standart getter.
     * 
     * @see #type
     */
    @XmlAttribute
    public String getType() {
        return (type != null) ? (type) : (TYPE_TEXT_FIELD);
    }

    /**
     * Standart getter.
     * 
     * @see #i18nKey
     */
    @XmlAttribute(name = "i18nKey")
    public String getI18nKey() {
        return (i18nKey != null) ? (i18nKey) : ("preference." + name);
    }

    /**
     * Standart setter.
     * 
     * @see #i18nKey
     */
    public void setI18nKey(String key) {
        i18nKey = key;
    }

    /**
     * Standart setter.
     * 
     * @see #type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Standart getter.
     * 
     * @see #value
     */
    @XmlValue
    public String getValue() {
        return value;
    }

    /**
     * Standart setter.
     * 
     * @see #value
     */
    public void setValue(String value) {
        this.value = value;
    }

}
