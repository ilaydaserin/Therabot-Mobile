package com.ilaydas.therabotmobile;

import java.util.List;

public class Question {
    private String text;
    private List<String> options;
    private int selectedOption = -1; // -1 means not answered

    public Question(String text, List<String> options) {
        this.text = text;
        this.options = options;
    }

    // Getters and setters
    public String getText() { return text; }
    public List<String> getOptions() { return options; }
    public int getSelectedOption() { return selectedOption; }
    public void setSelectedOption(int selectedOption) { this.selectedOption = selectedOption; }
}
