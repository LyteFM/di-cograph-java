package dicograph.graphIO;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;

/*
 *   This source file is part of the program for editing directed graphs
 *   into cographs using modular decomposition.
 *   Copyright (C) 2018 Fynn Leitow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

public class SimpleMatrixImporter {

    public static SimpleDirectedGraph<Integer, DefaultEdge> importIntGraph(File file) throws IOException{
        InputStream inStream = Files.newInputStream(file.toPath());
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));

        String line;
        boolean firstRun = true;
        int numberVertices = 0;
        int outVertex = 0;

        SimpleDirectedGraph<Integer, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

        while ((line = in.readLine()) != null) {

            // handle delimiters
            if(line.isEmpty() || line.equals(SimpleMatrixExporter.lineDelimiter))
                continue;
            line = line.replaceAll(SimpleMatrixExporter.columnDelimiter,"");

            if(firstRun){
                numberVertices = line.length();

                // read the vertices
                for( int i = 0; i< line.length(); i++){
                    graph.addVertex( i);
                }
            } else {
                if(line.length() != numberVertices){
                    throw new IOException("Error: in row " + outVertex + ", Invalid Matrix line:\n" + line);
                }
            }
            for( int inVertex = 0; inVertex< line.length(); inVertex++){

                // reads and asserts the 0/1-Matrix-Entry
                char c = line.charAt(inVertex);
                assert Character.isDigit(c);
                int isEdge = Character.getNumericValue(c);
                assert isEdge == 0 || isEdge == 1;

                if(isEdge == 1){
                    graph.addEdge(outVertex, inVertex);
                }

            }

            firstRun =false;
            outVertex++;
        }

        assert outVertex == numberVertices;

        return graph;
    }

    // Not used, I need Int-Graphs
//    public static SimpleDirectedGraph<String, DefaultEdge> importStringGraph(File file) throws IOException {
//
//        InputStream inStream = Files.newInputStream(file.toPath());
//        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
//
//        String line;
//        boolean firstRun = true;
//        int numberVertices = 0;
//        int rowNumber = 0;
//
//        SimpleDirectedGraph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
//
//        while ((line = in.readLine()) != null) {
//
//            // handle delimiters
//            if(line.isEmpty() || line.equals(SimpleMatrixExporter.lineDelimiter))
//                continue;
//            line = line.replaceAll(SimpleMatrixExporter.columnDelimiter,"");
//
//            if(firstRun){
//                numberVertices = line.length();
//
//                // read the vertices as Strings
//                for( int i = 0; i< line.length(); i++){
//                    graph.addVertex( StringVertexFactory.parseInt(i) );
//                }
//            } else {
//                if(line.length() != numberVertices){
//                    throw new IOException("Error: in row " + rowNumber + ", Invalid Matrix line:\n" + line);
//                }
//            }
//            for( int inVertex = 0; inVertex< line.length(); inVertex++){
//
//                // reads and asserts the 0/1-Matrix-Entry
//                char c = line.charAt(inVertex);
//                assert Character.isDigit(c);
//                int isEdge = Character.getNumericValue(c);
//                assert isEdge == 0 || isEdge == 1;
//
//                String outVertex = StringVertexFactory.parseInt(rowNumber);
//
//                if(isEdge == 1){
//                    graph.addEdge(outVertex, StringVertexFactory.parseInt(inVertex));
//                }
//
//            }
//
//            firstRun =false;
//            rowNumber++;
//        }
//
//        assert rowNumber == numberVertices;
//
//        return graph;
//    }

}
