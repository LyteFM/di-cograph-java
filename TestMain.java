package dicograph;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Created by Fynn Leitow on 05.08.17.
 */
public class TestMain {

    public static void main(String[] args) throws Exception {
        System.out.println("Hello world!");
        System.out.println( Integer.MAX_VALUE );
        System.out.println( Long.MAX_VALUE); // –128
        System.out.println( Byte.MAX_VALUE );

        System.out.println( 20000000000F == 20000000000F+1 );
        System.out.println( 20000000000D == 20000000000D+1 );        // String name = new java.util.Scanner( System.in ).nextLine();
        // int age = new java.util.Scanner( System.in ).nextInt();
        int baba = 1;
        System.out.println( baba == 1 );   // »true«: Ausdruck mit Vergleich
        System.out.println( baba = 2 );    // »2«: Ausdruck mit Zuweisung

        System.out.println( '0' + 2 );     // 50  – ASCII value for '0' is 48
        System.out.println( 'A' + 'a' );   // 162 – 'A'=65, 'a'=97

        String input = "Kaffee";
        System.out.printf( "Kann man %s essen?", input ); // eleganter.

        //Typ     Beschreibung
        // s      Der zugehörige Parameter wird als String ausgegeben.
        // S      Der zugehörige Parameter wird als String in Großbuchstaben ausgegeben.
        // d      Der zugehörige Parameter wird als Ganzzahl ausgegeben.
        // f      Der zugehörige Parameter wird als Dezimalzahl ausgegeben.

        // double mit Toleranz: if ( Math.abs(x – y) <= epsilon )

        // break mit labels. Für switch/if-else mit Schleifen.
        // Funktioniert aber auch ohne Schleife als "goto" zum Ende eines {}-Blocks.
        String dnaBases = "CGCAGTTCTTCGGXAC";
        int a = 0, g = 0, c = 0, t = 0;

        loop:
        for ( int i = 0; i < dnaBases.length(); i++ )
        {
            switch ( dnaBases.charAt( i ) )
            {
                case 'A': case 'a':
                a++;
                break;
                case 'G': case 'g':
                g++;
                break;
                case 'C': case 'c':
                c++;
                break;
                case 'T': case 't':
                t++;
                break;
                default:
                    System.err.println( "Unbekannte Nukleinbasen " + dnaBases.charAt( i ) );
                    break loop;
            }
        }
        System.out.printf( "Anzahl: A=%d, G=%d, C=%d, T=%d%n", a, g, c, t );

        // Stack vergrößern, falls StackOverflow-Error obwohl Programm korrekt:
        // $ java –XXs:2048 MyApplication

        // Verzeichnisstruktur:
        // src/
        //        core/
        //        examples/
        //        test/
        // lib/
        // bin/

        // bytes als strings zu verwenden, ist clever.

        // Varargs: behandelt eingabe quasi wie array. Lieber so, als als Array (mächtiger)

//        static int max( int... array )
//        {
//            if ( array == null || array.length == 0 )
//                throw new IllegalArgumentException( "Array null oder leer" );
//
//            int currentMax = Integer.MIN_VALUE;
//            for ( int e : array )
//                if ( e > currentMax )
//                    currentMax = e;
//            return currentMax;
//        }

        // Clone: ist flach, kopiert nur die erste Dimension in arrays.

        // Arrays.deepequals: vergleicht auch die Elemente mit equals (nötig bei mehrdim arrays)

        boolean [] test1 = {false, true, false, true};
        boolean [] test2 = {false, true, false, true};
        System.out.println(Arrays.equals(test1,test2));
        HashMap<boolean[], String> myMap = new HashMap<>();
        myMap.put(test1, "blah");
        myMap.put(test2, "blub");
        System.out.println(myMap.toString());
        // Schade - so geht das doch nicht.
        // aber vielleicht mit einem Byte?
        System.out.println(Math.pow(2,9));// 512. Aber es gab doch nur 64 GRAPH Möglichkeiten, oder?
        // Vielleicht ist einfach String nehmen doch nicht verkehrt...
        // Oder einfach int? über bool [] to int? -> String einfacher zu debuggen...
        // In späterer Optimierung bool to int evtl.

        // Arrays.binarySearch manchmal sinnvoll (wenn array sortiert ist)


        if ( args.length == 0 )
            System.out.println( "Was!! Keiner liebt kleine Hamster?" );
        else
        {
            System.out.print( "Liebt kleine Hamster: " );

            for ( String s : args )
                System.out.format( "%s ", s );

            System.out.println();
        }

        int testy1 = 100001010; // 9 groß -> 4 bytes todo: Führende nullen... -> ok, fehlen dann ja auch bei den Keys. Von hinten anfangen!!!
        long testy2 = 1000100010001000L; // 16 groß -> 8 bytes
        String aa = "100001010"; // 9*2 ->18 bytes
        String bb = "1000100010001000"; // 16*2 -> 32 bytes
        System.out.println( testy2 );
        // String hat allerdings den vorteil, dass man Elemente einfacher ändern kann.
        // Oder doch einfach ints verwenden und mit .toBinaryString() darstellen?
        int acht = 8;
        System.out.println( Integer.toBinaryString(acht)); // keine führenden nullen...


        // Scanner: zum zeilenweisen Einlesen von Datein

        Scanner scanner = new Scanner( new File("src/dicograph/README.md") );
        while ( scanner.hasNextLine() )
            System.out.println( scanner.nextLine() );
        scanner.close();

        String s = "Faulheit ist der Hang zur Ruhe ohne vorhergehende Arbeit";
        StringTokenizer tokenizer = new StringTokenizer( s );
        while ( tokenizer.hasMoreTokens() )
            System.out.println( tokenizer.nextToken() );

        for ( String charsetName : Charset.availableCharsets().keySet() ) {
            System.out.println(charsetName);


        }

        for ( int i = 0x0; i <= 0xf; i++ )
            System.out.printf( "%x%n", i );

        for ( int i = 1 ; i < 11; i++ )
            System.out.printf( "%02d%n", i );  // 01 02 ... 10

        // So schön effizient:
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter( sb );

        for ( double d = 0; d <= 1; d += 0.1 )
            formatter.format( "%.1f%n", d );

        System.out.println( formatter );


        // todo: Kapitel 5

        /*
        Player parry = new Player().name( "Parry" ).item( "Helm" );
        System.out.println( parry.name() );                     // Parry
        System.out.println( parry.id() );                       // Parry hat Helm
        */

    }

    public static int sign( int n )
    {
        return (n < 0) ? -1 : (n > 0) ? 1 : 0;
    }


}
