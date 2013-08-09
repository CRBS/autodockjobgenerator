/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

import edu.ucsd.crbs.autodockwrapper.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PROTOTYPE CODE!!!!!!!!!!!!!!
 * @author churas
 */
public class JobDirCreatorImpl implements JobDirCreator {

    final static Logger logger = LoggerFactory.getLogger(JobDirCreatorImpl.class);
    
    /**
     * Creates directory specified by path argument and also creates
     * INPUTS_DIR_NAME and OUTPUTS_DIR_NAME subdirectories under that
     * path
     * @param path Directory to create
     * @throws IOException If there is an error creating any of these directories
     */
    @Override
    public void createJobDirectories(final String path) throws IOException {
        
        File inputsDir = new File(path+File.separator+Constants.INPUTS_DIR_NAME);
        logger.debug("Creating directory: {}",inputsDir.getAbsolutePath());
        FileUtils.forceMkdir(inputsDir);
        File outputsDir = new File(path+File.separator+
                                   Constants.OUTPUTS_DIR_NAME);
        logger.debug("Creating directory: {}",outputsDir.getAbsolutePath());
        FileUtils.forceMkdir(outputsDir);
    }
    
}
