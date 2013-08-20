
AutoDockWrapper is a command line java application that generates Auto Dock jobs from a set
of ligands and receptors that can be run directly or via Panfish on compute clusters.  



Build
=====

To test:

mvn test

To compile the code and make the jar run the following:

mvn clean package assembly:single


Run
===

To run simply invoke this command replacing 1.0-SNAPSHOT with the current version:

java -jar target/autodockwrapper-1.0-SNAPSHOT-jar-with-dependencies.jar -h

The above should output something similar to the following:


Missing outputjobdir which is a required argument
Option                                 Description                           
------                                 -----------                           
-h                                     Show help                             
--ligands <File: file or directory>    (Required) either directory containing
                                         ligand files or file listing ligand 
                                         files                               
--outputjobdir <directory>             (Required) directory to write         
                                         generated jobs                      
--receptors <File: file or directory>  (Required) either directory containing
                                         receptor files or file listing      
                                         receptor files                      
--subjobs <Integer: # subjobs>         Number of subjobs to batch per job.   
                                         Default 400                         
--usetestdata                          Generates a small test job using test 
                                         data                                

Example invocation:

java -jar autodockwrapper.jar --ligands ~/ligandsdir --receptors ~/receptorsdir --outputjobdir ~/jobdir

The above command would look for any *.pdbqt in ~/ligandsdir and look for any *.pdbqt in ~/receptorsdir and
generate a job for every combination of ligand and receptor file.  These jobs would be batched for efficiency
and invokable via the generated scripts autodock.sh and panfish_autodock.sh written under ~/jobdir.
To reduce IO all input files are copied into ~/jobdir/inputs and stored in compressed
tarballs which are uncompressed only when the job runs on the local storage of the compute
node.

