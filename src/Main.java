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


        private final double alpha;

        private Map<String, Integer> realWordCounts = new HashMap<>();
        private Map<String, Integer> fakeWordCounts = new HashMap<>();

        private int totalRealTokens = 0;
        private int totalFakeTokens = 0;
        private int realDocs        = 0;
        private int fakeDocs        = 0;

        private Set<String> vocabulary = new HashSet<>();


        public NaiveBayesClassifier(double alpha) {
            if (alpha <= 0) throw new IllegalArgumentException("alpha must be > 0");
            this.alpha = alpha;
        }

        public NaiveBayesClassifier() {
            this(1.0);
        }


        public void train(List<String> texts, List<Integer> labels) {

            for (int i = 0; i < texts.size(); i++) {

                List<String> words = removeStopWords(
                        tokenize(clean(texts.get(i)))
                );

                int label = labels.get(i);

                if (label == 1) realDocs++;
                else            fakeDocs++;

                for (String word : words) {
                    vocabulary.add(word);

                    if (label == 1) {
                        realWordCounts.merge(word, 1, Integer::sum);
                        totalRealTokens++;
                    } else {
                        fakeWordCounts.merge(word, 1, Integer::sum);
                        totalFakeTokens++;
                    }
                }
            }

            System.out.println("Training complete.");
            System.out.println("  Real docs     : " + realDocs);
            System.out.println("  Fake docs     : " + fakeDocs);
            System.out.println("  Vocabulary    : " + vocabulary.size() + " tokens");
            System.out.println("  Alpha (smooth): " + alpha);
        }



        public int predict(String text) {
            double[] scores = logScores(text);
            return scores[1] >= scores[0] ? 1 : 0;
        }


        public double confidence(String text) {
            double[] scores = logScores(text);

            double maxScore = Math.max(scores[0], scores[1]);
            double expFake  = Math.exp(scores[0] - maxScore);
            double expReal  = Math.exp(scores[1] - maxScore);

            return expReal / (expReal + expFake);
        }


        private double[] logScores(String text) {
            List<String> words = removeStopWords(
                    tokenize(clean(text))
            );

            int vocabSize = vocabulary.size();

            double logPriorReal = Math.log(
                    (realDocs + alpha) / (double)(realDocs + fakeDocs + 2 * alpha));
            double logPriorFake = Math.log(
                    (fakeDocs + alpha) / (double)(realDocs + fakeDocs + 2 * alpha));

            double logScoreReal = logPriorReal;
            double logScoreFake = logPriorFake;

            for (String word : words) {
                double pReal = (realWordCounts.getOrDefault(word, 0) + alpha)
                        / (double)(totalRealTokens + alpha * vocabSize);

                double pFake = (fakeWordCounts.getOrDefault(word, 0) + alpha)
                        / (double)(totalFakeTokens + alpha * vocabSize);

                logScoreReal += Math.log(pReal);
                logScoreFake += Math.log(pFake);
            }

            return new double[]{logScoreFake, logScoreReal};
        }



        public void evaluate(List<String> testTexts, List<Integer> testLabels) {

            int correct = 0;
            int tp = 0, tn = 0, fp = 0, fn = 0;

            for (int i = 0; i < testTexts.size(); i++) {
                int pred   = predict(testTexts.get(i));
                int actual = testLabels.get(i);

                if (pred == actual) correct++;

                if      (pred == 1 && actual == 1) tp++;
                else if (pred == 0 && actual == 0) tn++;
                else if (pred == 1 && actual == 0) fp++;
                else                               fn++;
            }

            double accuracy  = (double) correct / testTexts.size();
            double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
            double recall    = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
            double f1        = (precision + recall) > 0
                    ? 2.0 * precision * recall / (precision + recall)
                    : 0.0;

            System.out.println("\n--- Evaluation Results ---");
            System.out.printf("Accuracy  : %.4f (%.2f%%)%n", accuracy, accuracy * 100);
            System.out.printf("Precision : %.4f%n", precision);
            System.out.printf("Recall    : %.4f%n", recall);
            System.out.printf("F1 Score  : %.4f%n", f1);

            System.out.println("\nConfusion Matrix:");
            System.out.println("            Predicted REAL  Predicted FAKE");
            System.out.printf("Actual REAL      %5d           %5d%n", tp, fn);
            System.out.printf("Actual FAKE      %5d           %5d%n", fp, tn);
        }




        public Map<String, Double> computeLogLikelihoodRatios() {
            int vocabSize = vocabulary.size();
            Map<String, Double> llr = new HashMap<>();

            for (String word : vocabulary) {
                double pReal = (realWordCounts.getOrDefault(word, 0) + alpha)
                        / (double)(totalRealTokens + alpha * vocabSize);
                double pFake = (fakeWordCounts.getOrDefault(word, 0) + alpha)
                        / (double)(totalFakeTokens + alpha * vocabSize);
                llr.put(word, Math.log(pReal / pFake));
            }

            return llr;
        }


        public void printTopWords(int n) {
            Map<String, Double> llr = computeLogLikelihoodRatios();

            System.out.println("\nTop " + n + " indicators for REAL news:");
            llr.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(n)
                    .forEach(e -> System.out.printf("  %-20s LLR = %+.4f%n",
                            e.getKey(), e.getValue()));

            System.out.println("\nTop " + n + " indicators for FAKE news:");
            llr.entrySet().stream()
                    .sorted((a, b) -> Double.compare(a.getValue(), b.getValue()))
                    .limit(n)
                    .forEach(e -> System.out.printf("  %-20s LLR = %+.4f%n",
                            e.getKey(), e.getValue()));
        }

        public int getVocabularySize() {
            return vocabulary.size();
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

        model.printTopWords(10);

        String sample = "Government releases shocking economic report";
        int result    = model.predict(sample);
        double conf   = model.confidence(sample);

        System.out.println("\nSample Prediction : " + (result == 1 ? "REAL" : "FAKE"));
        System.out.printf("Confidence        : %.4f%n", conf);
    }
}