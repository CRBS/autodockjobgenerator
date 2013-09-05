package edu.ucsd.crbs.autodockjobgenerator.io;

import edu.ucsd.crbs.autodockjobgenerator.Constants;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this class take the template auto dock script found in the 
 * class path resources of the jar and write the script to the output directory
 * for the job.
 * 
 * @author churas
 */
public class AutoDockScriptCreatorImpl implements AutoDockScriptCreator {

    final static Logger logger = LoggerFactory.getLogger(AutoDockScriptCreatorImpl.class);
    
    
    /**
     * This method reads the {@value edu.ucsd.crbs.autodockjobgenerator.Constants#AUTODOCK_TEMPLATE}
     * from the class path and writes out the file {@value edu.ucsd.crbs.autodockjobgenerator.Constants#AUTO_DOCK_SCRIPT}
     * to the <b>outputJobDir</b> directory.  The values in <b>arguments</b> and <b>pathToVina</b>
     * are set in the lines that start with <b>ARGUMENTS</b> and <b>VINA</b>.
     * @param outputJobDir Directory to write the auto dock script to
     * @param arguments Value to set in the ARGUMENTS line in script
     * @param pathToVina Value to set in the VINA line in the script
     * @throws IOException If there is an IO error.
     */
    @Override
    public void createAutoDockScript(final String outputJobDir,final String arguments,
                                     final String pathToVina) throws IOException {
        
        if (outputJobDir == null){
            throw new IllegalArgumentException("outputJobDir method parameter cannot be null");
        }
        
        logger.debug("Loading auto dock template from class path: {}",Constants.AUTODOCK_TEMPLATE);
        //load script
        List<String> scriptLines = IOUtils.readLines(Class.class.getResourceAsStream(Constants.AUTODOCK_TEMPLATE));

        String autoDockScript = outputJobDir+File.separator+
                                Constants.AUTO_DOCK_SCRIPT;

        BufferedWriter bw = new BufferedWriter(new FileWriter(autoDockScript));
                        
        //fix the VINA and ARGUMENTS and save script
        for (String line : scriptLines){
            if (pathToVina != null && line.startsWith(Constants.VINA)){
                line = Constants.VINA+"=\""+pathToVina+"\"";
                logger.debug("Replacing line starting with: {} and setting to: {}",Constants.VINA,line);
            }
            else if (arguments != null && line.startsWith(Constants.ARGUMENTS)){
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
