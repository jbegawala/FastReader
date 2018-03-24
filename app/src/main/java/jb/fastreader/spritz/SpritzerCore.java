package jb.fastreader.spritz;

import java.util.ArrayList;

class SpritzerCore
{
    static final int CHARS_LEFT_OF_PIVOT = 3;
    static final int LONG_WORD_DELAY_THRESHOLD = 8;

    private static int maxWordLength = 13;

    static ArrayList<SpritzerWord> ProcessText(String input)
    {
        return ProcessText(new ArrayList<SpritzerWord>(), input);
    }

    static ArrayList<SpritzerWord> ProcessText(ArrayList<SpritzerWord> wordList, String input)
    {
        // Merge adjacent spaces and split on spaces
        String[] wordArray = input.replaceAll("/\\s+/g", " ").split("[ \\r\\n]");

        // Add words to queue
        String word;
        for ( int i = 0; i < wordArray.length; i++ )
        {
            word = wordArray[i].trim();
            if ( word.length() > maxWordLength )
            {
                addWord(wordList, splitLongWord(word));
            }
            else
            {
                addWord(wordList, word);
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
        if ( word != null && !word.isEmpty() )
        {
            wordList.add(new SpritzerWord(word));
        }
    }

    /**
     * Split the given String if appropriate and
     * add the tail of the split to the head of
     *
     * @return array of word pieces
     */
    private static String[] splitLongWord(String word)
    {
        String[] wordSegments = new String[(word.length()/maxWordLength)+1];

        int splitIndex;
        int piece = 0;

        for ( int i = 0; i < word.length(); i = splitIndex)
        {
            splitIndex = findSplitIndex(word, i);
            String segment = word.substring(i, splitIndex);
            if ( !wordContainsSplittingCharacter(segment) && splitIndex < word.length() )
            {
                segment += "-";
            }
            wordSegments[piece++] = segment;
        }

        return wordSegments;
    }

    /**
     * Determine the split index on a given String
     * e.g If it exceeds maxWordLength or contains a hyphen
     *
     * @return the index on which to split the given String
     */
    private static int findSplitIndex(String word, int startingIndex)
    {
        int splitIndex;

        // leftover from previous word
        if ( (word.length() - startingIndex) <= maxWordLength )
        {
            splitIndex = word.length();
        }
        // Split long words, at hyphen or dot if present.
        else if (word.contains("-"))
        {
            splitIndex = word.indexOf("-", startingIndex) + 1;
        }
        else if (word.contains("."))
        {
            splitIndex = word.indexOf(".", startingIndex) + 1;
        }
        else if ((word.length() - startingIndex ) > (maxWordLength * 2) )
        {
            splitIndex = startingIndex + maxWordLength - 1;  // subtract one because hyphen will be added
        }
        else
        {
            splitIndex = startingIndex + (word.length() +1 ) / 2;
        }

//        // in case we found a split character that was > maxWordLength characters in.
//        if (splitIndex > maxWordLength)
//        {
//            // If we split the word at a splitting char like "-" or ".", we added one to the splitIndex
//            // in order to ensure the splitting char appears at the head of the split. Not accounting
//            // for this in the recursive call will cause a StackOverflowException
//            if ( wordContainsSplittingCharacter(word) )
//            {
//                splitIndex--;
//            }
//            return findSplitIndex(word.substring(startingIndex, splitIndex), 0);
//        }
        return splitIndex;
    }

    private static boolean wordContainsSplittingCharacter(String word)
    {
        return (word.contains(".") || word.contains("-"));
    }
}
