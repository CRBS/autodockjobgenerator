/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 * PROTOTYPE CODE!!!!!!!!!!!!!!
 * @author churas
 */
public class AutoDockScriptCreatorImpl implements AutoDockScriptCreator {

     public static final String AUTODOCK_TEMPLATE = "/autodock.sh.template";
     public static final String VINA = "VINA";
     public static final String ARGUMENTS = "ARGUMENTS";
     public static final String AUTO_DOCK_SCRIPT = "autodock.sh";
    
    @Override
    public void createAutoDockScript(final String outputJobDir,final String arguments,
                                     final String pathToVina) throws IOException {
        
        //load script
        List<String> scriptLines = IOUtils.readLines(Class.class.getResourceAsStream(AUTODOCK_TEMPLATE));

        String autoDockScript = outputJobDir+File.separator+AUTO_DOCK_SCRIPT;

        BufferedWriter bw = new BufferedWriter(new FileWriter(autoDockScript));
                        
        //fix the VINA and ARGUMENTS and save script
        for (String line : scriptLines){
            if (line.startsWith(VINA)){
                line = VINA+"=\""+pathToVina+"\"";
            }
            else if (line.startsWith(ARGUMENTS)){
                line = ARGUMENTS+"=\""+arguments+"\"";
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
