package edu.ucsd.crbs.autodockjobgenerator;

import java.io.File;

/**
 *
 * @author churas
 */
public class Constants {
    
    
    public static final String PANFISH_BASEDIR_VAR_NAME = "$PANFISH_BASEDIR";
    
    public static final String LOGS_DIR_NAME = "logs";
    
    public static final String PDBQT_DIR_NAME = "pdbqt";
    
    public static final String LOG_SUFFIX = "log";
    public static final String PDBQT_SUFFIX = "pdbqt";
    /**
     * New line character to use.  This is hard coded cause we don't want to 
     * use non unix new line in tar files.
     */
    public static final String NEW_LINE = "\n";
    
    /**
     * Script that will run batches of Auto Dock jobs
     */
    public static final String AUTO_DOCK_SCRIPT = "autodock.sh";
    
    /**
     * Script that runs the Auto Dock jobs via Panfish
     */
    public static final String PANFISH_AUTO_DOCK_SCRIPT = "panfish_autodock.sh";
    
    /**
     * Template file from which {@link #PANFISH_AUTO_DOCK_SCRIPT} is created
     */
    public static final String PANFISH_AUTO_DOCK_TEMPLATE = File.separator+PANFISH_AUTO_DOCK_SCRIPT+".template";
    
    /**
     * Suffix of Auto dock job configuration file
     */
    public static final String AUTO_DOCK_CONFIG_SUFFIX = "."+AUTO_DOCK_SCRIPT+".config";
    
    /**
     * Template file from which {@link #AUTO_DOCK_SCRIPT} is created
     */
    public static final String AUTODOCK_TEMPLATE = File.separator+AUTO_DOCK_SCRIPT+".template";
    
    /**
     * Variable holding path of Auto Dock binary in {@link #AUTO_DOCK_SCRIPT} script
     */
    public static final String VINA = "VINA";
    
    /**
     * Variable holding arguments for Auto Dock binary in {@link #AUTO_DOCK_SCRIPT} script
     */
     public static final String ARGUMENTS = "ARGUMENTS";
     
    
    /**
     *  Delimiter put after each task id in the Auto dock job configuration files
     */
    public static final String CONFIG_SUBTASK_ID_DELIM = ":::";
    
    /**
     * Flag passed to Auto Dock to denote Ligand model
     */
    public static final String LIGANDS_FLAG = "--ligand ";
    
    /**
     * Flag passed to Auto Dock to denote Receptor model
     */
    public static final String RECEPTORS_FLAG = "--receptor ";

    /**
     * Name of directory under {@value #INPUTS_DIR_NAME}/(task id) to hold ligand files
     */
    public static final String LIGANDS_DIR_NAME = "ligands";
    
    /**
     * Name of directory under {@value #INPUTS_DIR_NAME}/(task id) to hold receptor files
     */
    public static final String RECEPTORS_DIR_NAME = "receptors";
    
    /**
     * Directory name holding inputs for Auto dock jobs
     */
    public static final String INPUTS_DIR_NAME = "inputs";
    
    /**
     * Directory name holding outputs for Auto dock jobs
     */
    public static final String OUTPUTS_DIR_NAME = "outputs";
        
    public static final String LIGANDS_INPUTS_DIR = INPUTS_DIR_NAME+File.separator+LIGANDS_DIR_NAME+File.separator;
    
    
    public static final String RECEPTORS_INPUTS_DIR = INPUTS_DIR_NAME+File.separator+RECEPTORS_DIR_NAME+File.separator;
    
    /**
     * Number of sub jobs to batch into each {@link #AUTO_DOCK_SCRIPT} job
     */
    public static int SUB_JOBS_PER_JOB = 400;
    
    /**
     * Suffix for compressed input and output files
     */
    public static final String TAR_GZ_SUFFIX = ".tar.gz";
    
    /**
     * Percentage of available processors to use when setting up the thread pool
     * to run the input compression jobs during job generation
     */
    public static  double PERCENT_OF_CORES_TO_USE_FOR_COMPRESSION = 0.90;
    
}
