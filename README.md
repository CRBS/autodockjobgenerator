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

To run simply invoke this command replacing 1.0 with the current version and "-h" flag for usage.

java -jar target/autodockjobgenerator-1.0-jar-with-dependencies.jar -h
