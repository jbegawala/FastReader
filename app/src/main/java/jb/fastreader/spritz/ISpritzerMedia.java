package jb.fastreader.spritz;

import java.io.Serializable;

/**
 * ISpritzerMedia provides an interface that abstracts different
 * media types.
 *
 * @author defer (diogo@underdev.org)
 */
public interface ISpritzerMedia extends Serializable
{

    String getTitle();

    String getSubtitle();

    int getWordCount();

    boolean hasNext();

    SpritzerWord next();
}
