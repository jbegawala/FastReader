package jb.fastreader.spritz;

import java.io.Serializable;

public interface ISpritzerMedia extends Serializable
{

    String getTitle();

    String getSubtitle();

    int getWordCount();

    int getWordIndex();

    void rewindCurrentSentence();

    void rewindPreviousSentence();

    void rewindCurrentParagraph();

    boolean hasNext();

    SpritzerWord next();
}
