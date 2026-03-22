import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



import java.util.*;

public class FakeNewsProject {


    public static List<String> loadTexts(String filePath) {
        List<String> texts = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                texts.add(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return texts;
    }


    public static String clean(String text) {
        text = text.toLowerCase();
        text = text.replaceAll("[^a-zA-Z ]", "");
        return text;
    }


    public static String[] tokenize(String text) {
        return text.split("\\s+");
    }


    static class Vocabulary {
        private Set<String> vocab = new HashSet<>();

        public void addWords(String[] words) {
            vocab.addAll(Arrays.asList(words));
        }

        public int size() {
            return vocab.size();
        }
    }


    static class NaiveBayesClassifier {

        private Map<String, Integer> wordCountsReal = new HashMap<>();
        private Map<String, Integer> wordCountsFake = new HashMap<>();

        public void train(List<String> texts, List<Integer> labels) {
            for (int i = 0; i < texts.size(); i++) {
                String cleaned = clean(texts.get(i));
                String[] words = tokenize(cleaned);

                for (String word : words) {
                    if (labels.get(i) == 1) {
                        wordCountsReal.put(word, wordCountsReal.getOrDefault(word, 0) + 1);
                    } else {
                        wordCountsFake.put(word, wordCountsFake.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }

        public void printStats() {
            System.out.println("Real words: " + wordCountsReal.size());
            System.out.println("Fake words: " + wordCountsFake.size());
        }
    }


    public static void main(String[] args) {

        List<String> texts = loadTexts("data.csv");


        List<Integer> labels = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            labels.add(i % 2);
        }

        Vocabulary vocab = new Vocabulary();

        for (String text : texts) {
            String cleaned = clean(text);
            String[] tokens = tokenize(cleaned);
            vocab.addWords(tokens);
        }

        System.out.println("Vocabulary size: " + vocab.size());

        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.train(texts, labels);
        nb.printStats();
    }
}

