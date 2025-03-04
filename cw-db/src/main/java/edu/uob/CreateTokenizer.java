package edu.uob;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateTokenizer {
    private String[] specialCharacters = {"(", ")", ",", ";"};
    private String query;
    private ArrayList<String> tokens;
    public CreateTokenizer(String query) {
        this.query = query;
        tokens = new ArrayList<>();
    }
    public  ArrayList<String> tokenizeQuery() {
        ArrayList<String> tokens = new ArrayList<>();

        // Split the query on single quotes (to handle string literals properly)
        String[] fragments = query.split("'");

        for (int i = 0; i < fragments.length; i++) {
            // If it's a string literal, add it as a separate token
            if (i % 2 != 0) {
                tokens.add("'" + fragments[i] + "'");
            } else {
                // Tokenize non-string parts
                String[] nextBatchOfTokens = tokenise(fragments[i]);
                tokens.addAll(Arrays.asList(nextBatchOfTokens));
            }
        }
        return tokens;
    }

    private  String[] tokenise(String input) {
        // Add spaces around special characters for proper separation
        for (String specialChar : specialCharacters) {
            input = input.replace(specialChar, " " + specialChar + " ");
        }

        // Remove multiple spaces
        while (input.contains("  ")) {
            input = input.replace("  ", " ");
        }

        // Trim and split into tokens
        input = input.trim();
        return input.split(" ");
    }
}
