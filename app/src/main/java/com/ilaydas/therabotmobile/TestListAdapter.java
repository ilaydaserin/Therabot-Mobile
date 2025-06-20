package com.ilaydas.therabotmobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.TestViewHolder> {

    private final List<Tests.TestItem> testList;
    private final OnTestClickListener listener;

    public interface OnTestClickListener {
        void onTestClick(Tests.TestItem testItem);
    }

    public TestListAdapter(List<Tests.TestItem> testList, OnTestClickListener listener) {
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
        Tests.TestItem test = testList.get(position);

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
