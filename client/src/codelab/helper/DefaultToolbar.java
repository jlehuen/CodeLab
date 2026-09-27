package codelab.helper;

import codelab.AbstractModule;
import codelab.AbstractToolbar;

public class DefaultToolbar extends AbstractToolbar {

    ///////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////

	private static final long serialVersionUID = 1L;

	public DefaultToolbar(AbstractModule module) {
        super(module);
        addSeparatorflex();
        endToolbar();
    }

    ///////////////////////////////////////////////////
    // Toolbar updating
    ///////////////////////////////////////////////////

    public void update() {
        super.update();
    }

    ///////////////////////////////////////////////////
    // Action handler
    ///////////////////////////////////////////////////

    public void action(String ident) {
    	super.action(ident);
    }
}
