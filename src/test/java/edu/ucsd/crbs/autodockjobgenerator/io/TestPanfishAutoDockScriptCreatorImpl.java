package edu.ucsd.crbs.autodockjobgenerator.io;

import edu.ucsd.crbs.autodockjobgenerator.Constants;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.junit.Rule;

import static org.junit.Assert.*;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author churas
 */
public class TestPanfishAutoDockScriptCreatorImpl {
    
    public TestPanfishAutoDockScriptCreatorImpl() {
    }
    
    
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();
  
    @Test
    public void createPanfishAutoDockScriptNullOutputJobDir() throws Exception {
        PanfishAutoDockScriptCreatorImpl scriptMaker = new PanfishAutoDockScriptCreatorImpl();
        
        try {
            scriptMaker.createPanfishAutoDockScript(null);
            fail("Expected IllegalArgumentException");
        }
        catch(IllegalArgumentException iae){
            assertTrue(iae.getMessage().equals("Argument cannot be null"));
        }
    }
    
    @Test
    public void createPanfishAutoDockScriptWithValidDirectory() throws Exception {
        PanfishAutoDockScriptCreatorImpl scriptMaker = new PanfishAutoDockScriptCreatorImpl();
        File outputDirFile = folder.newFolder();
        scriptMaker.createPanfishAutoDockScript(outputDirFile.getAbsolutePath());
        File autoDockScript = new File(outputDirFile.getAbsoluteFile()+File.separator+Constants.PANFISH_AUTO_DOCK_SCRIPT);

        //verify we have an executable script
        assertTrue(autoDockScript.isFile() == true);
        assertTrue(autoDockScript.canExecute() == true);
        assertTrue(autoDockScript.length() > 0);
    }
    
    @Test
    public void createPanfishAutoDockScriptWithInValidDirectory() throws Exception {
        PanfishAutoDockScriptCreatorImpl scriptMaker = new PanfishAutoDockScriptCreatorImpl();
        File outputDirFile = folder.newFolder();
        try {
            scriptMaker.createPanfishAutoDockScript(outputDirFile.getAbsolutePath()+File.separator+"DIR_DOES_NOT_EXIST");
            fail("Expected IOException here");
        }
        catch(IOException ie){
            assertTrue(ie.getMessage().contains("No such file or directory"));
        }
    }
    
}
