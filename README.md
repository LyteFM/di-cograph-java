## Directed Cograph Editing
Java command-line tool to edit a directed graph with n vertices, vertices named 0 to n-1, into a di-cograph.
Changes to all files that were not created by myself are denoted by comments starting with: "//F.L.".
Changed were: ModDecomp.cpp /.h in MD, also Commons.h in FP_to_DMD; all files except SplitDirection, RecSubProblem and FactPermElement in src/java/dicograph/modDecomp.
Supported input: .dot, .txt as matrix with 0/1 entries, .jtxt for JGraphT-toString() format. For .dot and .jtxt, the vertices must be labeled from 0 to n-1.

Copyright (C) 2018 Fynn Leitow

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.


### Requirements:
Comilation of the C++ sources (necessary)
- boost 1.55 or higher
- cmake
- gcc

Running the .jar (necessary):

- Java Runtime Environment 8

Compilation of the Java sources (optional):

- Java Development Kit 8
- maven
- Cplex v.12.7 (older versions might work, too)

### Installation:

**Compile the necessary C++/C sources. Use $ make -j x with x cores and 2GB RAM per core.**

mkdir FP_to_DMD/build MD/build logs

cd FP_to_DMD/build/

cmake ..

make -j 2

cd ../../MD/build/

cmake ..

make -j 2

cd ../../OverlapComponentProg/

make -j 2

cd ..

The maven-steps can be skipped if you directly use the DCEdit-1.0.jar and don't want to compile the java sources.

**Add CPlex-jar to your local maven repository and compile. (change the path; for a different Cplex version you can either keep -Dversion or also change the POM.xml) **

mvn install:install-file -Dfile=/opt/ibm/ILOG/CPLEX_Studio1271/cplex/lib/cplex.jar -DgroupId=ilog -DartifactId=cplex -Dversion=12.7 -Dpackaging=jar

mvn package

mv target/DCEdit-1.0.jar .

(the 'target' folder can be deleted)

**Run some examples. Specify the path to your Cplex-binaries-folder if using ILP**

java -Xmx2048m -jar DCEdit-1.0.jar -test 5 50 10 -MD

java -Xmx2048m -jar DCEdit-1.0.jar -test 5 50 10 -lazy -gforce

java -Xmx2048m -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio1271/cplex/bin/x86-64_linux -jar DCEdit-1.0.jar -test 5 50 10 -lazy -ilp  -v


**Important note on memory management:**

For larger n, the JVM will require more memory. Increase it e.g. to 2GB by adding -Xmx2048m as first java-option.

When running CPlex for larger n, the RAM will fill up quickly and a large enough swap file will be required.
While OSX manages the Swap dynamically, on Linux a large-enough file must be allocated, else the kernel will kill the process.
For n=50, limiting the number of threads to 4 on an 8-core system by adding -threads 4 resulted in full 16 GB memory + 8-10 GB Swap usage.
According to the documentation, IloCplex.Param.WorkMem should limit the memory usage; It can be changed with e.g. with -mem 64 but did not help much.

