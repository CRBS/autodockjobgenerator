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
 * PROTOTYPE CODE!!!!!!!!!!!!!!
 * @author churas
 */
public class AutoDockScriptCreatorImpl implements AutoDockScriptCreator {

    final static Logger logger = LoggerFactory.getLogger(AutoDockScriptCreatorImpl.class);
    
    @Override
    public void createAutoDockScript(final String outputJobDir,final String arguments,
                                     final String pathToVina) throws IOException {
        
        logger.debug("Loading auto dock template from class path: {}",Constants.AUTODOCK_TEMPLATE);
        //load script
        List<String> scriptLines = IOUtils.readLines(Class.class.getResourceAsStream(Constants.AUTODOCK_TEMPLATE));

        String autoDockScript = outputJobDir+File.separator+
                                Constants.AUTO_DOCK_SCRIPT;

        BufferedWriter bw = new BufferedWriter(new FileWriter(autoDockScript));
                        
        //fix the VINA and ARGUMENTS and save script
        for (String line : scriptLines){
            if (line.startsWith(Constants.VINA)){
                line = Constants.VINA+"=\""+pathToVina+"\"";
                logger.debug("Replacing line starting with: {} and setting to: {}",Constants.VINA,line);
            }
            else if (line.startsWith(Constants.ARGUMENTS)){
                line = Constants.ARGUMENTS+"=\""+arguments+"\"";
                 logger.debug("Replacing line starting with: {} and setting to: {}",Constants.ARGUMENTS,line);
            }
            bw.write(line);
            bw.newLine();
        }
        bw.close();
        
        //make script executable
        File aDockScript = new File(autoDockScript);
        aDockScript.setExecutable(true, false);
    }
    
}
