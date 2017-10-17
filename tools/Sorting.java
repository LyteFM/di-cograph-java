package dicograph.tools;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Fynn Leitow on 17.10.17.
 */
public class Sorting {

    public static ArrayList<Set<Integer>> bucketSortBySize(ArrayList<Set<Integer>> input) {

        int[] sizes = new int[input.size()];
        int max = 0;
        // Bsp input: 6, 5, 3, 5 als sz

        // initialize size
        for (int i = 0; i < input.size(); i++) {
            int size = input.get(i).size();
            sizes[i] = size;
            if (size > max)
                max = size;
        }

        // initialize bucket
        int[] bucket = new int[max + 1];
        ArrayList<ArrayList<Set<Integer>>> bucketSets = new ArrayList<>(max + 1);
        for (int j = 0; j < bucket.length; j++) {
            bucket[j] = 0;
            bucketSets.add(j, new ArrayList<>());
        }
        // add entries for the existing sizes
        for (int i = 0; i < sizes.length; i++) {
            bucket[sizes[i]]++;
            bucketSets.get(sizes[i]).add(input.get(i));
            // bucket[6] = 1; bucket [5] = 2; bucket [3] = 1
            // bucketSets[6] hat {6}, [5] hat {5a, 5b}, [3] hat {3}
        }

        int returnPos = 0;
        ArrayList<Set<Integer>> ret = new ArrayList<>(input.size());

        for (int i = 0; i < bucket.length; i++) {
            for (int j = 0; j < bucket[i]; j++) {
                // Hier rein, wenn's Eintrag gibt
                Set<Integer> retSet = bucketSets.get(i).get(j);
                ret.add(returnPos++, retSet);
            }
        }

        return ret;
    }
}
