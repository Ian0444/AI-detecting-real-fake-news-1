import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

    static class NaiveBayesClassifier {

        private Map<String, Integer> wordCountsReal = new HashMap<>();
        private Map<String, Integer> wordCountsFake = new HashMap<>();

        private int totalRealWords = 0;
        private int totalFakeWords = 0;
        private int realDocs = 0;
        private int fakeDocs = 0;

        private Set<String> vocabulary = new HashSet<>();

        public void train(List<String> texts, List<Integer> labels) {
            for (int i = 0; i < texts.size(); i++) {

                String cleaned = clean(texts.get(i));
                String[] words = tokenize(cleaned);

                if (labels.get(i) == 1) {
                    realDocs++;
                } else {
                    fakeDocs++;
                }

                for (String word : words) {
                    vocabulary.add(word);

                    if (labels.get(i) == 1) {
                        wordCountsReal.put(word, wordCountsReal.getOrDefault(word, 0) + 1);
                        totalRealWords++;
                    } else {
                        wordCountsFake.put(word, wordCountsFake.getOrDefault(word, 0) + 1);
                        totalFakeWords++;
                    }
                }
            }
        }

        public int predict(String text) {
            String[] words = tokenize(clean(text));

            double logReal = Math.log((double) realDocs / (realDocs + fakeDocs));
            double logFake = Math.log((double) fakeDocs / (realDocs + fakeDocs));

            int vocabSize = vocabulary.size();

            for (String word : words) {
                int realCount = wordCountsReal.getOrDefault(word, 0);
                int fakeCount = wordCountsFake.getOrDefault(word, 0);

                double probWordReal = (realCount + 1.0) / (totalRealWords + vocabSize);
                double probWordFake = (fakeCount + 1.0) / (totalFakeWords + vocabSize);

                logReal += Math.log(probWordReal);
                logFake += Math.log(probWordFake);
            }

            return logReal > logFake ? 1 : 0;
        }

        public double test(List<String> texts, List<Integer> labels) {
            int correct = 0;

            for (int i = 0; i < texts.size(); i++) {
                int prediction = predict(texts.get(i));
                if (prediction == labels.get(i)) {
                    correct++;
                }
            }

            return (double) correct / texts.size();
        }

        public void printStats() {
            System.out.println("Vocabulary size: " + vocabulary.size());
            System.out.println("Real docs: " + realDocs);
            System.out.println("Fake docs: " + fakeDocs);
        }
    }

    public static void main(String[] args) {

        List<String> texts = loadTexts("data.csv");

        List<Integer> labels = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            labels.add(i % 2); // fake split just for testing
        }

        NaiveBayesClassifier nb = new NaiveBayesClassifier();

        nb.train(texts, labels);
        nb.printStats();

        double accuracy = nb.test(texts, labels);
        System.out.println("Accuracy: " + accuracy);

        String sample = "Breaking news government scandal shocking report";
        int result = nb.predict(sample);

        System.out.println("Sample prediction: " + (result == 1 ? "Real" : "Fake"));
    }
}

