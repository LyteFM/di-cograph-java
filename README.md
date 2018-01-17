###Directed Cograph Editing
Java command-line tool to edit a directed graph with n vertices, vertices named 0 to n-1, into a di-cograph.
Supported input: .dot, .txt as matrix with 0/1 entries, .jtxt for JGraphT-toString() format.

####Requirements:
- boost 1.55 or higher
- cmake
- gcc
- jdk 1.8
- maven
- Cplex v.12.7 (older versions might work, too)

####Installation:
1. Compile the necessary C++/C sources. Use $ make -j 4 with four cores.

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

2. Add your CPlex-jar to your local maven repository. Other dependencies will be downloaded automatically.
