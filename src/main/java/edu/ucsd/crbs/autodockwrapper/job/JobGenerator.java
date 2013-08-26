/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.job;

import java.io.IOException;

/**
 * Interface that defines methods to create Auto Dock jobs
 * @author churas
 */
public interface JobGenerator extends java.lang.Runnable {
    
    
    /**
     * This method should create the auto dock jobs
     * @throws IOException 
     */
    public void createJobs() throws IOException;

    /**
     * This method should return the number of jobs created after {@link #createJobs} is
     * invoked.
     * @return Number of jobs created by the {@link #createJobs} method.
     */
    public long getTotalJobs();
}
