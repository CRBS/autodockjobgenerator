/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import java.io.IOException;

/**
 * Interface to create Auto Dock script.
 * @author churas
 */
public interface AutoDockScriptCreator {
    
    
    /**
     * Implementing methods should create Auto Dock Script under <b>outputJobDir</b> named: {@value edu.ucsd.crbs.autodockwrapper.Constants#AUTO_DOCK_SCRIPT}
     * @param outputJobDir
     * @param arguments
     * @param pathToVina
     * @throws IOException 
     */
    public void createAutoDockScript(final String outputJobDir,final String arguments,
                                     final String pathToVina) throws IOException;
}
