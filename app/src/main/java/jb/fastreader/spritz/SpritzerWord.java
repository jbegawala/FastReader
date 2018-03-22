package jb.fastreader.spritz;

/**
 * Created by jb on 3/21/18.
 */

public class SpritzerWord
{
    final String word;
    final int pivotPosition;
    final int additionalDelay;

    public SpritzerWord(String word, int pivotPosition)
    {
        this(word, pivotPosition, 0);
    }

    public SpritzerWord(String word, int pivotPosition, int additionalDelay)
    {
        this.word = word;
        this.pivotPosition = pivotPosition;
        this.additionalDelay = additionalDelay;
    }
}
