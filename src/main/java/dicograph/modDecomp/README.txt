- The program takes a single command line argument: the name of a file specifying a graph; the comments in "GraphHandle.java" specify the correct format for this file.

- Little or no error checking is performed by the program.

- The program is still provisional in that it has not fully been tested and changes might still be made in the future.

- Thank you to Yixin Cao of the Department of Computer Science & Engineering at Texas A & M University for alerting me to a bug.

Marc Tedder (April 10, 2010)

F.L. 2018:
Due to a not-yet identified bug in the code, it sometimes produces wrong decompositions of isomorphic input graphs.
Switching to Dr. Tedder's instead of Adrian Fritz's MD can be done by using a different constructor of the MDTree in DirectedMD.
The files in the .zip-archive can be used to reproduce the error.
The original files can be found at http://www.cs.toronto.edu/~mtedder/
