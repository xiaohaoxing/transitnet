package whu.edu.cs.transitnet.hytra.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class PostingList {
    public static HashMap<String, HashSet<Integer>> CT = new HashMap<>();

    public static HashMap<String, HashSet<Integer>> CP = new HashMap<>();

    public static HashMap<Integer, List<String>> TC = new HashMap<>();

    public static HashMap<Integer, List<Integer>> TP = new HashMap<>();

    public static HashMap<Integer, HashSet<Integer>> GT = new HashMap<>();

    public static HashMap<Integer, Integer> TlP = new HashMap<>();

}
