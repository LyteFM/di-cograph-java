### Directed Cograph Editing
Java command-line tool to edit a directed graph with n vertices, vertices named 0 to n-1, into a di-cograph.
Supported input: .dot, .txt as matrix with 0/1 entries, .jtxt for JGraphT-toString() format.

#### Requirements:
- boost 1.55 or higher
- cmake
- gcc
- jdk 1.8
- maven
- Cplex v.12.7 (older versions might work, too)

#### Installation:

**Compile the necessary C++/C sources. Use $ make -j 4 with four cores.**
mkdir FP_to_DMD/build MD/build target target/logs target/graphs

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

**Add CPlex-jar to your local maven repository (change the path) and compile.**

mvn install:install-file -Dfile=/Users/praktikant/Applications/IBM/ILOG/CPLEX_Studio1271/cplex/lib/cplex.jar -DgroupId=ilog -DartifactId=cplex -Dversion=12.7 -Dpackaging=jar
mvn package

**Run some examples. Specify the path to your Cplex-binaries-folder if using ILP-Solver.**

cd target
java -jar DCEdit.jar -test 5 50 10 -MD -log
java -Djava.library.path=/Users/praktikant/Applications/IBM/ILOG/CPLEX_Studio1271/cplex/bin/x86-64_osx -jar DCEdit.jar test 5 50 10 -gilp -gforce -v
