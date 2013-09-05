package edu.ucsd.crbs.autodockjobgenerator;

import joptsimple.OptionParser;

import java.io.File;
import joptsimple.OptionSet;
import edu.ucsd.crbs.autodockjobgenerator.io.*;
import edu.ucsd.crbs.autodockjobgenerator.job.JobGenerator;
import edu.ucsd.crbs.autodockjobgenerator.job.JobGeneratorImpl;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import joptsimple.OptionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the main entry class for AutoDockJobGenerator. AutoDockJobGenerator is a
 * program that generates Auto Dock Vina jobs suitable for running via Panfish
 * on HPC resources.<P/>
 * 
 * This program invokes helper classes to do four main tasks.  The links
 * below describe these tasks.<P/>
 * 
 * {@link edu.ucsd.crbs.autodockjobgenerator.io.JobDirCreatorImpl}<BR/>
 * {@link edu.ucsd.crbs.autodockjobgenerator.io.AutoDockScriptCreatorImpl}<BR/>
 * {@link edu.ucsd.crbs.autodockjobgenerator.io.PanfishAutoDockScriptCreatorImpl}<BR/>
 * {@link edu.ucsd.crbs.autodockjobgenerator.job.JobGeneratorImpl}<BR/>
 * 
 *
 * @author Christopher Churas
 */
public class App {

    final static Logger logger = LoggerFactory.getLogger(App.class);
    
    
    public static final String PROGRAM_SUMMARY = "\nAutoDockJobGenerator generates grid enabled auto dock vina jobs\n";
                                    
        public static final String EXAMPLE_USAGE = "\n\nExample invocation:\n\n"
            + "java -jar autodockjobgenerator.jar --ligands ~/ligandsdir --receptors ~/receptorsdir --outputjobdir ~/jobdir\n\n"
            + "The above command would look for any *.pdbqt in ~/ligandsdir and look for any *.pdbqt in ~/receptorsdir and\n"
            + "generate a job for every combination of ligand and receptor file.  These jobs would be batched for efficiency\n"
            + "and invokable via the generated scripts autodock.sh and panfish_autodock.sh written under ~/jobdir.\n"
            + "To reduce IO all input files are copied into ~/jobdir/inputs and stored in compressed\n"
            + "tarballs which are uncompressed only when the job runs on the local storage of the compute\n"
            + "node.\n";
    
    static final String USE_TEST_DATA = "usetestdata";
    static final String LIGANDS_ARG = "ligands";
    static final String RECEPTORS_ARG = "receptors";
    static final String OUTPUT_ARG = "outputjobdir";
    static final String SUBJOBS_ARG = "subjobs";
    static final String PERCENT_CPU_ARG = "cpuload";
    static final String VINA_BIN = "autodockbin";
    static final String CONFIG_ARGS = "configargs";
    static final String HELP_ARG = "h";
    static long THREAD_SLEEP_TIME = 20000;

    /**
     * The default arguments passed to Auto Dock jobs: {@value}
     */
    public static final String DEFAULT_ARGS = "--center_x 41.1100 --center_y 34.9382 --center_z 35.8160 --size_x 25.0000 --size_y 25.0000 --size_z 25.0000 --cpu 2";
    
    /**
     * The default path to Auto Dock vina binary: {@value}
     */
    public static final String DEFAULT_VINA_BIN = Constants.PANFISH_BASEDIR_VAR_NAME+"/home/churas/bin/autodock_vina_1_1_2/bin/vina";
    

    /**
     * Command line application that generates Auto Dock Vina jobs
     */
    public static void main(String[] args) {
        try {

            OptionParser parser = new OptionParser() {

                {   
                    accepts(USE_TEST_DATA,"Generates a small test job using test data");
                    accepts(LIGANDS_ARG,"(Required) either directory containing ligand files or file listing ligand files").withRequiredArg().ofType(File.class).describedAs("file or directory");
                    accepts(RECEPTORS_ARG,"(Required) either directory containing receptor files or file listing receptor files").withRequiredArg().ofType(File.class).describedAs("file or directory");
                    accepts(OUTPUT_ARG,"(Required) directory to write generated jobs").withRequiredArg().ofType(String.class).describedAs("directory");
                    accepts(SUBJOBS_ARG,"Number of subjobs to batch per job (default 400).").withRequiredArg().ofType(Integer.class).describedAs("# subjobs");
                    accepts(PERCENT_CPU_ARG,"Percentage of cores to use for job generation (default 90).").withRequiredArg().ofType(Integer.class).describedAs("% of cores");
                    accepts(VINA_BIN,"Path to autodock binary (default "+DEFAULT_VINA_BIN+")").withRequiredArg().ofType(String.class).describedAs("Auto Dock Binary Path");
                    accepts(CONFIG_ARGS,"Autodock configuration parameters (default "+DEFAULT_ARGS+")").withRequiredArg().ofType(String.class).describedAs("auto dock parameters");
                    accepts(HELP_ARG, "Show help").forHelp();
                }
            };
            OptionSet optionSet = null;
            try {
                optionSet = parser.parse(args);
            }
            catch(OptionException oe){
                System.err.println();
                System.err.println("There was an error parsing arguments: "+oe.getMessage());
                System.err.println();
                parser.printHelpOn(System.err);
                System.exit(1);
            }

            if (optionSet.has(HELP_ARG)) {
                System.out.println(PROGRAM_SUMMARY);
                parser.printHelpOn(System.out);
                
                System.out.println(EXAMPLE_USAGE);
                System.exit(0);
            }
            
            File ligandFile;
            File receptorFile;
            
            if (!optionSet.has(App.OUTPUT_ARG)){
                System.err.println("Missing "+OUTPUT_ARG+" which is a required argument");
                parser.printHelpOn(System.err);
                System.exit(1);
            }
            
            String outputJobDir = (String)optionSet.valueOf(OUTPUT_ARG);
            
            if (!optionSet.has(App.USE_TEST_DATA)) {

                //Set alternate batching if detected
                if (optionSet.has(SUBJOBS_ARG)) {
                    Integer val = (Integer) optionSet.valueOf(SUBJOBS_ARG);
                    Constants.SUB_JOBS_PER_JOB = val.intValue();
                    if (Constants.SUB_JOBS_PER_JOB <= 0) {
                        System.err.println("# subjobs per job must be greater then 0");
                        parser.printHelpOn(System.err);
                        System.exit(1);
                    }
                }

                if (!optionSet.has(LIGANDS_ARG)
                        || !optionSet.has(RECEPTORS_ARG)) {
                    logger.error("Missing one or more required arguments");
                    parser.printHelpOn(System.err);
                    System.exit(1);
                }
                ligandFile = (File)optionSet.valueOf(LIGANDS_ARG);
                receptorFile =(File)optionSet.valueOf(RECEPTORS_ARG);
            }
            else {
                //copy the test data from internal resource and set ligandFile
                //and receptorFile to appropriate path
                System.out.println("Flag "+App.USE_TEST_DATA+" set using TEST data!");
                
                String testDataDir = outputJobDir+File.separator+"testdata";
                String testReceptorDir = testDataDir+File.separator+"receptor";
                String testLigandDir = testDataDir+File.separator+"ligand";
                FileUtils.forceMkdir(new File(testReceptorDir));
                FileUtils.forceMkdir(new File(testLigandDir));
                
                FileWriter fw;
                for (int i = 1 ; i < 3; i++){
                    fw = new FileWriter(testReceptorDir+File.separator+Integer.toString(i)+".receptor.pdbqt");
                    IOUtils.copy(Class.class.getResourceAsStream("/"+Integer.toString(i)+".receptor.pdbqt"), fw);
                    fw.flush();
                    fw.close();
                }
                

                for (int i = 1; i < 5; i++){
                    fw =  new FileWriter(testLigandDir+File.separator+Integer.toString(i)+".ligand.pdbqt");
                    IOUtils.copy(Class.class.getResourceAsStream("/"+Integer.toString(i)+".ligand.pdbqt"), fw);
                    fw.flush();
                    fw.close();
                }
                
                ligandFile = new File(testLigandDir);
                receptorFile = new File(testReceptorDir);
                Constants.SUB_JOBS_PER_JOB = 2;
                THREAD_SLEEP_TIME = 1000;
            }
            
            double percentCoresToUse = Constants.PERCENT_OF_CORES_TO_USE_FOR_COMPRESSION;
            
            if (optionSet.has(PERCENT_CPU_ARG)){
                Integer userCpuLoad = (Integer)optionSet.valueOf(PERCENT_CPU_ARG);
                
                if (userCpuLoad.intValue() <= 0 || userCpuLoad.intValue() > 100){
                    logger.error(PERCENT_CPU_ARG+" parameter must be given a value between 1 and 100");
                    parser.printHelpOn(System.err);
                    System.exit(1);
                }
                percentCoresToUse = (double)userCpuLoad.intValue()/100.0;
            }
            
            String vinaBinPath = DEFAULT_VINA_BIN;
            
            if (optionSet.has(VINA_BIN)){
                vinaBinPath = (String)optionSet.valueOf(VINA_BIN);
                if (!vinaBinPath.startsWith(Constants.PANFISH_BASEDIR_VAR_NAME)){
                    vinaBinPath = Constants.PANFISH_BASEDIR_VAR_NAME+vinaBinPath;
                }
            }
            
            String vinaArgs = DEFAULT_ARGS;
            
            if (optionSet.has(CONFIG_ARGS)){
                vinaArgs = (String)optionSet.valueOf(CONFIG_ARGS);
            }
            
            System.out.println("Generating job in directory: "+outputJobDir);
            System.out.println("Batching: "+Constants.SUB_JOBS_PER_JOB+" sub jobs per job");

            System.out.println("Path to auto dock set to: "+vinaBinPath);
            System.out.println("Auto Dock Configuration parameters set to:"+vinaArgs);
            
            //create the job directory
            JobDirCreator jdc = getJobDirCreator();
            jdc.createJobDirectories(outputJobDir);
            
            
            //write out the autodock.sh file into outputjobdir
            AutoDockScriptCreator adsc = getAutoDockScriptCreator();
            adsc.createAutoDockScript(outputJobDir, vinaArgs,vinaBinPath);
            
              //write out the panfish auto dock script file into outputjobdir
            PanfishAutoDockScriptCreator padc = getPanfishAutoDockScriptCreator();
            padc.createPanfishAutoDockScript(outputJobDir);
            
            
            FileListGenerator listGen = getFileListGenerator();
            
            ExecutorService es = getExecutorService(percentCoresToUse);
            List<Future> taskList = Collections.synchronizedList(new LinkedList<Future>());
            //generate jobs
            JobGenerator jg = getJobGenerator(es,taskList,outputJobDir, 
                    listGen.getFileList(ligandFile.getCanonicalPath()), 
                    listGen.getFileList(receptorFile.getCanonicalPath()));
            
            long startTime = System.currentTimeMillis();
            taskList.add(es.submit(jg));
            
            
            threadSleep(THREAD_SLEEP_TIME);
            long totalJobs = jg.getTotalJobs();
            long percentComplete;
            
            //okay we submitted the jobs now we can let the user know the status
            //by looking at the count of tasks remaining
            int removeCount = 0;
            while(taskList.size() > 0){
               percentComplete = getPercentComplete(removeCount,totalJobs);
               System.out.println(percentComplete+"% ("+removeCount+" of "+totalJobs+") job(s) created.  "+estimatedTimeRemaining(removeCount,totalJobs,startTime));
               threadSleep(THREAD_SLEEP_TIME);
               removeCount += removeCompletedTasks(taskList);
            }
            
            es.shutdown();
            
            System.out.println("Job generation complete.  Total Time: "+
                     Math.round((double)(System.currentTimeMillis() - startTime)/1000.0)+
                    " seconds.");
            File outputJobDirFile = new File(outputJobDir);
            System.out.println("Job created in: "+
                    outputJobDirFile.getCanonicalPath());
            
            System.out.println("Be sure to verify "+Constants.VINA+" and "+Constants.ARGUMENTS+" lines in: "+
                    outputJobDirFile.getCanonicalPath()+File.separator+
                    Constants.AUTO_DOCK_SCRIPT+ " file are correct before running");
            
            System.out.println("To run via panfish invoke:");
            
            System.out.println("cd "+outputJobDirFile.getCanonicalPath()+
                               ";."+File.separator+Constants.PANFISH_AUTO_DOCK_SCRIPT);
            
            System.out.println();
            
            System.out.println("To run directly in serial fashion invoke: ");
            
            //@todo if we are on windows this should be writing a different command
            System.out.println("cd "+outputJobDirFile.getCanonicalPath()+
                    ";for Y in `seq 1 "+Long.toString(totalJobs)+
                    "` ; do echo \"Running job $Y\";export SGE_TASK_ID=$Y;."+File.separator+Constants.AUTO_DOCK_SCRIPT+" ; done");
                    
            
            
            
        } catch (Exception ex) {
            logger.error("Caught Exception.  Exiting..", ex);
            System.exit(2);
        }
    }
    
    static void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
        }
    }
    
    static String estimatedTimeRemaining(long numberCompleted,long totalCount,long startTime){
        long curTime = System.currentTimeMillis();
        if (numberCompleted <= 0){
            return "Unknown time remaining.";
        }
        long elapsedTime = curTime - startTime;
        
        //using time elapsed and # jobs completed get a total time estimate.
        double estTimeMillis = (double)(elapsedTime*totalCount)/(double)numberCompleted;
        
        //use total time estimate to calculate time remaining
        double estMillisRemaining = estTimeMillis*(((double)totalCount-(double)numberCompleted)/(double)totalCount);
        return Long.toString(Math.round(estMillisRemaining/1000.0))+" seconds remaining.";
    }
    
    static long getPercentComplete(long numberCompleted, long totalCount){
        return Math.round(((double)numberCompleted/(double)totalCount)*100.0);
    }
    
    static FileListGenerator getFileListGenerator(){
        return new FileListGeneratorImpl();
    }
    
    static JobDirCreator getJobDirCreator(){
        return new JobDirCreatorImpl();
    }
    
    static AutoDockScriptCreator getAutoDockScriptCreator(){
        return new AutoDockScriptCreatorImpl();
    }
    
    static PanfishAutoDockScriptCreator getPanfishAutoDockScriptCreator(){
        return new PanfishAutoDockScriptCreatorImpl();
    }
    
    static JobGenerator getJobGenerator(ExecutorService es,List<Future> taskList,final String outputJobDir,List<String> ligands,List<String> receptors){
        return new JobGeneratorImpl(es,taskList,outputJobDir,ligands,receptors);
    }
    
     /**
     * This method creates an {@link java.util.concurrent.ExecutorService} with a thread pool size
     * set to percentOfCoresToUse passed in X
     * {@link java.lang.Runtime.getRuntime()#availableProcessors() Runtime.getRuntime().availableProcessors()} or 1 if value is less then 1.
     * @param percentOfCoresToUse The percentage of cores to use and should be a value between 0 and 1.  For example 0.9 means 90%
     * @return ExecutorService
     */
    static ExecutorService getExecutorService(double percentOfCoresToUse){
        
        //Create a threadpool that is 90% size of number of processors on system
        //with a minimum size of 1.
        int threadPoolSize = (int)Math.round((double)Runtime.getRuntime().availableProcessors()*
                                              percentOfCoresToUse);
        if (threadPoolSize < 1){
            threadPoolSize = 1;
        }
        logger.debug("Creating threadpool size of {} to compress all the input directories",threadPoolSize);
        return Executors.newFixedThreadPool(threadPoolSize);
    }
    
    static int removeCompletedTasks(List<Future> taskList){
        Future f;
        int removeCount = 0;
        Iterator<Future> itr = taskList.iterator();
        while(itr.hasNext()){
            f = itr.next();
            if (f.isDone() || f.isCancelled()){
                removeCount++;
                itr.remove();
            }
        }
        return removeCount;
    }
}
