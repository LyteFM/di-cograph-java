## Directed Cograph Editing
Java command-line tool to edit a directed graph with n vertices, vertices named 0 to n-1, into a di-cograph.
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

**Compile the necessary C++/C sources. Use $ make -j 4 with four cores.**

mkdir FP_to_DMD/build MD/build logs

cd FP_to_DMD/build/

cmake ..

make -j 4

cd ../..


cd MD/build/

cmake ..

make -j 4

cd ../..


cd OverlapComponentProg/

make -j 4


The maven-steps can be skipped if you directly use the DCEdit-1.0.jar and don't want to compile the java sources.

**Add CPlex-jar to your local maven repository (change the path) and compile.**

mvn install:install-file -Dfile=/opt/ibm/ILOG/CPLEX_Studio1271/cplex/lib/cplex.jar -DgroupId=ilog -DartifactId=cplex -Dversion=12.7 -Dpackaging=jar

mvn package

mv target/DCEdit-1.0.jar .

(the 'target' folder can be deleted)

**Run some examples. Specify the path to your Cplex-binaries-folder if using ILP**

java -jar DCEdit-1.0.jar -test 5 50 10 -MD

java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio1271/cplex/bin/x86-64_linux -jar DCEdit-1.0.jar -test 5 50 10 -gilp -gforce -v

**Important note on memory management:**

For larger n, the JVM will require more memory. Increase it e.g. to 2GB by adding -Xmx2048m -ea as first java-option.
Max CPlex tree size is set to same value.

2 GB per thread??? osx -> 4 threads -> ok;
ubuntu -> 8 threads -> immediately ram full. Swap fills -> crash.
ubuntu -> 4 threads, 64 mb -> Ram full, swap 8 GB -> OK!
While OSX manages the memory consumed by CPLex well by automatically adjusting the swap, the Linux kernel will simply kill the process when it consumes too much memory.
Use the method parameter -mem to limit the available memory (in MB) for CPlex.
Or make sure that your swapfile / swap partition is active and has about the same size as your RAM.

OSX - continues even if Swap runs full :O (35 GB java Speicher!)
