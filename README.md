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


Compilation of the Java sources:

- Java Development Kit 8
- maven

command to install all requirements on ubuntu:

sudo apt-get install openjdk-8-jdk cmake maven libboost-all-dev

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


**Compile the java sources with maven **


mvn package

mv target/DCEdit-1.0.jar .

(The working directory for the .jar must me this folder. The 'target' folder can be deleted)


**Run some examples.**

java -Xmx2048m -jar DCEdit-1.0.jar -test 5 50 10 -md

java -Xmx2048m -jar DCEdit-1.0.jar -test 5 50 10 -lazy -gforce
