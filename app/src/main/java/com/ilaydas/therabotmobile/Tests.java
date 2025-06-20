package com.ilaydas.therabotmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Tests extends AppCompatActivity {

    private RecyclerView testRecyclerView;
    private List<TestItem> testList;
    private TestListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tests);

        testRecyclerView = findViewById(R.id.test_list_recycler_view);
        testRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        testList = new ArrayList<>();
        testList.add(new TestItem("Big Five Personality Test", "Measures five broad personality traits.", "big-five.json"));
        testList.add(new TestItem("Fisher Temperament Inventory", "Assesses personality traits related to brain systems.", "fisher_temperament_inventory.json"));
        testList.add(new TestItem("Narcissism Test (NPI)", "Evaluates narcissistic personality traits.", "narcissism_test.json"));
        testList.add(new TestItem("Anxiety Scale(MMPI-2)", "Measures symptoms and experiences associated with anxiety.", "MMPI_2_Anxiety Scale.json"));
        testList.add(new TestItem("Humor Styles Questionnaire Test (HSQ)", "Assesses individual differences in humor styles.", "humor_styles_questionnaire.json"));
        testList.add(new TestItem("Empathizing & Systemizing Test (EQ-SQ)", "Measures empathy and systemizing drive.", "eqsq.json"));
        testList.add(new TestItem("General Knowledge Test", "Evaluates general knowledge across various domains.", "general_knowledge_test.json"));


        adapter = new TestListAdapter(testList, new TestListAdapter.OnTestClickListener() {
            @Override
            public void onTestClick(TestItem testItem) {
                Intent intent = new Intent(Tests.this, Test.class);
                intent.putExtra("jsonFile", testItem.getJsonFileName());
                startActivity(intent);
            }
        });
        testRecyclerView.setAdapter(adapter);
    }

    public static class TestItem {
        private final String title;
        private final String description;
        private final String jsonFileName;

        public TestItem(String title, String description, String jsonFileName) {
            this.title = title;
            this.description = description;
            this.jsonFileName = jsonFileName;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getJsonFileName() {
            return jsonFileName;
        }
    }

    public static class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.TestViewHolder> {

        private final List<TestItem> testList;
        private final OnTestClickListener listener;

        public interface OnTestClickListener {
            void onTestClick(TestItem testItem);
        }

        public TestListAdapter(List<TestItem> testList, OnTestClickListener listener) {
            this.testList = testList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test, parent, false);
            return new TestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
            TestItem test = testList.get(position);

            holder.testNameTextView.setText(test.getTitle());
            holder.testDescriptionTextView.setText(test.getDescription());

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTestClick(test);
                }
            });
        }

        @Override
        public int getItemCount() {
            return testList.size();
        }

        public static class TestViewHolder extends RecyclerView.ViewHolder {
            TextView testNameTextView;
            TextView testDescriptionTextView;

            public TestViewHolder(@NonNull View itemView) {
                super(itemView);
                testNameTextView = itemView.findViewById(R.id.test_name_text);
                testDescriptionTextView = itemView.findViewById(R.id.test_description_text);
            }
        }
    }
}
