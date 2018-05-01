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
        text = this.preProcessText(text);
        this.processText(text);
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
     * Additional cleanup of extracted content. This separate step returns the content so that it can
     * be logged for further analysis.
     * @param text A string of raw text extracted from an article
     * @return Cleaner string
     */
    private String preProcessText(String text)
    {
        String groupingOpen = "\\(\\[{“";
        String groupingClose = "\\)\\]}”";
        String endOfSentence = "\\.\\?!;";
        text = text.replaceAll("[\\x00-\\x09\\x0B\\x0C\\x0E\\x1F]",""); // delete non-printable characters
        text = text.replaceAll("\\xA0"," ");                            // replace nbsp with space
        text = text.trim().replaceAll(" +", " ");                       // remove extra spaces
        text = text.replaceAll(" ?[\\r\\n]+", "\n");                    // clean up end of line

        text = text.replaceAll("([" + endOfSentence + "]+) ?([" + groupingClose + "]+)","$2$1");    // correct order
        text = text.replaceAll("([^ ])([" + groupingOpen + "])","$1 $2");
        text = text.replaceAll("([" + groupingOpen + "]) ([A-Za-z0-9])","$1$2");                    // Split from below because grouping could enclose multiple sentences
        text = text.replaceAll("([A-Za-z0-9]) ([" + groupingClose + endOfSentence + "])","$1$2");   // remove space between characters and comma
        text = text.replaceAll("([" + groupingClose + endOfSentence + "–—])+([A-Za-z0-9])","$1 $2");   // make sure there is a space after punctuation
        text = text.replaceAll("([A-Za-z" + groupingClose + "]) ?, ?([A-Za-z0-9" + groupingOpen + "])", "$1, $2");
        text = text.replaceAll("([0-9" + groupingClose + "]),([A-Za-z" + groupingOpen + "])", "$1, $2");
        text = text.replaceAll("([a-z0-9]+)([A-Z][a-z0-9]+)","$1 $2");  // infer two words accidentally joined together

        return text;
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
        ArrayList<Word> wordList = new ArrayList<>();

        paragraphs = input.split("\\n");
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
                    if (word.isEmpty())
                    {
                        continue;
                    }
                    if ( w + 1 == words.length )  // add period back to end of sentence
                    {
                        word += ".";
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
        DatabaseHelper.getInstance(null).updateArticle(this.ID, this.wordIndex);
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
    public void restart()
    {
        this.wordIndex = 0;
        this.sentenceIndex = 0;
        this.paragraphIndex = 0;
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