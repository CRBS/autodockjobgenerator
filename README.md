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

Copyright
=========

Copyright 2013   The Regents of the University of California
All Rights Reserved
 Permission to copy, modify and distribute any part of this AutoDockJobGenerator for educational, research and non-profit purposes, without fee, and without a written agreement is hereby granted, provided that the above copyright notice, this paragraph and the following three paragraphs appear in all copies.
 
Those desiring to incorporate this AutoDockJobGenerator into commercial products or use for commercial purposes should contact the Technology Transfer Office, University of California, San Diego, 9500 Gilman Drive, Mail Code 0910, La Jolla, CA 92093-0910, Ph: (858) 534-5815, FAX: (858) 534-7345, E-MAIL:invent@ucsd.edu.
 
IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS AutoDockJobGenerator, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
THE AutoDockJobGenerator PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.  THE UNIVERSITY OF CALIFORNIA MAKES NO REPRESENTATIONS AND EXTENDS NO WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE, OR THAT THE USE OF THE AutoDockJobGenerator WILL NOT INFRINGE ANY PATENT, TRADEMARK OR OTHER RIGHTS.    

