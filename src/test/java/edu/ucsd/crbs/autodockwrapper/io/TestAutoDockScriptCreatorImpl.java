/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import edu.ucsd.crbs.autodockwrapper.Constants;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/**
 *
 * @author churas
 */
public class TestAutoDockScriptCreatorImpl {
    
    public TestAutoDockScriptCreatorImpl() {
    }
    
  @Rule
    public TemporaryFolder folder= new TemporaryFolder();
  
  @Test
  public void createAutoDockScriptNullOutputJobDir() throws Exception {
      AutoDockScriptCreatorImpl scriptMaker = new AutoDockScriptCreatorImpl();
      try {
        scriptMaker.createAutoDockScript(null, null, null);
      }
      catch(IllegalArgumentException iae){
          assertTrue(iae.getMessage().equals("outputJobDir method parameter cannot be null"));
      }
      
  }
  
  @Test
    public void createAutoDockScriptWithValidOutputJobDir() throws Exception {
        AutoDockScriptCreatorImpl scriptMaker = new AutoDockScriptCreatorImpl();
        File outputDirFile = folder.newFolder();
        scriptMaker.createAutoDockScript(outputDirFile.getAbsolutePath(),"somearg","pathtovina");
        File autoDockScript = new File(outputDirFile.getAbsoluteFile()+File.separator+Constants.AUTO_DOCK_SCRIPT);

        //verify we have an executable script
        assertTrue(autoDockScript.isFile() == true);
        assertTrue(autoDockScript.canExecute() == true);
        assertTrue(autoDockScript.length() > 0);
        
        List<String> lines = IOUtils.readLines(new FileReader(autoDockScript));
        
        boolean foundVina = false;
        boolean foundArgs = false;
        for (String line : lines){
            if (line.startsWith(Constants.ARGUMENTS)){
                foundArgs = true;
                assertTrue(line.equals(Constants.ARGUMENTS+"=\"somearg\""));
            }
            if (line.startsWith(Constants.VINA)){
                foundVina = true;
                assertTrue(line.equals(Constants.VINA+"=\"pathtovina\""));
            }
        }
        assertTrue(foundArgs == true);
        assertTrue(foundVina == true);
    }
    
  @Test
  public void createAutoDockScriptWithValidOutputJobDirButOtherArgsNull() throws Exception {
        AutoDockScriptCreatorImpl scriptMaker = new AutoDockScriptCreatorImpl();
        File outputDirFile = folder.newFolder();
        scriptMaker.createAutoDockScript(outputDirFile.getAbsolutePath(),null,null);
        File autoDockScript = new File(outputDirFile.getAbsoluteFile()+File.separator+Constants.AUTO_DOCK_SCRIPT);

        //verify we have an executable script
        assertTrue(autoDockScript.isFile() == true);
        assertTrue(autoDockScript.canExecute() == true);
        assertTrue(autoDockScript.length() > 0);        
    }
    
  @Test
    public void createAutoDockScriptWithInValidOutputJobDir() throws Exception {
        AutoDockScriptCreatorImpl scriptMaker = new AutoDockScriptCreatorImpl();
        File outputDirFile = folder.newFolder();
        try {
            scriptMaker.createAutoDockScript(outputDirFile.getAbsolutePath()+File.separator+"PATH_DOES_NOT_EXIST","somearg","pathtovina");
        }
        catch(IOException ex){
            assertTrue(ex.getMessage().contains("No such file or directory"));
        }
    }
  
}
