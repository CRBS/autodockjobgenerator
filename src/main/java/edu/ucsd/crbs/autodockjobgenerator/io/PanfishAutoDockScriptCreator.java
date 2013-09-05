package edu.ucsd.crbs.autodockjobgenerator.io;

import java.io.IOException;

/**
 * Interface to create Panfish Auto Dock Script.
 * @author churas
 */
public interface PanfishAutoDockScriptCreator {
    
    /**
     * Implementing methods should create {@value edu.ucsd.crbs.autodockjobgenerator.Constants#PANFISH_AUTO_DOCK_SCRIPT} under <b>outputJobDir</b> directory.
     * @param outputJobDir Path under which the Panfish Auto Dock script should be created.
     * @throws IOException 
     */
    public void createPanfishAutoDockScript(final String outputJobDir) throws IOException;
    
}
