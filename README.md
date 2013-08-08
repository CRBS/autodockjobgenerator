Build
=====

mvn clean package assembly:single

mvn clean package assembly:single;java -jar target/autodockwrapper-1.0-SNAPSHOT-jar-with-dependencies.jar --ligands /home/churas/tests/autodock/data/ligand.list --receptors /home/churas/tests/autodock/data/receptor.list --outputjobdir ./foo

Run
===

