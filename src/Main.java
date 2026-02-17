import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileLoader {

    public static List<String> loadTexts(String filePath) {
        List<String> texts = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            // Skip header if dataset has one
            br.readLine();

            while ((line = br.readLine()) != null) {
                texts.add(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return texts;
    }
}


import java.util.ArrayList;
import java.util.List;

public class TextPreprocessor {

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();

        // Convert to lowercase
        text = text.toLowerCase();

        // Remove punctuation
        text = text.replaceAll("[^a-zA-Z ]", "");

        // Split by spaces
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (!word.isEmpty()) {
                tokens.add(word);
            }
        }

        return tokens;
    }
}

import java.util.List;

public class Main {

    public static void main(String[] args) {

        List<String> texts = FileLoader.loadTexts("data/news.csv");

        if (!texts.isEmpty()) {
            String sample = texts.get(0);

            List<String> tokens = TextPreprocessor.tokenize(sample);

            System.out.println("Original Text:");
            System.out.println(sample);

            System.out.println("\nTokens:");
            System.out.println(tokens);
        }
    }
}
