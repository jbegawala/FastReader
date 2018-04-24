package jb.fastreader.rsvp;

import java.io.Serializable;

/**
 * Created by Junaid Begawala on 4/8/18.
 */
public interface IRSVPMedia extends Serializable
{
    String getTitle();

    String getUri();

    int getWordCount();

    int getWordIndex();

    void rewindCurrentSentence();

    void rewindPreviousSentence();

    void rewindCurrentParagraph();

    void restart();

    void saveState();

    boolean hasNext();

    Word next();
}
