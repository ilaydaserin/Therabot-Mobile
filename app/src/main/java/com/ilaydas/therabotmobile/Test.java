package com.ilaydas.therabotmobile;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent; // Import Intent
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.core.content.res.ResourcesCompat;

public class Test extends AppCompatActivity {
    private RadioGroup optionsGroup;
    private Button nextButton;
    private TextView questionText, questionCounter, progressPercent;
    private ProgressBar progressBar;

    private int currentQuestionIndex = 0;
    private List<Question> questionsList;
    private List<String> currentTestOptions;
    private String currentTestName;
    private int currentTestMaxScorePerItem;

    private NpiResults npiTestResults;
    private MmpiAnxietyResults mmpiAnxietyTestResults;
    private HsqResults hsqTestResults;
    private EqSqResults eqsqTestResults;

    private List<Integer> mmpiAnxietyScoresMapping;
    private List<Integer> hsqScoresMapping;
    private List<Integer> eqsqScoresMapping;

    private Map<String, Integer> traitScores = new HashMap<>();
    private Map<String, Integer> traitCounts = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        optionsGroup = findViewById(R.id.options_group);
        nextButton = findViewById(R.id.next_button);
        questionText = findViewById(R.id.question_text);
        questionCounter = findViewById(R.id.question_counter);
        progressBar = findViewById(R.id.progress_bar);
        progressPercent = findViewById(R.id.progress_percent);

        questionsList = loadQuestions();

        if (questionsList != null && !questionsList.isEmpty()) {
            progressBar.setMax(questionsList.size());
            loadQuestion(currentQuestionIndex);
        } else {
            Log.e("TestActivity", "No questions loaded!");
            Toast.makeText(this, "Failed to load questions. Please check your application.", Toast.LENGTH_LONG).show();
            finish();
        }

        nextButton.setOnClickListener(v -> {
            // If the test is complete, the button's action changes
            if (currentQuestionIndex >= questionsList.size()) {
                // This means the button is already "Back to Tests" and is clicked
                // Navigate back to TestsActivity
                Intent intent = new Intent(Test.this, Tests.class); // Assuming TestsActivity is your activity name
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear back stack
                startActivity(intent);
                finish(); // Close current activity
                return; // Stop further execution
            }

            int selectedId = optionsGroup.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(this, "Please select an option!", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadioButton = findViewById(selectedId);
            int selectedOptionIndex = optionsGroup.indexOfChild(selectedRadioButton);

            Question current = questionsList.get(currentQuestionIndex);
            int score = 0;

            if ("Big Five Personality Test".equals(currentTestName)) {
                int rawScore = selectedOptionIndex + 1;
                score = current.isReverseScored ? (currentTestMaxScorePerItem + 1 - rawScore) : rawScore;
            } else if ("Fisher Temperament Inventory".equals(currentTestName)) {
                score = selectedOptionIndex + 1;
            } else if ("MMPI-2 Anxiety Scale (Sample)".equals(currentTestName)) {
                if (mmpiAnxietyScoresMapping != null && selectedOptionIndex < mmpiAnxietyScoresMapping.size()) {
                    int rawScore = mmpiAnxietyScoresMapping.get(selectedOptionIndex);
                    score = current.isReverseScored ? ((rawScore == 1) ? 0 : 1) : rawScore;
                } else {
                    Log.e("TestActivity", "MMPI Anxiety Scale scoring mapping is missing or invalid.");
                    score = 0;
                }
            } else if ("Narcissistic Personality Test".equals(currentTestName)) {
                score = (selectedOptionIndex == 0) ? 1 : 0;
            } else if ("Humor Styles Questionnaire (HSQ)".equals(currentTestName)) {
                if (hsqScoresMapping != null && selectedOptionIndex < hsqScoresMapping.size()) {
                    int rawScore = hsqScoresMapping.get(selectedOptionIndex);
                    score = current.isReverseScored ? (currentTestMaxScorePerItem + 1 - rawScore) : rawScore;
                } else {
                    Log.e("TestActivity", "HSQ scoring mapping is missing or invalid.");
                    score = 0;
                }
            } else if ("Empathizing and Systemizing Quotients (EQ-SQ)".equals(currentTestName)) {
                if (eqsqScoresMapping != null && selectedOptionIndex < eqsqScoresMapping.size()) {
                    int rawScore = eqsqScoresMapping.get(selectedOptionIndex);
                    score = current.isReverseScored ? (currentTestMaxScorePerItem + 1 - rawScore) : rawScore;
                } else {
                    Log.e("TestActivity", "EQ-SQ scoring mapping is missing or invalid.");
                    score = 0;
                }
            }
            else {
                score = selectedOptionIndex + 1;
                Log.w("TestActivity", "Unknown test type for scoring or no specific scoring logic: " + currentTestName + ". Using default.");
            }

            String trait = current.getTrait();
            if (trait != null && !trait.isEmpty()) {
                traitScores.put(trait, traitScores.getOrDefault(trait, 0) + score);
                traitCounts.put(trait, traitCounts.getOrDefault(trait, 0) + 1);
            } else {
                String defaultTrait = "Unknown Trait";
                if ("Narcissistic Personality Test".equals(currentTestName)) {
                    defaultTrait = "Narcissism";
                } else if ("MMPI-2 Anxiety Scale (Sample)".equals(currentTestName)) {
                    defaultTrait = "Anxiety";
                } else if ("Humor Styles Questionnaire (HSQ)".equals(currentTestName)) {
                    Log.e("TestActivity", "HSQ question " + current.getText() + " is missing a trait!");
                    defaultTrait = "Unknown Humor Style";
                } else if ("Empathizing and Systemizing Quotients (EQ-SQ)".equals(currentTestName)) {
                    Log.e("TestActivity", "EQ-SQ question " + current.getText() + " is missing a trait!");
                    defaultTrait = "Unknown EQ/SQ Trait";
                }
                traitScores.put(defaultTrait, traitScores.getOrDefault(defaultTrait, 0) + score);
                traitCounts.put(defaultTrait, traitCounts.getOrDefault(defaultTrait, 0) + 1);
                Log.w("TestActivity", "Question " + current.getText() + " has no trait defined. Assigned to: " + defaultTrait + ". Score: " + score);
            }

            currentQuestionIndex++;
            if (currentQuestionIndex < questionsList.size()) {
                loadQuestion(currentQuestionIndex);
            } else {
                showResult();
            }
        });
    }

    private List<Question> loadQuestions() {
        Reader reader = null;
        try {
            String jsonFile = getIntent().getStringExtra("jsonFile");
            if (jsonFile == null) {
                Log.e("TestActivity", "jsonFile not found in intent extras!");
                return new ArrayList<>();
            }

            InputStream inputStream = getAssets().open(jsonFile);
            reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            Gson gson = new Gson();

            List<Question> unifiedQuestions = new ArrayList<>();

            if (jsonFile.equals("big-five.json")) {
                IpipBffmTestData testData = gson.fromJson(reader, IpipBffmTestData.class);
                currentTestName = testData.test_name;
                currentTestMaxScorePerItem = 5;
                currentTestOptions = new ArrayList<>(testData.scale.values());
                for (IpipBffmQuestion q : testData.questions) {
                    unifiedQuestions.add(new Question(q.text, q.trait, q.reverse_scored, null));
                }
                Log.d("TestActivity", "Loaded Big Five questions. Total: " + unifiedQuestions.size());
                return unifiedQuestions;

            } else if (jsonFile.equals("fisher_temperament_inventory.json")) {
                FisherTestData testData = gson.fromJson(reader, FisherTestData.class);
                currentTestName = testData.test_name;
                currentTestMaxScorePerItem = 4;
                currentTestOptions = new ArrayList<>(testData.scale.values());
                for (FisherQuestion q : testData.questions) {
                    unifiedQuestions.add(new Question(q.text, q.trait, false, null));
                }
                Log.d("TestActivity", "Loaded Fisher questions. Total: " + unifiedQuestions.size());
                return unifiedQuestions;

            } else if (jsonFile.equals("MMPI_2_Anxiety Scale.json")) {
                MmpiAnxietyTestData mmpiTestData = gson.fromJson(reader, MmpiAnxietyTestData.class);
                currentTestName = mmpiTestData.test;
                currentTestMaxScorePerItem = 1;
                currentTestOptions = mmpiTestData.options;
                mmpiAnxietyScoresMapping = mmpiTestData.scores;
                mmpiAnxietyTestResults = mmpiTestData.results;

                if (mmpiTestData.questions != null) {
                    for (MmpiAnxietyQuestion q : mmpiTestData.questions) {
                        unifiedQuestions.add(new Question(q.text, q.trait, q.reverse, null));
                    }
                }
                Log.d("TestActivity", "Loaded MMPI-2 Anxiety questions. Total: " + unifiedQuestions.size());
                return unifiedQuestions;

            } else if (jsonFile.equals("narcissism_test.json")) {
                NpiTestData npiTestData = gson.fromJson(reader, NpiTestData.class);
                currentTestName = npiTestData.test_name;
                currentTestMaxScorePerItem = 1;
                npiTestResults = npiTestData.results;

                if (npiTestData.questions != null) {
                    for (NpiQuestion q : npiTestData.questions) {
                        unifiedQuestions.add(new Question(q.question, "Narcissism", false, q.options));
                    }
                }
                Log.d("TestActivity", "Loaded NPI questions. Total: " + unifiedQuestions.size());
                return unifiedQuestions;

            } else if (jsonFile.equals("humor_styles_questionnaire.json")) {
                HsqTestData hsqTestData = gson.fromJson(reader, HsqTestData.class);
                currentTestName = hsqTestData.test;
                currentTestMaxScorePerItem = 5;
                currentTestOptions = hsqTestData.options;
                hsqScoresMapping = hsqTestData.scores;
                hsqTestResults = hsqTestData.results;

                if (hsqTestData.questions != null) {
                    for (HsqQuestion q : hsqTestData.questions) {
                        unifiedQuestions.add(new Question(q.text, q.trait, q.reverse, null));
                    }
                }
                Log.d("TestActivity", "Loaded HSQ questions. Total: " + unifiedQuestions.size());
                return unifiedQuestions;

            } else if (jsonFile.equals("eqsq.json")) {
                EqSqTestData eqsqTestData = gson.fromJson(reader, EqSqTestData.class);
                currentTestName = eqsqTestData.test;
                currentTestMaxScorePerItem = 4;
                currentTestOptions = eqsqTestData.options;
                eqsqScoresMapping = eqsqTestData.scores;
                eqsqTestResults = eqsqTestData.results;

                if (eqsqTestData.questions != null) {
                    for (EqSqQuestion q : eqsqTestData.questions) {
                        boolean isReverse = (q.reverse != null) ? q.reverse : false;
                        unifiedQuestions.add(new Question(q.text, q.trait, isReverse, null));
                    }
                }
                Log.d("TestActivity", "Loaded EQ-SQ questions. Total: " + unifiedQuestions.size());
                return unifiedQuestions;

            } else {
                Log.e("TestActivity", "Unknown JSON file type: " + jsonFile + ". Attempting generic parse.");
                return loadGenericQuestions(reader, gson, jsonFile);
            }

        } catch (Exception e) {
            Log.e("TestActivity", "Error opening asset file or general parsing error: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading test file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new ArrayList<>();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                Log.e("TestActivity", "Error closing reader: " + e.getMessage(), e);
            }
        }
    }

    private List<Question> loadGenericQuestions(Reader reader, Gson gson, String jsonFile) {
        try {
            GenericTestData genericTestData = gson.fromJson(reader, GenericTestData.class);
            currentTestName = genericTestData.test_name != null ? genericTestData.test_name : jsonFile.replace(".json", "");
            currentTestMaxScorePerItem = 5;
            currentTestOptions = genericTestData.options != null ? genericTestData.options : new ArrayList<>();
            if (currentTestOptions.isEmpty() && genericTestData.scale != null) {
                currentTestOptions = new ArrayList<>(genericTestData.scale.values());
                Collections.sort(currentTestOptions);
            }
            if (currentTestOptions.isEmpty()) {
                currentTestOptions.add("1"); currentTestOptions.add("2"); currentTestOptions.add("3"); currentTestOptions.add("4"); currentTestOptions.add("5");
                Log.w("TestActivity", "No options found for " + jsonFile + ", using default 1-5.");
            }

            List<Question> unifiedQuestions = new ArrayList<>();
            if (genericTestData.questions != null) {
                for (GenericQuestion q : genericTestData.questions) {
                    unifiedQuestions.add(new Question(
                            q.text != null ? q.text : q.question,
                            q.trait,
                            q.isReverseSafely(),
                            null
                    ));
                }
            }
            if (unifiedQuestions.isEmpty()) {
                Log.e("TestActivity", "No questions found in generic test data for: " + jsonFile);
                Toast.makeText(this, "No questions found: " + jsonFile, Toast.LENGTH_LONG).show();
            }
            Log.d("TestActivity", "Loaded generic questions. Total: " + unifiedQuestions.size());
            return unifiedQuestions;
        } catch (Exception e) {
            Log.e("TestActivity", "Error loading generic questions from " + jsonFile + ": " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void loadQuestion(int index) {
        Question currentQuestion = questionsList.get(index);
        questionText.setText(currentQuestion.getText());
        questionCounter.setText("Question " + (index + 1) + " of " + questionsList.size());
        optionsGroup.removeAllViews();

        Typeface comfortaFont = ResourcesCompat.getFont(this, R.font.comforta);

        List<String> optionsToDisplay = new ArrayList<>();

        if ("Narcissistic Personality Test".equals(currentTestName)) {
            if (currentQuestion.getOptions() != null && !currentQuestion.getOptions().isEmpty()) {
                optionsToDisplay.addAll(currentQuestion.getOptions());
            } else {
                Log.e("TestActivity", "NPI question has no options defined: " + currentQuestion.getText());
                optionsToDisplay.add("True");
                optionsToDisplay.add("False");
            }
        } else {
            if (currentTestOptions != null && !currentTestOptions.isEmpty()) {
                optionsToDisplay.addAll(currentTestOptions);
            } else {
                Log.e("TestActivity", "No options defined for test: " + currentTestName + ". Using default 1-5.");
                optionsToDisplay.add("1"); optionsToDisplay.add("2"); optionsToDisplay.add("3"); optionsToDisplay.add("4"); optionsToDisplay.add("5");
            }
        }

        for (int i = 0; i < optionsToDisplay.size(); i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(optionsToDisplay.get(i));
            rb.setTextColor(getResources().getColor(R.color.black));
            rb.setTextSize(17);
            rb.setTypeface(comfortaFont);

            int paddingStart = (int) getResources().getDimension(R.dimen.rb_padding_horizontal);
            int paddingEnd = (int) getResources().getDimension(R.dimen.rb_padding_horizontal);
            int paddingVertical = (int) getResources().getDimension(R.dimen.rb_padding_vertical);

            rb.setPaddingRelative(paddingStart, paddingVertical, paddingEnd, paddingVertical);
            rb.setBackgroundResource(R.drawable.selectable_option_background);
            rb.setButtonDrawable(null);
            rb.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_button_selector, 0, 0, 0);
            rb.setCompoundDrawablePadding((int) getResources().getDimension(R.dimen.rb_drawable_padding));
            rb.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            rb.setElevation(3f);

            RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.bottomMargin = (int) getResources().getDimension(R.dimen.rb_margin_bottom);
            rb.setLayoutParams(layoutParams);

            rb.setId(View.generateViewId());

            optionsGroup.addView(rb);
        }
        optionsGroup.clearCheck();

        progressBar.setProgress(index + 1);
        progressPercent.setText(((index + 1) * 100 / questionsList.size()) + "%");
    }

    private void showResult() {
        StringBuilder result = new StringBuilder("Test Completed!\n\n");

        if (currentTestName != null && currentTestMaxScorePerItem > 0) {
            result.append(currentTestName).append(" Results:\n\n");

            if (traitScores.isEmpty()) {
                result.append("Results could not be calculated. Please check test data.\n");
            } else {
                for (Map.Entry<String, Integer> entry : traitScores.entrySet()) {
                    String trait = entry.getKey();
                    int totalScore = entry.getValue();
                    int count = traitCounts.getOrDefault(trait, 0);

                    if (count > 0) {
                        if ("Narcissistic Personality Test".equals(currentTestName)) {
                            result.append(String.format("%s Score: %d / %d\n", trait, totalScore, questionsList.size()));

                            if (npiTestResults != null && npiTestResults.score_description != null) {
                                result.append("\n");
                                for (NpiResults.ScoreRangeDescription desc : npiTestResults.score_description) {
                                    String[] rangeParts = desc.range.split("-");
                                    try {
                                        int min = Integer.parseInt(rangeParts[0]);
                                        int max = Integer.parseInt(rangeParts[1]);

                                        if (totalScore >= min && totalScore <= max) {
                                            result.append(desc.text).append("\n");
                                            break;
                                        }
                                    } catch (NumberFormatException e) {
                                        Log.e("TestActivity", "Invalid range format for NPI: " + desc.range, e);
                                    }
                                }
                            }
                            if (npiTestResults != null && npiTestResults.disclaimer != null) {
                                result.append("\n").append(npiTestResults.disclaimer).append("\n");
                            }
                        } else if ("MMPI-2 Anxiety Scale (Sample)".equals(currentTestName)) {
                            result.append(String.format("%s Score: %d / %d\n", trait, totalScore, questionsList.size()));

                            if (mmpiAnxietyTestResults != null && mmpiAnxietyTestResults.score_description != null) {
                                result.append("\n");
                                for (MmpiAnxietyResults.ScoreRangeDescription desc : mmpiAnxietyTestResults.score_description) {
                                    String[] rangeParts = desc.range.split("-");
                                    try {
                                        int min = Integer.parseInt(rangeParts[0]);
                                        int max = Integer.parseInt(rangeParts[1]);

                                        if (totalScore >= min && totalScore <= max) {
                                            result.append(desc.text).append("\n");
                                            break;
                                        }
                                    } catch (NumberFormatException e) {
                                        Log.e("TestActivity", "Invalid range format for MMPI Anxiety: " + desc.range, e);
                                    }
                                }
                            }
                            if (mmpiAnxietyTestResults != null && mmpiAnxietyTestResults.disclaimer != null) {
                                result.append("\n").append(mmpiAnxietyTestResults.disclaimer).append("\n");
                            }
                        } else if ("Humor Styles Questionnaire (HSQ)".equals(currentTestName)) {
                            double averageScore = (double) totalScore / count;
                            result.append(String.format("%s Score: %.2f / %d (average)\n", trait, averageScore, currentTestMaxScorePerItem));
                        } else if ("Empathizing and Systemizing Quotients (EQ-SQ)".equals(currentTestName)) {
                            result.append(String.format("%s Score: %d / %d\n", trait, totalScore, count * currentTestMaxScorePerItem));

                            if (eqsqTestResults != null && eqsqTestResults.score_description != null) {
                                EqSqResults.TraitScoreDescription traitDesc = null;
                                for (EqSqResults.TraitScoreDescription tsd : eqsqTestResults.score_description) {
                                    if (tsd.trait != null && tsd.trait.equalsIgnoreCase(trait)) {
                                        traitDesc = tsd;
                                        break;
                                    }
                                }

                                if (traitDesc != null) {
                                    if (traitDesc.general_note != null) {
                                        result.append("\n").append(traitDesc.general_note).append("\n");
                                    }
                                    if (traitDesc.ranges != null) {
                                        for (EqSqResults.ScoreRangeDescription rangeDesc : traitDesc.ranges) {
                                            String[] rangeParts = rangeDesc.range.split("-");
                                            try {
                                                int min = Integer.parseInt(rangeParts[0]);
                                                int max = Integer.parseInt(rangeParts[1]);

                                                if (totalScore >= min && totalScore <= max) {
                                                    result.append(rangeDesc.text).append("\n");
                                                    break;
                                                }
                                            } catch (NumberFormatException e) {
                                                Log.e("TestActivity", "Invalid range format for EQ-SQ: " + rangeDesc.range, e);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            double averageScore = (double) totalScore / count;
                            result.append(String.format("%s: %.2f / %d (average)\n", trait, averageScore, currentTestMaxScorePerItem));
                        }
                    } else {
                        result.append(String.format("%s: N/A (No questions scored)\n", trait));
                    }
                }
            }

            if ("Humor Styles Questionnaire (HSQ)".equals(currentTestName) && hsqTestResults != null) {
                if (hsqTestResults.score_description != null) {
                    result.append("\n--- Humor Style Interpretations ---\n");
                    for (HsqResults.ScoreTraitDescription desc : hsqTestResults.score_description) {
                        result.append(String.format("\n%s: %s\n", desc.trait, desc.description));
                    }
                }
                if (hsqTestResults.general_note != null) {
                    result.append("\n").append(hsqTestResults.general_note).append("\n");
                }
                if (hsqTestResults.disclaimer != null) {
                    result.append("\n").append(hsqTestResults.disclaimer).append("\n");
                }
            }
            else if ("Empathizing and Systemizing Quotients (EQ-SQ)".equals(currentTestName) && eqsqTestResults != null) {
                if (eqsqTestResults.disclaimer != null) {
                    result.append("\n").append(eqsqTestResults.disclaimer).append("\n");
                }
            }
            else if (!("Narcissistic Personality Test".equals(currentTestName) || "MMPI-2 Anxiety Scale (Sample)".equals(currentTestName))) {
                result.append("\nConsult a specialist for interpretation of results. This is not a diagnostic tool.");
            }

        } else {
            result.append("Results could not be calculated or unknown test type.");
        }

        Log.d("TestActivity", result.toString());
        questionText.setText(result.toString());
        optionsGroup.removeAllViews();
        progressBar.setProgress(questionsList.size());
        progressPercent.setText("100%");
        Toast.makeText(this, "Test completed! Check results.", Toast.LENGTH_LONG).show();

        nextButton.setText("Back to Tests");
        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(Test.this,Tests.class); // Assuming TestsActivity is your activity name
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear the back stack
            startActivity(intent);
            finish();
        });
        nextButton.setEnabled(true);
    }


    public static class Question {
        private String questionText;
        private String trait;
        private boolean isReverseScored;
        private List<String> options;

        public Question(String questionText, String trait, boolean isReverseScored, List<String> options) {
            this.questionText = questionText;
            this.trait = trait;
            this.isReverseScored = isReverseScored;
            this.options = options;
        }

        public String getText() {
            return questionText;
        }

        public String getTrait() {
            return trait;
        }

        public boolean isReverseScored() {
            return isReverseScored;
        }

        public List<String> getOptions() {
            return options;
        }
    }

    // --- Data Classes for IPIP-BFFM JSON Structure (mapped to big-five.json) ---
    public static class IpipBffmTestData {
        @SerializedName("test_name")
        public String test_name;
        @SerializedName("scale")
        public Map<String, String> scale;
        @SerializedName("questions")
        public List<IpipBffmQuestion> questions;
    }

    public static class IpipBffmQuestion {
        @SerializedName("text")
        public String text;
        @SerializedName("trait")
        public String trait;
        @SerializedName("reverse_scored")
        public boolean reverse_scored;
    }

    // --- Data Classes for Fisher Temperament Inventory JSON Structure (mapped to fisher_temperament_inventory.json) ---
    public static class FisherTestData {
        @SerializedName("test_name")
        public String test_name;
        @SerializedName("scale")
        public Map<String, String> scale;
        @SerializedName("questions")
        public List<FisherQuestion> questions;
    }

    public static class FisherQuestion {
        @SerializedName("text")
        public String text;
        @SerializedName("trait")
        public String trait;
    }

    // --- Data Classes for the MMPI-2 Anxiety Scale JSON Structure ---
    public static class MmpiAnxietyTestData {
        @SerializedName("test")
        public String test;
        @SerializedName("version")
        public String version;
        @SerializedName("scale")
        public String scale;
        @SerializedName("questions")
        public List<MmpiAnxietyQuestion> questions;
        @SerializedName("options")
        public List<String> options;
        @SerializedName("scores")
        public List<Integer> scores;
        @SerializedName("results")
        public MmpiAnxietyResults results;
    }

    public static class MmpiAnxietyQuestion {
        @SerializedName("id")
        public int id;
        @SerializedName("text")
        public String text;
        @SerializedName("trait")
        public String trait;
        @SerializedName("reverse")
        public boolean reverse;
    }

    public static class MmpiAnxietyResults {
        @SerializedName("title")
        public String title;
        @SerializedName("score_description")
        public List<ScoreRangeDescription> score_description;
        @SerializedName("disclaimer")
        public String disclaimer;

        public static class ScoreRangeDescription {
            @SerializedName("range")
            public String range;
            @SerializedName("text")
            public String text;
        }
    }

    // --- Data Classes for Narcissistic Personality Test (NPI) JSON Structure ---
    public static class NpiTestData {
        @SerializedName("test_name")
        public String test_name;
        @SerializedName("description")
        public String description;
        @SerializedName("questions")
        public List<NpiQuestion> questions;
        @SerializedName("results")
        public NpiResults results;
    }

    public static class NpiQuestion {
        @SerializedName("id")
        public int id;
        @SerializedName("question")
        public String question;
        @SerializedName("options")
        public List<String> options;
    }

    public static class NpiResults {
        @SerializedName("title")
        public String title;
        @SerializedName("score_description")
        public List<ScoreRangeDescription> score_description;
        @SerializedName("disclaimer")
        public String disclaimer;

        public static class ScoreRangeDescription {
            @SerializedName("range")
            public String range;
            @SerializedName("text")
            public String text;
        }
    }

    // --- Data Classes for Humor Styles Questionnaire (HSQ) JSON Structure ---
    public static class HsqTestData {
        @SerializedName("test")
        public String test;
        @SerializedName("version")
        public String version;
        @SerializedName("description")
        public String description;
        @SerializedName("scale")
        public String scale;
        @SerializedName("questions")
        public List<HsqQuestion> questions;
        @SerializedName("options")
        public List<String> options;
        @SerializedName("scores")
        public List<Integer> scores;
        @SerializedName("results")
        public HsqResults results;
    }

    public static class HsqQuestion {
        @SerializedName("id")
        public int id;
        @SerializedName("text")
        public String text;
        @SerializedName("trait")
        public String trait;
        @SerializedName("reverse")
        public boolean reverse;
    }

    public static class HsqResults {
        @SerializedName("title")
        public String title;
        @SerializedName("general_note")
        public String general_note;
        @SerializedName("score_description")
        public List<ScoreTraitDescription> score_description;
        @SerializedName("disclaimer")
        public String disclaimer;

        public static class ScoreTraitDescription {
            @SerializedName("trait")
            public String trait;
            @SerializedName("description")
            public String description;
        }
    }

    // --- Data Classes for Empathizing and Systemizing Quotients (EQ-SQ) JSON Structure ---
    public static class EqSqTestData {
        @SerializedName("test")
        public String test;
        @SerializedName("version")
        public String version;
        @SerializedName("description")
        public String description;
        @SerializedName("scale")
        public String scale;
        @SerializedName("questions")
        public List<EqSqQuestion> questions;
        @SerializedName("options")
        public List<String> options;
        @SerializedName("scores")
        public List<Integer> scores;
        @SerializedName("results")
        public EqSqResults results;
    }

    public static class EqSqQuestion {
        @SerializedName("id")
        public String id;
        @SerializedName("text")
        public String text;
        @SerializedName("trait")
        public String trait;
        @SerializedName("reverse")
        public Boolean reverse;
    }

    public static class EqSqResults {
        @SerializedName("title")
        public String title;
        @SerializedName("score_description")
        public List<TraitScoreDescription> score_description;
        @SerializedName("disclaimer")
        public String disclaimer;

        public static class TraitScoreDescription {
            @SerializedName("trait")
            public String trait;
            @SerializedName("general_note")
            public String general_note;
            @SerializedName("ranges")
            public List<ScoreRangeDescription> ranges;
        }

        public static class ScoreRangeDescription {
            @SerializedName("range")
            public String range;
            @SerializedName("text")
            public String text;
        }
    }

    // --- Data Classes for Generic Test JSON Structure ---
    public static class GenericTestData {
        @SerializedName("test_name")
        public String test_name;
        @SerializedName("questions")
        public List<GenericQuestion> questions;
        @SerializedName("options")
        public List<String> options;
        @SerializedName("scale")
        public Map<String, String> scale;
    }

    public static class GenericQuestion {
        @SerializedName("text")
        public String text;
        @SerializedName("question")
        public String question;
        @SerializedName("trait")
        public String trait;
        @SerializedName("reverse")
        public Boolean reverse;
        @SerializedName("reverse_scored")
        public Boolean reverse_scored;

        public boolean isReverseSafely() {
            if (reverse != null) {
                return reverse;
            }
            if (reverse_scored != null) {
                return reverse_scored;
            }
            return false;
        }
    }
}