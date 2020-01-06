#!/bin/bash
# -*- ENCODING: UTF-8 -*-

echo "Generate JAR"
mkdir class
echo "Manifest-Version: 1.0
Main-Class: modelPGAS/Sorter" > class/manifest.mf 
javac -d class src/modelPGAS/*.java
cd class
jar cmf manifest.mf ../modelPGAS.jar modelPGAS/*.class
cd ..
rm -R class
echo "Finished, to run execute: java -jar modelPGAS.jar LEADER_PROCESS_IP LEADER_PROCESS_PORT PROCESS_ID GLOBAL_ARRAY_SIZE PROCESS_QUANTITY"
exit
