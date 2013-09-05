package edu.ucsd.crbs.autodockjobgenerator.io;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

import edu.ucsd.crbs.autodockjobgenerator.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this class create the directories which will hold the generated
 * jobs.  
 * @author churas
 */
public class JobDirCreatorImpl implements JobDirCreator {

    final static Logger logger = LoggerFactory.getLogger(JobDirCreatorImpl.class);
    
    /**
     * Creates the following directories under the <b>path</b> argument:<p/>
     * 
     * {@value edu.ucsd.crbs.autodockjobgenerator.Constants#INPUTS_DIR_NAME}<br/>
     * {@value edu.ucsd.crbs.autodockjobgenerator.Constants#OUT_DIR}<br/>
     * {@value edu.ucsd.crbs.autodockjobgenerator.Constants#ERR_DIR}<br/>
     * <p/>
     * The <b>path</b> does NOT already need to exist.  
     * 
     * 
     * @param path Directory to create
     * @throws IOException If there is an error creating any of these directories
     * @throws IllegalArgumentException if the path argument passed in is null
     */
    @Override
    public void createJobDirectories(final String path) throws IOException {
        
        if (path == null){
            throw new IllegalArgumentException("path method parameter cannot be null");
        }
        
        File inputsDir = new File(path+File.separator+Constants.INPUTS_DIR_NAME);
        logger.debug("Creating directory: {}",inputsDir.getAbsolutePath());
        FileUtils.forceMkdir(inputsDir);
        File outputsDir = new File(path+File.separator+
                                   Constants.OUTPUTS_DIR_NAME);
        logger.debug("Creating directory: {}",outputsDir.getAbsolutePath());
        FileUtils.forceMkdir(outputsDir);
    }
    
}
