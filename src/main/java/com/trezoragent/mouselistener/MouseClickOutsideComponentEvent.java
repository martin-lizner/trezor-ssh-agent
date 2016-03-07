package com.trezoragent.mouselistener;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.Serializable;

/**
 * @author Martin Lizner
 * 
 * Event for mouse click outside the component.
 * 
 */

public class MouseClickOutsideComponentEvent extends MouseEvent implements Serializable {

    public MouseClickOutsideComponentEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount, boolean popupTrigger, int button) {
        super(source, id, when, modifiers, x, y, clickCount, popupTrigger, button);
    }

    public MouseClickOutsideComponentEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount, boolean popupTrigger) {
        super(source, id, when, modifiers, x, y, clickCount, popupTrigger);
    }

    public MouseClickOutsideComponentEvent(Component source, int id, long when, int modifiers, int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger, int button) {
        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger, button);
    }
}
