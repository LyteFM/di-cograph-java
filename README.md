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

make

cd ../..


cd MD/build/

cmake ..

make

cd ../..


cd OverlapComponentProg/

make


The maven-steps can be skipped if you directly use the DCEdit-1.0.jar and don't want to compile the java sources.

**Add CPlex-jar to your local maven repository (change the path) and compile.**

mvn install:install-file -Dfile=/Users/praktikant/Applications/IBM/ILOG/CPLEX_Studio1271/cplex/lib/cplex.jar -DgroupId=ilog -DartifactId=cplex -Dversion=12.7 -Dpackaging=jar

mvn package

mv target/DCEdit-1.0.jar .

(the 'target' folder can be deleted)

**Run some examples. Specify the path to your Cplex-binaries-folder if using ILP**

java -jar DCEdit-1.0.jar -test 5 50 10 -MD

java -Djava.library.path=/Users/praktikant/Applications/IBM/ILOG/CPLEX_Studio1271/cplex/bin/x86-64_osx -jar DCEdit-1.0.jar -test 5 50 10 -gilp -gforce -v
