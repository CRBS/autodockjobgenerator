/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper;

import java.io.File;

/**
 *
 * @author churas
 */
public class Constants {
    
    public static final String NEW_LINE = "\n";
    
    public static final String AUTO_DOCK_SCRIPT = "autodock.sh";
    
    public static final String AUTO_DOCK_CONFIG_SUFFIX = "."+AUTO_DOCK_SCRIPT+".config";
    
    public static final String AUTODOCK_TEMPLATE = File.separator+AUTO_DOCK_SCRIPT+".template";
     public static final String VINA = "VINA";
     public static final String ARGUMENTS = "ARGUMENTS";
     
    
    public static final String CONFIG_SUBTASK_ID_DELIM = ":::";
    public static final String LIGANDS_FLAG = "--ligand ";
    public static final String RECEPTORS_FLAG = "--receptor ";
    
    public static final String LIGANDS_DIR_NAME = "ligands";
    public static final String RECEPTORS_DIR_NAME = "receptors";
    public static final String INPUTS_DIR_NAME = "inputs";
    public static final String OUTPUTS_DIR_NAME = "outputs";
    
    public static final String LIGANDS_INPUTS_DIR = INPUTS_DIR_NAME+File.separator+LIGANDS_DIR_NAME+File.separator;
    public static final String RECEPTORS_INPUTS_DIR = INPUTS_DIR_NAME+File.separator+RECEPTORS_DIR_NAME+File.separator;
    
    public static int SUB_JOBS_PER_JOB = 400;
    
    public static final String TAR_GZ_SUFFIX = ".tar.gz";
    
    public static double PERCENT_OF_CORES_TO_USE_FOR_COMPRESSION = 0.75;
    
     
    
}
