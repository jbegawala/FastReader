package jb.fastreader.spritz;

import java.util.ArrayList;

/**
 * Created by Junaid Begawala on 3/27/18.
 */

public abstract class SpritzerMedia implements ISpritzerMedia
{
    static final int CHARS_LEFT_OF_PIVOT = 3;
    static final int LONG_WORD_DELAY_THRESHOLD = 8;

    private static int maxWordLength = 13;
    private String title;
    private String subtitle;
    private ArrayList<SpritzerWord> content;
    private int contentIndex;  // index of text to show next
    private int contentLength;

    private int wordIndex;  // number of words shown to user including current word
    private int wordCount;

    /**
     * Creates media object
     * @param title Title of content
     * @param subtitle Subtitle of content, typically author or url
     * @param content A string without any markup or formatting
     */
    public SpritzerMedia(String title, String subtitle, String content)
    {
        this.title = title;
        this.subtitle = subtitle;
        this.processText(content);
    }

    /**
     * Processes content into an array of strings to flash to user. Long words will be spread
     * out over multiple entries.
     * @param input A string without any markup or formatting
     */
    private void processText(String input)
    {
        ArrayList<SpritzerWord> wordList = new ArrayList<>();

        // Merge adjacent spaces and split on spaces
        String[] wordArray = input.replaceAll("/\\s+/g", " ").split("[ \\r\\n]");
        this.wordIndex = 0;
        this.wordCount = wordArray.length;

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
                addWord(wordList, word, true);
            }
        }

        this.content = wordList;
        this.contentIndex = 0;
        this.contentLength = wordList.size();
    }

    /**
     * Generates an array of {@link SpritzerWord} from given string array
     * @param wordList Array of {@link SpritzerWord} to add to
     * @param words String array with words to add
     */
    private static void addWord(ArrayList<SpritzerWord> wordList, String[] words)
    {
        for (int i = 0; i < words.length; i++ )
        {
            addWord(wordList, words[i], (i == 0));
        }
    }

    /**
     * Generates a {@link SpritzerWord} from given string
     * @param wordList Array of {@link SpritzerWord} to add to
     * @param word String to add
     * @param isNewWord True if given string is the start of a word, false otherwise
     */
    private static void addWord(ArrayList<SpritzerWord> wordList, String word, boolean isNewWord)
    {
        if ( word != null && !word.isEmpty() )
        {
            wordList.add(new SpritzerWord(word, isNewWord));
        }
    }

    /**
     * Splits the given string into an array based on the {@link #maxWordLength}. Some entries
     * in the array can be empty.
     * @return String array of word segments
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
            if ( splittingCharacterIndex(segment) == -1 && splitIndex < word.length() )
            {
                segment += "-";
            }
            wordSegments[piece++] = segment;
        }

        return wordSegments;
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
     * Determines whether the given string contains a character to split h
     * @param word
     * @return
     */
    private static boolean wordContainsSplittingCharacter(String word)
    {
        return (word.contains(".") || word.contains("-"));
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

    @Override
    public String getTitle()
    {
        return this.title;
    }

    @Override
    public String getSubtitle()
    {
        return this.subtitle;
    }

    @Override
    public int getWordCount()
    {
        return this.wordCount;
    }

    @Override
    public int getWordIndex()
    {
        return this.wordIndex;
    }

    @Override
    public boolean hasNext()
    {
        return ( this.contentIndex < this.contentLength );
    }

    @Override
    public SpritzerWord next()
    {
        SpritzerWord spritzerWord = this.content.get(this.contentIndex++);
        if ( spritzerWord.isNewWord() )
        {
            this.wordIndex++;
        }
        return spritzerWord;
    }
}
