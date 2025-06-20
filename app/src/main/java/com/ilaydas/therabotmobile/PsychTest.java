package com.ilaydas.therabotmobile;

import java.util.List;

//for tests
public class PsychTest {
    public String test_id;
    public String title;
    public String description;
    public List<Question> questions;
    public List<String> options;
    public List<Integer> scores;

    public static class Question {
        public int id;
        public String text;
        public String trait;
        public boolean reverse;
    }
}

