package edu.uob;

import java.util.ArrayList;
import java.util.Arrays;

public class Parser {
    private static String[] specialCharacters = {"(", ")", ",", ";"};
    private ArrayList<String> tokens;
    private int i;                                                    // For the index of the tokens we are at tokens[i]
    private Node temp;
    public Parser ( ArrayList<String> tokens)
    {
        this.tokens = tokens;
        i = 0;
    }






    public Node buildTree()
    {
        return parseIt();
    }
    public Node parseIt()
    {

    }








}
