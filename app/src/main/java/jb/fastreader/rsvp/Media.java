package jb.fastreader.rsvp;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import jb.fastreader.Settings;
import jb.fastreader.library.DatabaseHelper;

/**
 * Created by Junaid Begawala on 3/27/18.
 */
abstract class Media implements IRSVPMedia
{
    static final int CHARS_LEFT_OF_PIVOT = 3;
    static final int LONG_WORD_DELAY_THRESHOLD = 8;
    private static final int SENTENCE = 0;
    private static final int PARAGRAPH = 1;
    private static final char SENTENCE_MARKER = '\u0001';   // Removed during preprocessing
    private static final char PARAGRAPH_MARKER = '\u0002';  // Removed during preprocessing
    private static final String END_OF_SENTENCE_CHARS_NO_PERIOD = "\\?!;";
    private static final String END_OF_SENTENCE_CHARS = "\\." + END_OF_SENTENCE_CHARS_NO_PERIOD;
    private static final String GROUPING_OPEN_CHARS = "\\(\\[{“";
    private static final String GROUPING_CLOSE_CHARS = "\\)\\]}”";

    private long ID;
    private String title;
    private String uri;
    private ArrayList<Word> content;
    private int wordCount;

    // Zero based indices. Current value is the index to show next.
    private int wordIndex;
    private int sentenceIndex;
    private int paragraphIndex;

    private List<List<Integer>> mapToIndex;
    private Integer[][] mapFromIndex;

    /**
     * Parses given text to generate structured media file and saves to disk
     * @param title Title of content
     * @param uri Uri of content
     * @param text A string without any markup or formatting
     */
    Media(@NonNull String title, @NonNull String uri, @NonNull String text, @NonNull Context context) throws FailedToSave
    {
        this.title = title;
        this.uri = uri;

        if ( text.isEmpty() )
        {
            throw new FailedToSave("No article content to save");
        }

        // Process media
        text = this.processText(text);
        this.importText(text);
        this.indexContent();

        // Save media
        String fileName = UUID.nameUUIDFromBytes(uri.getBytes()).toString();
        long result = DatabaseHelper.getInstance(context).addArticle(fileName, title, uri, this.getWordIndex(), this.getWordCount());
        if ( result == -2 )
        {
            return;  // article already exists in database
        }
        if ( result == -1 )
        {
            throw new FailedToSave("Failed to insert article into sql database");
        }

        this.ID = result;
        try
        {
            File file = new File(DatabaseHelper.getInstance(context).getArticleDirectory(), fileName);
            FileOutputStream fos = new FileOutputStream(file, false);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
            fos.close();

            if ( Settings.saveRawExtract(context) )
            {
                File root = DatabaseHelper.getInstance(context).getExtractDirectory();
                if ( !root.exists() && !root.mkdir())
                {
                    throw new FailedToSave("Failed to create directory on external storage");
                }
                File extractFile = new File(root, fileName + ".txt");
                FileOutputStream extractFos = new FileOutputStream(extractFile, false);
                extractFos.write(text.getBytes());
                extractFos.close();

                root = DatabaseHelper.getInstance(context).getProcessedMediaDirectory();
                if ( !root.exists() && !root.mkdir())
                {
                    throw new FailedToSave("Failed to create directory on external storage");
                }
                File processedFile = new File(root, fileName + ".csv");
                FileWriter fileWriter = new FileWriter(processedFile);
                String line;
                Word word;
                String delim = "\",\"";
                fileWriter.write("\"Word fragment" + delim + "New sentence?" + delim + "New paragraph?\"\n");
                for ( int i = 0; i < this.content.size(); i++ )
                {
                    word = this.content.get(i);
                    line = "\"" + word.getWord();
                    if ( word.isNewSentence() )
                    {
                        line = line + delim + "true";
                        if ( word.isNewParagraph() )
                        {
                            line = line + delim + "true";
                        }
                    }
                    line = line + "\"\n";
                    fileWriter.write(line);
                }
                fileWriter.close();
            }
        }
        catch (java.io.IOException e)
        {
            throw new FailedToSave(e.getMessage());
        }
    }

    /**
     * Divides text into sentences
     * @param text A string of raw text extracted from an article
     * @return Cleaner string
     */
    private String processText(String text)
    {
        ArrayList<String> noSplitList = loadDoNotSplit();
        StringBuilder builder = new StringBuilder(1250);

        String[] paragraphs = preProcessText(text).split(Character.toString(PARAGRAPH_MARKER));
        String[] words;
        String tmp;
        String[] words2;
        Pattern splitJoinedWords1 = Pattern.compile("([" + GROUPING_OPEN_CHARS + "]*[^" + GROUPING_OPEN_CHARS + END_OF_SENTENCE_CHARS_NO_PERIOD + GROUPING_CLOSE_CHARS + "]+[" + GROUPING_CLOSE_CHARS + "]*[" + END_OF_SENTENCE_CHARS_NO_PERIOD +"]+[" + GROUPING_CLOSE_CHARS + "]*)" +
                "([" + GROUPING_OPEN_CHARS + "]*[^" + GROUPING_OPEN_CHARS + END_OF_SENTENCE_CHARS_NO_PERIOD + GROUPING_CLOSE_CHARS + "]+[" + GROUPING_CLOSE_CHARS + "]*)");

        Pattern removeGrouping = Pattern.compile("[" + GROUPING_OPEN_CHARS + "]*([^" + GROUPING_OPEN_CHARS + GROUPING_CLOSE_CHARS + "]+)[" + GROUPING_CLOSE_CHARS + "]*");

        Pattern endOfSentence = Pattern.compile("([" + END_OF_SENTENCE_CHARS + "]+[" + GROUPING_CLOSE_CHARS + "]*)");

        Pattern splitJoinedWords2 = Pattern.compile("([a-z0-9]+)([A-Z][a-z0-9]+)");

        Pattern numberUpperCase = Pattern.compile("[^A-Z]?[0-9]+\\.[0-9]+%?");

        Pattern initialUpperCase = Pattern.compile("[A-Z]\\.");

        for ( int i = 0; i < paragraphs.length; i++ )
        {
            words = paragraphs[i].trim().split(" +");
            for ( int j = 0; j < words.length; j++ )
            {
                words2 = splitJoinedWords1.matcher(words[j]).replaceAll("$1 $2").trim().split(" ");

                for ( int k = 0; k < words2.length; k++ )
                {
                    tmp = removeGrouping.matcher(words2[k]).replaceAll("$1").toUpperCase();
                    if ( initialUpperCase.matcher(tmp).matches() || noSplitList.contains(tmp) || numberUpperCase.matcher(tmp).matches() )
                    {
                        builder.append(words2[k]).append(" ");
                    }
                    else
                    {
                        // Label end of sentence
                        tmp = endOfSentence.matcher(words2[k]).replaceAll("$1" + SENTENCE_MARKER);

                        // Infer two words accidentally joined together
                        builder.append(splitJoinedWords2.matcher(tmp).replaceAll("$1 $2")).append(" ");
                    }
                }
            }
            builder.append(PARAGRAPH_MARKER);
        }

        return builder.toString();
    }

    /**
     * Additional cleanup of extracted content
     * @param text A string of raw text extracted from an article
     * @return Cleaner string
     */
    private static String preProcessText(String text)
    {
        text = text.replaceAll("[\\x00-\\x09\\x0B\\x0C\\x0E\\x1F]",""); // delete non-printable characters (some are used by data structure)
        text = text.replaceAll("\\xA0"," ");                            // replace nbsp with space
        text = text.trim().replaceAll(" +", " ");                       // remove extra spaces
        text = text.replaceAll(" ?[\\r\\n]+", Character.toString(PARAGRAPH_MARKER));  // clean up and label end of paragraph

        text = text.replaceAll("([" + GROUPING_OPEN_CHARS + "]) +([" + GROUPING_OPEN_CHARS + "])","$1$2");    // Collapse adjacent grouping characters
        text = text.replaceAll("([" + GROUPING_CLOSE_CHARS + "]) +([" + GROUPING_CLOSE_CHARS + "])","$1$2");  // Collapse adjacent grouping characters
        text = text.replaceAll("([^ ])([" + GROUPING_OPEN_CHARS + "]+)","$1 $2");
        text = text.replaceAll("([" + GROUPING_OPEN_CHARS + "]) +","$1");                    // Remove extra spaces (split from below because grouping could enclose multiple sentences)
        text = text.replaceAll(" +([" + GROUPING_CLOSE_CHARS + END_OF_SENTENCE_CHARS + "/,])","$1");
        text = text.replaceAll("([" + GROUPING_CLOSE_CHARS + END_OF_SENTENCE_CHARS_NO_PERIOD + "–—])([A-Za-z0-9])","$1 $2");   // make sure there is a space after punctuation
        text = text.replaceAll("([A-Za-z" + GROUPING_CLOSE_CHARS + "]) ?, ?([A-Za-z0-9" + GROUPING_OPEN_CHARS + "])", "$1, $2");
        text = text.replaceAll("([0-9" + GROUPING_CLOSE_CHARS + "]),([A-Za-z" + GROUPING_OPEN_CHARS + "])", "$1, $2");
        text = text.replaceAll("[/]([a-zA-Z])","/ $1");

        return text;
    }

    /**
     * Builds a list of some abbreviations that shouldn't be considered end of sentence
     * @return List of abbreviations
     */
    private static ArrayList<String> loadDoNotSplit()
    {
        ArrayList<String> list = new ArrayList<>(1);
        list.add("U.S.");
        list.add("U.S.A.");
        list.add("A.M.");
        list.add("P.M.");
        list.add("A.C.");
        list.add("D.C.");
        list.add("PROF.");
        list.add("DR.");
        list.add("MR.");
        list.add("MRS.");
        list.add("JR.");
        list.add("I.E.");
        list.add("E.G.");
        list.add("ETC.");
        list.add("ST.");
        list.add("CORP.");
        list.add("INC.");
        list.add("LTD.");
        list.add("R.S.V.P.");
        list.add("YOUTUBE");
        list.add("JAN.");
        list.add("FEB.");
        list.add("MAR.");
        list.add("APR.");
        list.add("JUN.");
        list.add("JUL.");
        list.add("AUG.");
        list.add("SEP.");
        list.add("OCT.");
        list.add("NOV.");
        list.add("DEC.");
        return list;
    }

    /**
     * Imports processed text into data structure.
     * @param processedText A string without any markup or formatting
     */
    private void importText(String processedText)
    {
        String[] paragraphs;
        String[] sentences;
        String[] words;
        String word;
        boolean isNewParagraph;
        boolean isNewSentence;
        ArrayList<Word> wordList = new ArrayList<>();

        paragraphs = processedText.split(Character.toString(PARAGRAPH_MARKER));
        for (int p = 0; p < paragraphs.length; p++)
        {
            sentences = paragraphs[p].split(Character.toString(SENTENCE_MARKER));
            for (int s = 0; s < sentences.length; s++)
            {
                words = sentences[s].trim().split(" +");
                for (int w = 0; w < words.length; w++)
                {
                    isNewSentence = (w == 0);
                    isNewParagraph = (w == 0) && (s == 0);
                    word = words[w].trim();
                    if (word.isEmpty())
                    {
                        continue;
                    }

                    wordList.add(new Word(word, isNewSentence, isNewParagraph));
                }
            }
        }

        this.content = wordList;
        this.wordCount = this.content.size();
    }

    /**
     * Maps words, sentences, and paragraphs
     */
    private void indexContent()
    {
        Word word;
        this.mapToIndex = new ArrayList<>(2);
        this.mapToIndex.add(SENTENCE, new ArrayList<Integer>());
        this.mapToIndex.add(PARAGRAPH, new ArrayList<Integer>());
        this.mapFromIndex = new Integer[2][this.wordCount];

        for ( int i = 0; i < this.wordCount; i++ )
        {
            word = this.content.get(i);
            if ( word.isNewSentence() )
            {
                this.mapToIndex.get(SENTENCE).add(i);
                if ( word.isNewParagraph() )
                {
                    this.mapToIndex.get(PARAGRAPH).add(i);
                }
            }
            this.mapFromIndex[SENTENCE][i] = this.mapToIndex.get(SENTENCE).size()-1;
            this.mapFromIndex[PARAGRAPH][i] = this.mapToIndex.get(PARAGRAPH).size()-1;
        }

        this.wordIndex = 0;
        this.sentenceIndex = 0;
        this.paragraphIndex = 0;
    }

    @Override
    public void saveState()
    {
        int startOfSentence = this.mapToIndex.get(SENTENCE).get(Math.max(this.sentenceIndex - 1, 0));
        DatabaseHelper.getInstance(null).updateArticle(this.ID, startOfSentence);
    }

    @Override
    public void rewindCurrentSentence()
    {
        // If user is on the first word of the sentence, jump to previous sentence. Useful if paused
        // right after sentence finished.
        int curSentenceStartIndex = this.mapToIndex.get(SENTENCE).get(Math.max(this.sentenceIndex - 1, 0));
        if ( this.wordIndex == curSentenceStartIndex )
        {
            this.rewindPreviousSentence();
        }
        else
        {
            this.sentenceIndex = Math.max(this.sentenceIndex - 1, 0);
            this.wordIndex = curSentenceStartIndex;
        }
    }

    @Override
    public void rewindPreviousSentence()
    {
        this.sentenceIndex = Math.max(this.sentenceIndex - 2, 0);
        this.wordIndex = this.mapToIndex.get(SENTENCE).get(this.sentenceIndex);
        this.paragraphIndex = this.mapFromIndex[PARAGRAPH][this.wordIndex];
    }

    @Override
    public void rewindCurrentParagraph()
    {
        // If user is on the first sentence of the paragraph, jump to previous paragraph
        int curSentenceStartIndex = this.mapToIndex.get(SENTENCE).get(Math.max(this.sentenceIndex - 1, 0));
        int curParagraphStartIndex = this.mapToIndex.get(PARAGRAPH).get(Math.max(this.paragraphIndex - 1, 0));
        int shift = curSentenceStartIndex == curParagraphStartIndex ? 2 : 1;

        this.paragraphIndex = Math.max(this.paragraphIndex - shift, 0);
        this.wordIndex = this.mapToIndex.get(PARAGRAPH).get(this.paragraphIndex);
        this.sentenceIndex = this.mapFromIndex[SENTENCE][this.wordIndex];
    }

    @Override
    public void setIndex(int index)
    {
        if ( index == 0 || index >= this.wordCount )
        {
            this.wordIndex = 0;
            this.sentenceIndex = 0;
            this.paragraphIndex = 0;
        }
        else
        {
            this.wordIndex = index;
            this.sentenceIndex = this.mapFromIndex[SENTENCE][index];
            this.paragraphIndex = this.mapFromIndex[PARAGRAPH][index];
        }
        this.saveState();
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
        return ( this.wordIndex < this.wordCount );
    }

    @Override
    public Word next()
    {
        Word word = this.content.get(this.wordIndex++);
        if ( word.isNewSentence() )
        {
            this.sentenceIndex++;
            if ( word.isNewParagraph() )
            {
                this.paragraphIndex++;
            }
        }
        return word;
    }
}

class FailedToSave extends IOException
{
    FailedToSave(String message)
    {
        super(message);
    }
}