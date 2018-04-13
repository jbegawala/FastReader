package jb.fastreader.rsvp;

import android.util.Log;

import java.io.Serializable;

/**
 * Created by Junaid Begawala on 3/21/18.
 */

class Word implements Serializable
{
    private static final String TAG = Word.class.getSimpleName();

    private String word;
    private int pivotPosition;
    private int delayFactor;
    private boolean isNewWord;
    private boolean isNewSentence;
    private boolean isNewParagraph;

    /**
     * Creates an object that holds a short string that is flashed to the user
     * @param word String to flash
     */
    Word(String word, boolean isNewWord, boolean isNewSentence, boolean isNewParagraph)
    {
        if ( word.isEmpty() )
        {
            word = " ";
            Log.i(TAG, "Tried to instantiate with an empty string");
        }

        this.word = this.padWordAndFindPivot(word);
        this.delayFactor = delayMultiplierForWord(word);
        this.isNewWord = isNewWord;
        this.isNewSentence = isNewSentence;
        this.isNewParagraph = isNewParagraph;
    }

    /**
     * Finds character for pivot and saves to {@link #pivotPosition}.
     * Will also pad short words to align with pivot.
     * @param word String to find pivot for
     * @return Padded string if word is shorter then pivot position
     */
    private String padWordAndFindPivot(String word)
    {
        StringBuilder builder = new StringBuilder();

        if ( word.length() > Media.CHARS_LEFT_OF_PIVOT * 2 )
        {
            this.pivotPosition = Media.CHARS_LEFT_OF_PIVOT;
        }
        else if ( word.length() == 1 )
        {
            for (int i = 0; i < Media.CHARS_LEFT_OF_PIVOT; i++ )
            {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
        }
        else
        {
            int mid = word.length() / 2;
            int start = Media.CHARS_LEFT_OF_PIVOT - mid;
            for ( int i = 0; i <= start; i++ )
            {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
            this.pivotPosition = start + mid;
        }

        return word;
    }

    /**
     * Returns multiplier for given string. Longer words and words at the end of
     * sentences/clauses/etc. will have a multiplier greater than 1 to provide
     * user with a little more time to process information.
     * @param word String to find multiplier for
     * @return integer multiplier
     */
    private static int delayMultiplierForWord(String word)
    {
        if ( word.contains(",") || word.contains(":") || word.contains(";") || word.contains(".") || word.contains("?") || word.contains("!") || word.contains("\"") )
        {
            return 3;
        }

        else if ( word.length() >= Media.LONG_WORD_DELAY_THRESHOLD )
        {
            return 2;
        }

        return 1;
    }

    String getWord()
    {
        return this.word;
    }

    int getPivotPosition()
    {
        return this.pivotPosition;
    }

    int getDelayFactor()
    {
        return this.delayFactor;
    }

    boolean isNewWord()
    {
        return this.isNewWord;
    }

    boolean isNewSentence()
    {
        return this.isNewSentence;
    }

    boolean isNewParagraph()
    {
        return this.isNewParagraph;
    }
}