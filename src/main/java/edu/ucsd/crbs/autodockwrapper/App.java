package edu.ucsd.crbs.autodockwrapper;

import joptsimple.OptionParser;

import java.io.File;
import joptsimple.OptionSet;
import edu.ucsd.crbs.autodockwrapper.io.*;
import edu.ucsd.crbs.autodockwrapper.job.JobGenerator;
import edu.ucsd.crbs.autodockwrapper.job.JobGeneratorImpl;
import java.io.FileReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * PROTOTYPE CODE!!!!!!!!!!!!!!
 * This is the main entry class for AutoDockWrapper. AutoDockWrapper is a
 * program that generates Auto Dock Vina jobs and optionally will run them on
 * HPC compute resources via Panfish.
 *
 * @author Christopher Churas
 */
public class App {

    public static final String LIGANDS_ARG = "ligands";
    public static final String RECEPTORS_ARG = "receptors";
    public static final String OUTPUT_ARG = "outputjobdir";
    public static final String HELP_ARG = "h";

    public static final String DEFAULT_ARGS = "--center_x 41.1100 --center_y 34.9382 --center_z 35.8160 --size_x 25.0000 --size_y 25.0000 --size_z 25.0000 --cpu 2";
    public static final String DEFAULT_VINA_BIN = "$PANFISH_BASEDIR/home/churas/bin/autodock_vina_1_1_2/bin/vina";
    
    
    public static void main(String[] args) {
        try {
            System.out.println("AutoDockWrapper");

            OptionParser parser = new OptionParser() {

                {
                    accepts(LIGANDS_ARG).withRequiredArg().ofType(File.class).describedAs("list of ligand files");
                    accepts(RECEPTORS_ARG).withRequiredArg().ofType(File.class).describedAs("list of receptor files");
                    accepts(OUTPUT_ARG).withRequiredArg().ofType(String.class).describedAs("Output Job Directory");
                    accepts(HELP_ARG, "show help").forHelp();
                }
            };

            OptionSet optionSet = parser.parse(args);

            if (optionSet.has(HELP_ARG)) {
                parser.printHelpOn(System.out);
                System.exit(0);
            }
            
            if (!optionSet.has(LIGANDS_ARG) || 
                !optionSet.has(RECEPTORS_ARG) ||
                !optionSet.has(OUTPUT_ARG)){
                System.err.println("Missing one or more required arguments.\n");
                parser.printHelpOn(System.err);
                System.exit(1);
            }
            
            File ligandFile = (File)optionSet.valueOf(LIGANDS_ARG);
            File receptorFile =(File)optionSet.valueOf(RECEPTORS_ARG);
            String outputJobDir = (String)optionSet.valueOf(OUTPUT_ARG);
            
            System.out.println("Generating job in directory: "+outputJobDir);
            
            //create the job directory
            JobDirCreator jdc = getJobDirCreator();
            jdc.createJobDirectories(outputJobDir);
            
            
            //write out the autodock.sh file into outputjobdir
            AutoDockScriptCreator adsc = getAutoDockScriptCreator();
            adsc.createAutoDockScript(outputJobDir, DEFAULT_ARGS,DEFAULT_VINA_BIN);
            
            //generate jobs
            JobGenerator jg = getJobGenerator();
            jg.createJobs(outputJobDir, IOUtils.readLines(new FileReader(ligandFile)), 
                    IOUtils.readLines(new FileReader(receptorFile)));
            
            
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(2);
        }
    }
    
    public static JobDirCreator getJobDirCreator(){
        return new JobDirCreatorImpl();
    }
    
    public static AutoDockScriptCreator getAutoDockScriptCreator(){
        return new AutoDockScriptCreatorImpl();
    }
    
    public static JobGenerator getJobGenerator(){
        return new JobGeneratorImpl();
    }
}
