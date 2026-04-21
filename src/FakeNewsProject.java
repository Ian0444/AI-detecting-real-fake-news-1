import java.io.*;
import java.util.*;

public class FakeNewsProject {

    static class Article {
        private final String text;
        private final int label;

        public Article(String text, int label) {
            this.text  = text;
            this.label = label;
        }

        public String getText()  { return text; }
        public int    getLabel() { return label; }

        @Override
        public String toString() {
            String preview = text.length() > 60 ? text.substring(0, 60) + "..." : text;
            return "Article{label=" + (label == 1 ? "REAL" : "FAKE") + ", text=\"" + preview + "\"}";
        }
    }


    static class DataLoader {

        public static List<Article> loadCSV(String filePath,
                                            String textColumn,
                                            String labelColumn) {
            List<Article> articles = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String headerLine = br.readLine();
                if (headerLine == null) {
                    System.err.println("[DataLoader] File is empty: " + filePath);
                    return articles;
                }

                String[] headers = parseCSVLine(headerLine);
                int textIdx = -1, labelIdx = -1;

                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i].trim().toLowerCase();
                    if (h.equals(textColumn.toLowerCase()))  textIdx  = i;
                    if (h.equals(labelColumn.toLowerCase())) labelIdx = i;
                }

                if (textIdx == -1 || labelIdx == -1) {
                    System.err.println("[DataLoader] Column not found.");
                    return articles;
                }

                String line;
                int skipped = 0;

                while ((line = br.readLine()) != null) {
                    String[] fields = parseCSVLine(line);
                    int maxNeeded = Math.max(textIdx, labelIdx);

                    if (fields.length <= maxNeeded) { skipped++; continue; }

                    String rawText  = fields[textIdx].trim();
                    int    label    = parseLabel(fields[labelIdx].trim());

                    if (label == -1 || rawText.isEmpty()) { skipped++; continue; }

                    articles.add(new Article(rawText, label));
                }

                System.out.println("[DataLoader] Loaded  : " + articles.size() + " articles");
                if (skipped > 0)
                    System.out.println("[DataLoader] Skipped : " + skipped + " malformed rows");

            } catch (IOException e) {
                System.err.println("[DataLoader] Error: " + e.getMessage());
            }

            return articles;
        }

        private static int parseLabel(String raw) {
            switch (raw.toLowerCase()) {
                case "1": case "real": case "true":  return 1;
                case "0": case "fake": case "false": return 0;
                default:
                    try { return Integer.parseInt(raw) != 0 ? 1 : 0; }
                    catch (NumberFormatException e) { return -1; }
            }
        }

        public static String[] parseCSVLine(String line) {
            List<String> fields = new ArrayList<>();
            StringBuilder sb    = new StringBuilder();
            boolean inQuotes    = false;

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"') {
                    if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"'); i++;
                    } else {
                        inQuotes = !inQuotes;
                    }
                } else if (c == ',' && !inQuotes) {
                    fields.add(sb.toString()); sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
            fields.add(sb.toString());
            return fields.toArray(new String[0]);
        }

        public static void shuffle(List<Article> articles, long seed) {
            Collections.shuffle(articles, new Random(seed));
        }

        public static List<Article>[] split(List<Article> articles, double trainRatio) {
            int cutoff = (int)(articles.size() * trainRatio);
            @SuppressWarnings("unchecked")
            List<Article>[] result = new List[2];
            result[0] = new ArrayList<>(articles.subList(0, cutoff));
            result[1] = new ArrayList<>(articles.subList(cutoff, articles.size()));
            return result;
        }

        public static void printDistribution(List<Article> articles, String setName) {
            long real = 0, fake = 0;
            for (Article a : articles) { if (a.getLabel() == 1) real++; else fake++; }
            System.out.printf("[DataLoader] %-12s -> total: %5d  real: %5d  fake: %5d%n",
                    setName, articles.size(), real, fake);
        }
    }


    static final Set<String> stopWords = new HashSet<>(Arrays.asList(
            "the", "a", "an", "this", "that", "these", "those", "some", "any",
            "each", "every", "both", "either", "neither", "no", "all",
            "and", "or", "but", "nor", "so", "yet", "for",
            "in", "on", "at", "by", "to", "of", "from", "with", "about",
            "above", "after", "against", "along", "among", "around", "before",
            "behind", "below", "beneath", "beside", "between", "beyond", "during",
            "except", "inside", "into", "near", "off", "onto", "outside", "over",
            "past", "since", "through", "throughout", "under", "until", "up",
            "upon", "within", "without",
            "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
            "you", "your", "yours", "yourself", "he", "him", "his", "himself",
            "she", "her", "hers", "herself", "it", "its", "itself",
            "they", "them", "their", "theirs", "themselves",
            "what", "which", "who", "whom", "whose",
            "is", "am", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did",
            "will", "would", "shall", "should", "may", "might", "must",
            "can", "could", "need", "dare", "ought",
            "not", "also", "just", "very", "too", "more", "most", "other",
            "only", "same", "now", "then", "here", "there", "when", "where",
            "how", "why", "once", "again", "ever", "never", "always",
            "already", "still", "yet", "back", "even", "well", "far",
            "said", "says", "say", "make", "made", "know", "think", "take",
            "come", "came", "get", "got", "go", "went", "see", "use", "used"
    ));

    public static String clean(String text) {
        if (text == null || text.isEmpty()) return "";
        text = text.toLowerCase();
        text = text.replaceAll("[^a-z ]", " ");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    public static String[] tokenize(String text) {
        if (text.isEmpty()) return new String[0];
        return text.split("\\s+");
    }

    public static List<String> removeStopWords(String[] words) {
        List<String> filtered = new ArrayList<>();
        for (String word : words) {
            if (!stopWords.contains(word) && word.length() > 2)
                filtered.add(word);
        }
        return filtered;
    }

    public static List<String> getFeatures(String text, boolean useBigrams) {
        List<String> tokens = removeStopWords(tokenize(clean(text)));
        if (!useBigrams) return tokens;
        List<String> features = new ArrayList<>(tokens);
        for (int i = 0; i < tokens.size() - 1; i++)
            features.add(tokens.get(i) + "_" + tokens.get(i + 1));
        return features;
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

        public NaiveBayesClassifier() { this(1.0); }

        public void train(List<Article> articles) {
            for (Article article : articles) {
                List<String> words = getFeatures(article.getText(), false);
                int label = article.getLabel();

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

            System.out.println("[NaiveBayes] Training complete.");
            System.out.println("  Real docs  : " + realDocs);
            System.out.println("  Fake docs  : " + fakeDocs);
            System.out.println("  Vocabulary : " + vocabulary.size() + " tokens");
            System.out.println("  Alpha      : " + alpha);
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
            List<String> words    = getFeatures(text, false);
            int          vocabSize = vocabulary.size();

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

            System.out.println("\n[NaiveBayes] Top " + n + " indicators for REAL news:");
            llr.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(n)
                    .forEach(e -> System.out.printf("  %-20s LLR = %+.4f%n",
                            e.getKey(), e.getValue()));

            System.out.println("\n[NaiveBayes] Top " + n + " indicators for FAKE news:");
            llr.entrySet().stream()
                    .sorted((a, b) -> Double.compare(a.getValue(), b.getValue()))
                    .limit(n)
                    .forEach(e -> System.out.printf("  %-20s LLR = %+.4f%n",
                            e.getKey(), e.getValue()));
        }

        public int getVocabularySize() { return vocabulary.size(); }
    }


    static class TFIDFVectorizer {

        private final int maxFeatures;
        private Map<String, Integer> vocabulary = new LinkedHashMap<>();
        private Map<String, Double>  idfScores  = new HashMap<>();
        private int numDocuments;
        private boolean fitted = false;

        public TFIDFVectorizer(int maxFeatures) {
            this.maxFeatures = maxFeatures;
        }

        public void fit(List<Article> articles) {
            numDocuments = articles.size();

            Map<String, Integer> documentFrequency = new HashMap<>();
            Map<String, Integer> corpusFrequency   = new HashMap<>();

            for (Article article : articles) {
                List<String> tokens      = getFeatures(article.getText(), false);
                Set<String>  seenInDoc   = new HashSet<>(tokens);

                for (String token : tokens)    corpusFrequency.merge(token, 1, Integer::sum);
                for (String token : seenInDoc) documentFrequency.merge(token, 1, Integer::sum);
            }

            List<Map.Entry<String, Integer>> ranked = new ArrayList<>(corpusFrequency.entrySet());
            ranked.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            int vocabSize = Math.min(maxFeatures, ranked.size());
            for (int i = 0; i < vocabSize; i++)
                vocabulary.put(ranked.get(i).getKey(), i);

            for (String term : vocabulary.keySet()) {
                int df = documentFrequency.getOrDefault(term, 0);
                idfScores.put(term, Math.log((numDocuments + 1.0) / (df + 1.0)) + 1.0);
            }

            fitted = true;
            System.out.println("[TFIDFVectorizer] Fitted. Vocabulary size: " + vocabulary.size());
        }

        public double[] transform(String text) {
            if (!fitted) throw new IllegalStateException("Call fit() before transform().");

            double[] vector = new double[vocabulary.size()];
            List<String> tokens = getFeatures(text, false);
            if (tokens.isEmpty()) return vector;

            Map<String, Integer> termCounts = new HashMap<>();
            for (String token : tokens) termCounts.merge(token, 1, Integer::sum);

            int totalTokens = tokens.size();

            for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
                Integer idx = vocabulary.get(entry.getKey());
                if (idx == null) continue;
                double tf  = (double) entry.getValue() / totalTokens;
                double idf = idfScores.getOrDefault(entry.getKey(), 1.0);
                vector[idx] = tf * idf;
            }

            double norm = 0.0;
            for (double v : vector) norm += v * v;
            if (norm > 0) {
                norm = Math.sqrt(norm);
                for (int i = 0; i < vector.length; i++) vector[i] /= norm;
            }

            return vector;
        }

        public int                   getVocabularySize() { return vocabulary.size(); }
        public Map<String, Integer>  getVocabulary()     { return Collections.unmodifiableMap(vocabulary); }
    }


    static class LogisticRegressionClassifier {

        private final double learningRate;
        private final int    epochs;
        private final double lambda;
        private double[]        weights;
        private double          bias = 0.0;
        private TFIDFVectorizer vectorizer;
        private List<String>    featureNames;

        public LogisticRegressionClassifier(double learningRate, int epochs, double lambda) {
            this.learningRate = learningRate;
            this.epochs       = epochs;
            this.lambda       = lambda;
        }

        public void train(List<Article> articles, TFIDFVectorizer vectorizer) {
            this.vectorizer   = vectorizer;
            this.featureNames = new ArrayList<>(vectorizer.getVocabulary().keySet());
            int n             = featureNames.size();

            Random rng = new Random(42);
            weights    = new double[n];
            for (int i = 0; i < n; i++) weights[i] = (rng.nextDouble() - 0.5) * 0.01;
            bias = 0.0;

            System.out.println("[LogisticRegression] Pre-computing TF-IDF vectors...");
            double[][] featureMatrix = new double[articles.size()][];
            for (int i = 0; i < articles.size(); i++)
                featureMatrix[i] = vectorizer.transform(articles.get(i).getText());

            int[] labels  = new int[articles.size()];
            for (int i = 0; i < articles.size(); i++) labels[i] = articles.get(i).getLabel();

            int[] indices = new int[articles.size()];
            for (int i = 0; i < indices.length; i++) indices[i] = i;

            System.out.println("[LogisticRegression] Starting SGD training...");

            for (int epoch = 0; epoch < epochs; epoch++) {
                shuffleArray(indices, epoch);
                double totalLoss = 0.0;

                for (int idx : indices) {
                    double[] x = featureMatrix[idx];
                    int      y = labels[idx];

                    double z    = bias;
                    for (int j = 0; j < n; j++) z += weights[j] * x[j];
                    double yHat = sigmoid(z);

                    totalLoss += -(y * Math.log(yHat + 1e-10)
                            + (1 - y) * Math.log(1.0 - yHat + 1e-10));

                    double error = yHat - y;
                    bias -= learningRate * error;
                    for (int j = 0; j < n; j++)
                        weights[j] -= learningRate * (error * x[j] + lambda * weights[j]);
                }

                if ((epoch + 1) % 5 == 0 || epoch == 0)
                    System.out.printf("  Epoch %3d / %d  |  Avg Loss: %.5f%n",
                            epoch + 1, epochs, totalLoss / articles.size());
            }
            System.out.println("[LogisticRegression] Training complete.");
        }

        public int predict(String text) {
            return predictProbability(text) >= 0.5 ? 1 : 0;
        }

        public double predictProbability(String text) {
            if (weights == null) throw new IllegalStateException("Call train() first.");
            double[] x = vectorizer.transform(text);
            double z   = bias;
            for (int j = 0; j < weights.length; j++) z += weights[j] * x[j];
            return sigmoid(z);
        }

        public void printTopWeights(int n) {
            if (weights == null) { System.out.println("Not trained yet."); return; }

            Integer[] indices = new Integer[weights.length];
            for (int i = 0; i < indices.length; i++) indices[i] = i;
            Arrays.sort(indices, (a, b) ->
                    Double.compare(Math.abs(weights[b]), Math.abs(weights[a])));

            System.out.println("\n[LogisticRegression] Top " + n + " feature weights:");
            System.out.printf("  %-25s %s%n", "Feature", "Weight");
            System.out.println("  " + "-".repeat(40));

            for (int i = 0; i < Math.min(n, indices.length); i++) {
                int    idx = indices[i];
                double w   = weights[idx];
                System.out.printf("  %-25s %+.6f  %s%n",
                        featureNames.get(idx), w, w > 0 ? "(-> REAL)" : "(-> FAKE)");
            }
        }

        private double sigmoid(double z) {
            if (z >= 0) return 1.0 / (1.0 + Math.exp(-z));
            double e = Math.exp(z);
            return e / (1.0 + e);
        }

        private void shuffleArray(int[] arr, int seed) {
            Random rng = new Random(seed);
            for (int i = arr.length - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
            }
        }
    }


    static class Evaluator {

        static class Metrics {
            public final int    tp, tn, fp, fn;
            public final double accuracy, precision, recall, f1;

            public Metrics(int tp, int tn, int fp, int fn) {
                this.tp = tp; this.tn = tn; this.fp = fp; this.fn = fn;
                int total      = tp + tn + fp + fn;
                this.accuracy  = total           > 0 ? (double)(tp + tn) / total : 0.0;
                this.precision = (tp + fp)       > 0 ? (double) tp / (tp + fp)   : 0.0;
                this.recall    = (tp + fn)       > 0 ? (double) tp / (tp + fn)   : 0.0;
                this.f1        = (precision + recall) > 0
                        ? 2.0 * precision * recall / (precision + recall)
                        : 0.0;
            }

            public void print(String modelName) {
                System.out.println("\n" + "=".repeat(48));
                System.out.println("  Evaluation Report: " + modelName);
                System.out.println("=".repeat(48));
                System.out.printf("  Accuracy   : %.4f  (%.2f%%)%n", accuracy, accuracy * 100);
                System.out.printf("  Precision  : %.4f%n", precision);
                System.out.printf("  Recall     : %.4f%n", recall);
                System.out.printf("  F1 Score   : %.4f%n", f1);
                System.out.println();
                System.out.println("  Confusion Matrix:");
                System.out.println("            REAL    FAKE");
                System.out.printf("  REAL  |  %5d   %5d%n", tp, fn);
                System.out.printf("  FAKE  |  %5d   %5d%n", fp, tn);
                System.out.println("=".repeat(48));
            }
        }

        public static Metrics evaluate(NaiveBayesClassifier model, List<Article> testData) {
            int tp = 0, tn = 0, fp = 0, fn = 0;
            for (Article a : testData) {
                int pred = model.predict(a.getText()), actual = a.getLabel();
                if      (pred == 1 && actual == 1) tp++;
                else if (pred == 0 && actual == 0) tn++;
                else if (pred == 1 && actual == 0) fp++;
                else                               fn++;
            }
            return new Metrics(tp, tn, fp, fn);
        }

        public static Metrics evaluate(LogisticRegressionClassifier model, List<Article> testData) {
            int tp = 0, tn = 0, fp = 0, fn = 0;
            for (Article a : testData) {
                int pred = model.predict(a.getText()), actual = a.getLabel();
                if      (pred == 1 && actual == 1) tp++;
                else if (pred == 0 && actual == 0) tn++;
                else if (pred == 1 && actual == 0) fp++;
                else                               fn++;
            }
            return new Metrics(tp, tn, fp, fn);
        }

        public static void compare(String name1, Metrics m1, String name2, Metrics m2) {
            System.out.println("\n" + "=".repeat(64));
            System.out.println("  Model Comparison");
            System.out.println("=".repeat(64));
            System.out.printf("  %-24s %-12s %-12s %-12s %-10s%n",
                    "Model", "Accuracy", "Precision", "Recall", "F1");
            System.out.println("  " + "-".repeat(60));
            System.out.printf("  %-24s %-12.4f %-12.4f %-12.4f %-10.4f%n",
                    name1, m1.accuracy, m1.precision, m1.recall, m1.f1);
            System.out.printf("  %-24s %-12.4f %-12.4f %-12.4f %-10.4f%n",
                    name2, m2.accuracy, m2.precision, m2.recall, m2.f1);
            System.out.println("=".repeat(64));

            System.out.println("\n  Winners per metric:");
            printWinner("Accuracy",  name1, m1.accuracy,  name2, m2.accuracy);
            printWinner("Precision", name1, m1.precision, name2, m2.precision);
            printWinner("Recall",    name1, m1.recall,    name2, m2.recall);
            printWinner("F1 Score",  name1, m1.f1,        name2, m2.f1);
        }

        private static void printWinner(String metric,
                                        String n1, double v1,
                                        String n2, double v2) {
            String winner = v1 > v2 ? n1 : (v2 > v1 ? n2 : "Tie");
            System.out.printf("    %-12s -> %s (%.4f vs %.4f)%n", metric, winner, v1, v2);
        }
    }


    static class DatasetMerger {

        public static void merge(String fakeFile, String trueFile, String outputFile) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                writer.write("text,label");
                writer.newLine();

                processFile(fakeFile, 0, writer);
                processFile(trueFile, 1, writer);

                writer.close();
                System.out.println("[DatasetMerger] Done! " + outputFile + " is ready.");

            } catch (IOException e) {
                System.err.println("[DatasetMerger] Error: " + e.getMessage());
            }
        }

        private static void processFile(String filePath, int label, BufferedWriter writer)
                throws IOException {

            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String headerLine = reader.readLine();

            if (headerLine == null) {
                System.err.println("[DatasetMerger] File is empty: " + filePath);
                reader.close();
                return;
            }

            String[] headers = DataLoader.parseCSVLine(headerLine);
            int textIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().toLowerCase().equals("text")) {
                    textIndex = i;
                    break;
                }
            }

            if (textIndex == -1) {
                System.err.println("[DatasetMerger] No 'text' column found in: " + filePath);
                reader.close();
                return;
            }

            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                String[] fields = DataLoader.parseCSVLine(line);
                if (fields.length <= textIndex) continue;

                String text = fields[textIndex].trim();
                if (text.isEmpty()) continue;

                text = text.replace("\"", "'");
                writer.write("\"" + text + "\"," + label);
                writer.newLine();
                count++;
            }

            reader.close();
            System.out.println("[DatasetMerger] Processed " + count + " articles from "
                    + filePath + " (label=" + label + ")");
        }
    }


    public static void main(String[] args) {
        System.out.println("=".repeat(55));
        System.out.println("     Fake News Detection System");
        System.out.println("=".repeat(55));

        if (!new java.io.File("data.csv").exists()) {
            System.out.println("[DatasetMerger] data.csv not found. Attempting to merge Fake.csv and True.csv...");
            DatasetMerger.merge("Fake.csv", "True.csv", "data.csv");
        }

        List<Article> articles = DataLoader.loadCSV("data.csv", "text", "label");
        if (articles.isEmpty()) {
            System.err.println("No articles loaded. Make sure Fake.csv and True.csv are in the same folder.");
            return;
        }

        DataLoader.shuffle(articles, 42);
        List<Article>[] sets   = DataLoader.split(articles, 0.8);
        List<Article> trainSet = sets[0];
        List<Article> testSet  = sets[1];

        DataLoader.printDistribution(articles, "Full dataset");
        DataLoader.printDistribution(trainSet, "Training set");
        DataLoader.printDistribution(testSet,  "Test set    ");

        System.out.println("\n" + "-".repeat(55));
        System.out.println("  NAIVE BAYES");
        System.out.println("-".repeat(55));

        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.train(trainSet);
        nb.printTopWords(10);
        Evaluator.Metrics nbMetrics = Evaluator.evaluate(nb, testSet);
        nbMetrics.print("Naive Bayes");

        System.out.println("\n" + "-".repeat(55));
        System.out.println("  LOGISTIC REGRESSION");
        System.out.println("-".repeat(55));

        TFIDFVectorizer vectorizer = new TFIDFVectorizer(3000);
        vectorizer.fit(trainSet);

        LogisticRegressionClassifier lr =
                new LogisticRegressionClassifier(0.05, 20, 0.001);
        lr.train(trainSet, vectorizer);
        lr.printTopWeights(10);
        Evaluator.Metrics lrMetrics = Evaluator.evaluate(lr, testSet);
        lrMetrics.print("Logistic Regression");

        Evaluator.compare("Naive Bayes", nbMetrics, "Logistic Regression", lrMetrics);

        System.out.println("\n" + "-".repeat(55));
        System.out.println("  SAMPLE PREDICTIONS");
        System.out.println("-".repeat(55));

        String[] samples = {
                "Government announces new healthcare legislation after months of debate",
                "Secret alien base found beneath the Arctic, whistleblower reveals",
                "Stock markets rally after positive inflation data released by central bank",
                "NASA confirms moon landing was filmed in a Hollywood studio"
        };

        System.out.printf("%-52s %-8s %-8s %-8s %-8s%n",
                "Text", "NB", "LR", "NB Conf", "LR Prob");
        System.out.println("-".repeat(88));

        for (String sample : samples) {
            String t = sample.length() > 50 ? sample.substring(0, 49) + "..." : sample;
            System.out.printf("%-52s %-8s %-8s %-8.3f %-8.3f%n",
                    t,
                    nb.predict(sample) == 1 ? "REAL" : "FAKE",
                    lr.predict(sample) == 1 ? "REAL" : "FAKE",
                    nb.confidence(sample),
                    lr.predictProbability(sample));
        }
    }
}