/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import edu.ucsd.crbs.autodockwrapper.Constants;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this class write out a Panfish auto dock script which runs the
 * auto dock jobs via panfish.  The script is pulled from a template found in
 * the /{@value edu.ucsd.crbs.autodockwrapper.Constants#PANFISH_AUTO_DOCK_TEMPLATE}
 * @author churas
 */
public class PanfishAutoDockScriptCreatorImpl implements PanfishAutoDockScriptCreator {

    final static Logger logger = LoggerFactory.getLogger(PanfishAutoDockScriptCreatorImpl.class);
    
    /**
     * Creates {@value edu.ucsd.crbs.autodockwrapper.Constants#PANFISH_AUTO_DOCK_SCRIPT} script
     * in the <b>outputjobDir</b> specified as a parameter to this method.  The script
     * is derived from a template file found in the class path.
     * @param outputJobDir Directory to write the panfish auto dock script to
     * @throws IOException 
     */
    @Override
    public void createPanfishAutoDockScript(String outputJobDir) throws IOException {
        
        if (outputJobDir == null){
            throw new IllegalArgumentException("Argument cannot be null");
        }
        logger.debug("Loading panfish auto dock template from class path: {}",Constants.PANFISH_AUTO_DOCK_TEMPLATE);
        //load script
        List<String> scriptLines = IOUtils.readLines(Class.class.getResourceAsStream(Constants.PANFISH_AUTO_DOCK_TEMPLATE));

        String panfishAutoDockScript = outputJobDir+File.separator+
                                Constants.PANFISH_AUTO_DOCK_SCRIPT;

        BufferedWriter bw = new BufferedWriter(new FileWriter(panfishAutoDockScript));
                        
        for (String line : scriptLines){
            bw.write(line);
            bw.newLine();
        }
        bw.close();
        
        //make script executable
        File aDockScript = new File(panfishAutoDockScript);
        aDockScript.setExecutable(true, false);
    }
    
}
