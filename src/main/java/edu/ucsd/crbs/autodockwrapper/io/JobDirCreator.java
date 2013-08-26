/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import java.io.IOException;

/**
 * Classes implementing this interface will support the creation of the job
 * directories needed to run Auto Dock jobs
 * @author churas
 */
public interface JobDirCreator {
    
    
    /**
     * Creates job directories for auto dock vina jobs
     * @param path Path to create job directories under
     * @throws IOException If there is an error during the creation step.
     */
    public void createJobDirectories(final String path) throws IOException;
    
}
