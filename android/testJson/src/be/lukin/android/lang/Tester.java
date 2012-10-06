package be.lukin.android.lang;

import be.lukin.android.lang.backend.LangService;
import be.lukin.android.lang.backend.Sentence;
import java.util.List;

public class Tester {

    public static void main(String[] args) {
        List<Sentence> list = LangService.getSentences();

        for(Sentence sentence : list) {
            System.out.println(sentence.toString());
        }
    }

}
