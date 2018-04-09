package jb.fastreader.spritz;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Junaid Begawala on 3/27/18.
 */

public abstract class SpritzerMedia implements ISpritzerMedia
{
    static final int CHARS_LEFT_OF_PIVOT = 3;
    static final int LONG_WORD_DELAY_THRESHOLD = 8;

    private static int maxWordLength = 13;
    private static int WORD = 0;
    private static int SENTENCE = 1;
    private static int PARAGRAPH = 2;
    private String title;
    private String uri;
    private ArrayList<SpritzerWord> content;
    private int contentLength;
    private int wordCount;

    // Zero based indices. Current value is the index to show next.
    private int index;
    private int wordIndex;
    private int sentenceIndex;
    private int paragraphIndex;

    private List<List<Integer>> mapToIndex;
    private Integer[][] mapFromIndex;

    /**
     * Creates media object
     * @param title Title of content
     * @param uri Uri of content
     * @param text A string without any markup or formatting
     */
    public SpritzerMedia(String title, String uri, String text)
    {
        this.title = title;
        this.uri = uri;
        this.processText(text);
        this.indexContent();
    }

    /**
     * Processes content into an array of strings to flash to user. Long words will be spread
     * out over multiple entries.
     * @param input A string without any markup or formatting
     */
    private void processText(String input)
    {
        String[] paragraphs;
        String[] sentences;
        String[] words;
        String word;
        boolean isNewParagraph;
        boolean isNewSentence;
        ArrayList<SpritzerWord> wordList = new ArrayList<>();

        paragraphs = input.trim().replaceAll("/\\s+/g", " ").replaceAll(" ?[\\r\\n]+", "\n").split("\\n");
        for (int p = 0; p < paragraphs.length; p++)
        {
            sentences = paragraphs[p].split("\\.");
            for (int s = 0; s < sentences.length; s++)
            {
                words = sentences[s].trim().split(" ");
                for (int w = 0; w < words.length; w++)
                {
                    isNewSentence = (w == 0);
                    isNewParagraph = (w == 0) && (s == 0);
                    word = words[w].trim();
                    if ( w + 1 == words.length )  // add period back to end of sentence
                    {
                        word += ".";
                    }
                    if (word.isEmpty())
                    {
                        continue;
                    }

                    if (word.length() > maxWordLength)
                    {
                        addWord(wordList, splitLongWord(word), isNewSentence, isNewParagraph);
                    }
                    else
                    {
                        addWord(wordList, word, true, isNewSentence, isNewParagraph);
                    }
                }
            }
        }

        this.content = wordList;
        this.contentLength = this.content.size();
    }

    /**
     * Maps words, sentences, and paragraphs
     */
    private void indexContent()
    {
        SpritzerWord spritzerWord;
        this.mapToIndex = new ArrayList<>(3);
        this.mapToIndex.add(WORD, new ArrayList<Integer>());
        this.mapToIndex.add(SENTENCE, new ArrayList<Integer>());
        this.mapToIndex.add(PARAGRAPH, new ArrayList<Integer>());
        this.mapFromIndex = new Integer[3][this.contentLength];

        for ( int i = 0; i < this.contentLength; i++ )
        {
            spritzerWord = this.content.get(i);
            if ( spritzerWord.isNewWord() )
            {
                this.mapToIndex.get(WORD).add(i);
                if ( spritzerWord.isNewSentence() )
                {
                    this.mapToIndex.get(SENTENCE).add(i);
                    if ( spritzerWord.isNewParagraph() )
                    {
                        this.mapToIndex.get(PARAGRAPH).add(i);
                    }
                }
            }
            this.mapFromIndex[WORD][i] = this.mapToIndex.get(WORD).size()-1;
            this.mapFromIndex[SENTENCE][i] = this.mapToIndex.get(SENTENCE).size()-1;
            this.mapFromIndex[PARAGRAPH][i] = this.mapToIndex.get(PARAGRAPH).size()-1;
        }

        this.index = 0;
        this.wordIndex = 0;
        this.sentenceIndex = 0;
        this.paragraphIndex = 0;
        this.wordCount =  this.mapToIndex.get(WORD).size();
    }

    /**
     * Generates an array of {@link SpritzerWord} from given string array
     * @param wordList Array of {@link SpritzerWord} to add to
     * @param words String array with words to add
     * @param isNewSentence True if given string is the start of a sentence, false otherwise
     * @param isNewParagraph True if given string is the start of a paragraph, false otherwise
     */
    private static void addWord(ArrayList<SpritzerWord> wordList, String[] words, boolean isNewSentence, boolean isNewParagraph)
    {
        addWord(wordList, words[0], true, isNewSentence, isNewParagraph);
        for (int i = 1; i < words.length; i++ )
        {
            addWord(wordList, words[i], false, false, false);
        }
    }

    /**
     * Generates a {@link SpritzerWord} from given string
     * @param wordList Array of {@link SpritzerWord} to add to
     * @param word String to add
     * @param isNewWord True if given string is the start of a word, false otherwise
     * @param isNewSentence True if given string is the start of a sentence, false otherwise
     * @param isNewParagraph True if given string is the start of a paragraph, false otherwise
     */
    private static void addWord(ArrayList<SpritzerWord> wordList, String word, boolean isNewWord, boolean isNewSentence, boolean isNewParagraph)
    {
        if ( word != null && !word.isEmpty() )
        {
            wordList.add(new SpritzerWord(word, isNewWord, isNewSentence, isNewParagraph));
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
    public void rewindCurrentSentence()
    {
        // If user is on the first word of the sentence, jump to previous sentence. Useful if paused
        // right after sentence finished.
        int curWordStartIndex = this.mapToIndex.get(WORD).get(Math.max(this.wordIndex - 1, 0));
        int curSentenceStartIndex = this.mapToIndex.get(SENTENCE).get(Math.max(this.sentenceIndex - 1, 0));
        if ( curWordStartIndex == curSentenceStartIndex )
        {
            this.rewindPreviousSentence();
        }
        else
        {
            this.sentenceIndex = Math.max(this.sentenceIndex - 1, 0);
            this.index = this.mapToIndex.get(SENTENCE).get(this.sentenceIndex);
            this.wordIndex = this.mapFromIndex[WORD][this.index];
        }
    }

    @Override
    public void rewindPreviousSentence()
    {
        this.sentenceIndex = Math.max(this.sentenceIndex - 2, 0);
        this.index = this.mapToIndex.get(SENTENCE).get(this.sentenceIndex);
        this.wordIndex = this.mapFromIndex[WORD][this.index];
        this.paragraphIndex = this.mapFromIndex[PARAGRAPH][this.index];
    }

    @Override
    public void rewindCurrentParagraph()
    {
        // If user is on the first sentence of the paragraph, jump to previous paragraph
        int curSentenceStartIndex = this.mapToIndex.get(SENTENCE).get(Math.max(this.sentenceIndex - 1, 0));
        int curParagraphStartIndex = this.mapToIndex.get(PARAGRAPH).get(Math.max(this.paragraphIndex - 1, 0));
        int shift = curSentenceStartIndex == curParagraphStartIndex ? 2 : 1;

        this.paragraphIndex = Math.max(this.paragraphIndex - shift, 0);
        this.index = this.mapToIndex.get(PARAGRAPH).get(this.paragraphIndex);
        this.sentenceIndex = this.mapFromIndex[SENTENCE][this.index];
        this.wordIndex= this.mapFromIndex[WORD][this.index];
    }

    @Override
    public String getTitle()
    {
        return this.title;
    }

    public String getUri()
    {
        return this.uri;
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
        return ( this.index < this.contentLength );
    }

    @Override
    public SpritzerWord next()
    {
        SpritzerWord spritzerWord = this.content.get(this.index++);
        if ( spritzerWord.isNewWord() )
        {
            this.wordIndex++;
            if ( spritzerWord.isNewSentence() )
            {
                this.sentenceIndex++;
                if ( spritzerWord.isNewParagraph() )
                {
                    this.paragraphIndex++;
                }
            }
        }
        return spritzerWord;
    }
}
