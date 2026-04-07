import java.io.*;
import java.util.*;

public class FakeNewsProject {


    static Set<String> stopWords = new HashSet<>(Arrays.asList(
            "the", "is", "a", "an", "and", "or", "to", "of", "in", "on",
            "for", "with", "at", "by", "from", "that", "this", "it"
    ));


    public static List<String> loadTexts(String filePath) {
        List<String> texts = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine();

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


    public static List<String> removeStopWords(String[] words) {
        List<String> filtered = new ArrayList<>();

        for (String word : words) {
            if (!stopWords.contains(word) && !word.isEmpty()) {
                filtered.add(word);
            }
        }

        return filtered;
    }


    public static int splitIndex(int size, double ratio) {
        return (int) (size * ratio);
    }


    static class NaiveBayesClassifier {

        private Map<String, Integer> realWordCounts = new HashMap<>();
        private Map<String, Integer> fakeWordCounts = new HashMap<>();

        private int totalRealWords = 0;
        private int totalFakeWords = 0;
        private int realDocs = 0;
        private int fakeDocs = 0;

        private Set<String> vocabulary = new HashSet<>();

        public void train(List<String> texts, List<Integer> labels) {

            for (int i = 0; i < texts.size(); i++) {

                List<String> words = removeStopWords(
                        tokenize(clean(texts.get(i)))
                );

                int label = labels.get(i);

                if (label == 1) realDocs++;
                else fakeDocs++;

                for (String word : words) {
                    vocabulary.add(word);

                    if (label == 1) {
                        realWordCounts.put(word,
                                realWordCounts.getOrDefault(word, 0) + 1);
                        totalRealWords++;
                    } else {
                        fakeWordCounts.put(word,
                                fakeWordCounts.getOrDefault(word, 0) + 1);
                        totalFakeWords++;
                    }
                }
            }
        }

        public int predict(String text) {

            List<String> words = removeStopWords(
                    tokenize(clean(text))
            );

            double logReal = Math.log((double) realDocs / (realDocs + fakeDocs));
            double logFake = Math.log((double) fakeDocs / (realDocs + fakeDocs));

            int vocabSize = vocabulary.size();

            for (String word : words) {

                int realCount = realWordCounts.getOrDefault(word, 0);
                int fakeCount = fakeWordCounts.getOrDefault(word, 0);

                double probReal =
                        (realCount + 1.0) / (totalRealWords + vocabSize);

                double probFake =
                        (fakeCount + 1.0) / (totalFakeWords + vocabSize);

                logReal += Math.log(probReal);
                logFake += Math.log(probFake);
            }

            return logReal > logFake ? 1 : 0;
        }

        public void evaluate(List<String> testTexts, List<Integer> testLabels) {

            int correct = 0;
            int tp = 0, tn = 0, fp = 0, fn = 0;

            for (int i = 0; i < testTexts.size(); i++) {
                int pred = predict(testTexts.get(i));
                int actual = testLabels.get(i);

                if (pred == actual) correct++;

                if (pred == 1 && actual == 1) tp++;
                if (pred == 0 && actual == 0) tn++;
                if (pred == 1 && actual == 0) fp++;
                if (pred == 0 && actual == 1) fn++;
            }

            double accuracy = (double) correct / testTexts.size();
            double precision = tp / (double)(tp + fp + 1);
            double recall = tp / (double)(tp + fn + 1);
            double f1 = 2 * precision * recall / (precision + recall + 1e-9);

            System.out.println("Accuracy: " + accuracy);
            System.out.println("Precision: " + precision);
            System.out.println("Recall: " + recall);
            System.out.println("F1 Score: " + f1);

            System.out.println("\nConfusion Matrix");
            System.out.println("TP: " + tp + " FP: " + fp);
            System.out.println("FN: " + fn + " TN: " + tn);
        }

        public void printTopWords() {
            System.out.println("\nTop Real Words:");
            realWordCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(10)
                    .forEach(System.out::println);

            System.out.println("\nTop Fake Words:");
            fakeWordCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(10)
                    .forEach(System.out::println);
        }
    }


    public static void main(String[] args) {

        List<String> texts = loadTexts("data.csv");

        List<Integer> labels = new ArrayList<>();
        Random rand = new Random();


        for (int i = 0; i < texts.size(); i++) {
            labels.add(rand.nextInt(2));
        }

        int split = splitIndex(texts.size(), 0.8);

        List<String> trainTexts = texts.subList(0, split);
        List<Integer> trainLabels = labels.subList(0, split);

        List<String> testTexts = texts.subList(split, texts.size());
        List<Integer> testLabels = labels.subList(split, labels.size());

        NaiveBayesClassifier model = new NaiveBayesClassifier();

        model.train(trainTexts, trainLabels);

        model.evaluate(testTexts, testLabels);

        model.printTopWords();

        String sample = "Government releases shocking economic report";
        int result = model.predict(sample);

        System.out.println("\nSample Prediction: " +
                (result == 1 ? "REAL" : "FAKE"));
    }
}

