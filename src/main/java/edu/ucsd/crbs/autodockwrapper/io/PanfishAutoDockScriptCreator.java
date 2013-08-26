/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import java.io.IOException;

/**
 * Interface to create Panfish Auto Dock Script.
 * @author churas
 */
public interface PanfishAutoDockScriptCreator {
    
    /**
     * Implementing methods should create {@value edu.ucsd.crbs.autodockwrapper.Constants#PANFISH_AUTO_DOCK_SCRIPT} under <b>outputJobDir</b> directory.
     * @param outputJobDir Path under which the Panfish Auto Dock script should be created.
     * @throws IOException 
     */
    public void createPanfishAutoDockScript(final String outputJobDir) throws IOException;
    
}
