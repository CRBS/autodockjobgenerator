package edu.ucsd.crbs.autodockjobgenerator.io;

import edu.ucsd.crbs.autodockjobgenerator.Constants;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author churas
 */
public class TestJobDirCreatorImpl {
    
    public TestJobDirCreatorImpl() {
    }
    
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();
   
    @Test
    public void createJobDirectoriesNullArgument() throws Exception {
        JobDirCreatorImpl jci = new JobDirCreatorImpl();
        
        try {
            jci.createJobDirectories(null);
            fail("Expected IllegalArgument Exception");
        }
        catch(IllegalArgumentException iae){
            assertTrue(iae.getMessage().equals("path method parameter cannot be null"));
        }
        
    }
    
    @Test
    public void createJobDirectoriesValidPath() throws Exception {
        JobDirCreatorImpl jci = new JobDirCreatorImpl();

            File baseDir = folder.newFolder();
            File jobDir = new File(baseDir.getAbsoluteFile()+File.separator+"jobdir");
            jci.createJobDirectories(jobDir.getAbsolutePath());
            
            File inputsDir = new File(jobDir.getAbsoluteFile()+File.separator+Constants.INPUTS_DIR_NAME);
            assertTrue(inputsDir.isDirectory() == true);
            
            File outputsOutDir = new File(jobDir.getAbsoluteFile()+File.separator+Constants.OUTPUTS_DIR_NAME);
            assertTrue(outputsOutDir.isDirectory() == true);
            
           
    }
    
}
