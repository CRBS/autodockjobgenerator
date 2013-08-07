/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import java.io.IOException;

/**
 * PROTOTYPE CODE!!!!!!!!!!!!!!
 * @author churas
 */
public interface AutoDockScriptCreator {
    
    public void createAutoDockScript(final String outputJobDir,final String arguments,
                                     final String pathToVina) throws IOException;
}
