/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/**
 * PROTOTYPE CODE!!!!!!!!!!!!!!
 * @author churas
 */
public class JobDirCreatorImpl implements JobDirCreator {

    public static final String INPUTS_DIR_NAME = "inputs";
    public static final String OUTPUTS_DIR_NAME = "outputs";

    /**
     * Creates directory specified by path argument and also creates
     * INPUTS_DIR_NAME and OUTPUTS_DIR_NAME subdirectories under that
     * path
     * @param path Directory to create
     * @throws IOException If there is an error creating any of these directories
     */
    @Override
    public void createJobDirectories(final String path) throws IOException {
        File inputsDir = new File(path+File.separator+INPUTS_DIR_NAME);
        FileUtils.forceMkdir(inputsDir);
        File outputsDir = new File(path+File.separator+OUTPUTS_DIR_NAME);
        FileUtils.forceMkdir(outputsDir);
    }
    
}
