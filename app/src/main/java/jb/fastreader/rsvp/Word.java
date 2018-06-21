package jb.fastreader.rsvp;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Junaid Begawala on 3/21/18.
 */

class Word implements Serializable
{
    private static final String TAG = Word.class.getSimpleName();
    private static final int maxWordLength = 13;

    private String word;
    private String[] fragments;
    private int[] pivotPosition;
    private int[] delayFactor;
    private boolean isNewSentence;
    private boolean isNewParagraph;

    /**
     * Creates an object that holds a short string that is flashed to the user
     * @param word String to flash
     */
    Word(String word, boolean isNewSentence, boolean isNewParagraph)
    {
        if ( word.isEmpty() )
        {
            word = " ";
            Log.i(TAG, "Tried to instantiate with an empty string");
        }

        this.word = word;

        if ( word.length() <= maxWordLength )
        {
            this.fragments = new String[1];
            this.pivotPosition = new int[this.fragments.length];
            this.delayFactor = new int[this.fragments.length];

            this.fragments[0] = this.padWordAndFindPivot(word, 0);
            this.delayFactor[0] = delayMultiplierForWord(word);
        }
        else
        {
            this.fragments = splitLongWord(word);
            this.pivotPosition = new int[this.fragments.length];
            this.delayFactor = new int[this.fragments.length];
            for ( int i = 0; i < this.fragments.length; i++ )
            {
                this.fragments[i] = this.padWordAndFindPivot(this.fragments[i], i);
                this.delayFactor[i] = delayMultiplierForWord(this.fragments[i]);
            }
        }

        this.isNewSentence = isNewSentence;
        this.isNewParagraph = isNewParagraph;
    }

    /**
     * Splits the given string into an array based on the {@link #maxWordLength}. Some entries
     * in the array can be empty.
     * @return String array of word segments
     */
    private static String[] splitLongWord(String word)
    {
        ArrayList<String> wordSegments = new ArrayList<>(3);

        int splitIndex;
        for ( int i = 0; i < word.length(); i = splitIndex)
        {
            splitIndex = findSplitIndex(word, i);
            String segment = word.substring(i, splitIndex);
            if ( splittingCharacterIndex(segment) == -1 && splitIndex < word.length() )
            {
                segment += "-";
            }
            if ( !segment.isEmpty())
            {
                wordSegments.add(segment);
            }
        }
        String[] wordSegmentAray = new String[wordSegments.size()];
        for ( int i = 0; i < wordSegments.size(); i++ )
        {
            wordSegmentAray[i] = wordSegments.get(i);
        }
        return wordSegmentAray;
    }

    /**
     * Calculates where to split the given string.
     * @return The position on which to split the given string
     */
    private static int findSplitIndex(String word, int startingIndex)
    {
        // Leftover from previous word
        if ( (word.length() - startingIndex) <= maxWordLength )
        {
            return word.length();
        }

        // Split at splitting character if present
        int splitIndex = splittingCharacterIndex(word, startingIndex);
        if ( 0 <= splitIndex )
        {
            return splitIndex + 1;
        }

        // Split at full length if word is long enough
        if ((word.length() - startingIndex ) > (maxWordLength * 2) )
        {
            return startingIndex + maxWordLength - 1;  // subtract one because hyphen will be added
        }

        // Split in the middle
        return startingIndex + (word.length() + 1) / 2;
    }

    /**
     * Determines whether the given string contains a character to split on
     * @param word String to check
     * @return The index of the first occurrence of a split character, or -1 if there is no such
     * occurrence.
     */
    private static int splittingCharacterIndex(String word)
    {
        return splittingCharacterIndex(word, 0);
    }

    /**
     * Determines whether the given string contains a character to split on
     * @param word String to check
     * @param startingIndex The index from which to start the search
     * @return The index of the first occurrence of a split character, starting at the specified
     * index, or -1 if there is no such occurrence.
     */
    private static int splittingCharacterIndex(String word, int startingIndex)
    {
        int index = word.indexOf("-", startingIndex);
        if ( 0 <= index )
        {
            return index;
        }
        index = word.indexOf(".", startingIndex);
        if ( 0 <= index )
        {
            return index;
        }
        return -1;
    }

    /**
     * Finds character for pivot and saves to {@link #pivotPosition}.
     * Will also pad short words to align with pivot.
     * @param word String to find pivot for
     * @return Padded string if word is shorter then pivot position
     */
    private String padWordAndFindPivot(String word, int index)
    {
        StringBuilder builder = new StringBuilder();

        if ( word.length() > Media.CHARS_LEFT_OF_PIVOT * 2 )
        {
            this.pivotPosition[index] = Media.CHARS_LEFT_OF_PIVOT;
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
            this.pivotPosition[index] = start + mid;
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

    int getFragmentCount()
    {
        return this.fragments.length;
    }

    String getWord()
    {
        return this.word;
    }

    @Override
    public String toString()
    {
        return this.word;
    }

    String getWordFragment(int index)
    {
        return this.fragments[index];
    }

    int getPivotPosition(int index)
    {
        return this.pivotPosition[index];
    }

    int getDelayFactor(int index)
    {
        return this.delayFactor[index];
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
