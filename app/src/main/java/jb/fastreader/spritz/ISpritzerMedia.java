package jb.fastreader.spritz;

import java.io.Serializable;

/**
 * Created by Junaid Begawala on 4/8/18.
 */
public interface ISpritzerMedia extends Serializable
{

    String getTitle();

    String getUri();

    int getWordCount();

    int getWordIndex();

    void rewindCurrentSentence();

    void rewindPreviousSentence();

    void rewindCurrentParagraph();

    boolean hasNext();

    SpritzerWord next();
}
