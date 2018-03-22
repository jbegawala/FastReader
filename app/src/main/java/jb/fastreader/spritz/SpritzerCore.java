package jb.fastreader.spritz;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import jb.fastreader.R;


/**
 * SpritzerCore parses a String into a Queue
 * of words, and displays them one-by-one
 * onto a TextView at a given WPM.
 */
public class SpritzerCore
{

    static final int MSG_PRINT_WORD = 1;
    static final int MSG_SET_ENABLED = 2;
    static final int CHARS_LEFT_OF_PIVOT = 3;

    private static int maxWordLength = 13;

    public static ArrayList<SpritzerWord> ProcessText(String input)
    {
        return ProcessText(new ArrayList<SpritzerWord>(), input);
    }


    public static ArrayList<SpritzerWord> ProcessText(ArrayList<SpritzerWord> wordList, String input)
    {
        // Merge adjacent spaces and split on spaces
        String[] wordArray = input.replaceAll("/\\s+/g", " ").split(" ");

        // Add words to queue
        for ( int i = 0; i < wordArray.length; i++ )
        {
            if ( wordArray[i].length() > maxWordLength )
            {
                addWord(wordList, splitLongWord(wordArray[i]));
            }
            else
            {
                addWord(wordList, wordArray[i]);
            }
        }

        return wordList;
    }

    private static void addWord(ArrayList<SpritzerWord> wordList, String[] words)
    {
        for (int i = 0; i < words.length; i++ )
        {
            addWord(wordList, words[i]);
        }
    }

    private static void addWord(ArrayList<SpritzerWord> wordList, String word)
    {

    }



    /**
     * Split the given String if appropriate and
     * add the tail of the split to the head of
     * {@link #wordList}
     * <p/>
     * Currently public for testing purposes
     *
     * @param word
     * @return
     */
    public static String[] splitLongWord(String word)
    {

            int splitIndex = findSplitIndex(word);
            String firstSegment;
            firstSegment = word.substring(0, splitIndex);
            // A word split is always indicated with a hyphen unless ending in a period
            if (!firstSegment.contains("-") && !firstSegment.endsWith(".")) {
                firstSegment = firstSegment + "-";
            }
            wordList.add(mCurWordIdx + 1, word.substring(splitIndex));
            word = firstSegment;
        return word;
    }

    /**
     * Determine the split index on a given String
     * e.g If it exceeds maxWordLength or contains a hyphen
     *
     * @param thisWord
     * @return the index on which to split the given String
     */
    private int findSplitIndex(String thisWord) {
        int splitIndex;
        // Split long words, at hyphen or dot if present.
        if (thisWord.contains("-")) {
            splitIndex = thisWord.indexOf("-") + 1;
        } else if (thisWord.contains(".")) {
            splitIndex = thisWord.indexOf(".") + 1;
        } else if (thisWord.length() > maxWordLength * 2) {
            // if the word is floccinaucinihilipilifcation, for example.
            splitIndex = maxWordLength - 1;
            // 12 characters plus a "-" == 13.
        } else {
            // otherwise we want to split near the middle.
            splitIndex = Math.round(thisWord.length() / 2F);
        }
        // in case we found a split character that was > maxWordLength characters in.
        if (splitIndex > maxWordLength) {
            // If we split the word at a splitting char like "-" or ".", we added one to the splitIndex
            // in order to ensure the splitting char appears at the head of the split. Not accounting
            // for this in the recursive call will cause a StackOverflowException
            return findSplitIndex(thisWord.substring(0,
                    wordContainsSplittingCharacter(thisWord) ? splitIndex - 1 : splitIndex));
        }
        return splitIndex;
    }

    private boolean wordContainsSplittingCharacter(String word)
    {
        return (word.contains(".") || word.contains("-"));
    }


    private void printWord(String word) {
        int startSpan = 0;
        int endSpan = 0;
        word = word.trim();
        if (word.length() == 1) {
            StringBuilder builder = new StringBuilder();
            for (int x = 0; x < CHARS_LEFT_OF_PIVOT; x++) {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
            startSpan = CHARS_LEFT_OF_PIVOT;
            endSpan = startSpan + 1;
        } else if (word.length() <= CHARS_LEFT_OF_PIVOT * 2) {
            StringBuilder builder = new StringBuilder();
            int halfPoint = word.length() / 2;
            int beginPad = CHARS_LEFT_OF_PIVOT - halfPoint;
            for (int x = 0; x <= beginPad; x++) {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
            startSpan = halfPoint + beginPad;
            endSpan = startSpan + 1;
        } else {
            startSpan = CHARS_LEFT_OF_PIVOT;
            endSpan = startSpan + 1;
        }

        Spannable spanRange = new SpannableString(word);
        TextAppearanceSpan tas = new TextAppearanceSpan(textViewTarget.getContext(), R.style.PivotLetter);
        spanRange.setSpan(tas, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textViewTarget.setText(spanRange);
    }


    private int delayMultiplierForWord(String word)
    {
        // double rest if length > 6 or contains (.,!?)
        if (word.length() >= 6 || word.contains(",") || word.contains(":") || word.contains(";") || word.contains(".") || word.contains("?") || word.contains("!") || word.contains("\""))
        {
            return 3;
        }
        return 1;
    }
}
