package edu.ucsd.crbs.autodockwrapper;

import joptsimple.OptionParser;

import java.io.File;
import joptsimple.OptionSet;

/**
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
            
            System.out.println(outputJobDir);
            System.out.println(ligandFile.getAbsolutePath());
            System.out.println(receptorFile.getAbsolutePath());
            
            
            
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(2);
        }
    }
}
