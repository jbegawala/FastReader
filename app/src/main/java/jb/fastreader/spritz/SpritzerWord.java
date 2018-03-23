package jb.fastreader.spritz;

/**
 * Created by jb on 3/21/18.
 */

public class SpritzerWord
{
    private String word;
    private int pivotPosition;
    private int additionalDelay;

    public SpritzerWord(String word)
    {
        if ( word.isEmpty() )
        {
            word = " ";
        }

        this.word = this.padWordAndFindPivot(word);
        this.additionalDelay = this.delayMultiplierForWord(word);
    }

    private String padWordAndFindPivot(String word)
    {

        this.pivotPosition = SpritzerCore.CHARS_LEFT_OF_PIVOT;
        StringBuilder builder = new StringBuilder();
        if ( word.length() == 1 )
        {
            for ( int i = 0; i < SpritzerCore.CHARS_LEFT_OF_PIVOT; i++)
            {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
        }

        else if ( word.length() <= SpritzerCore.CHARS_LEFT_OF_PIVOT * 2 )
        {
            int halfPoint = word.length() / 2;
            int beginPad = SpritzerCore.CHARS_LEFT_OF_PIVOT - halfPoint;
            for (int i = 0; i <= beginPad; i++)
            {
                builder.append(" ");
            }
            builder.append(word);
            word = builder.toString();
            this.pivotPosition = halfPoint + beginPad;
        }

        return word;
    }

    private int delayMultiplierForWord(String word)
    {
        if ( word.contains(",") || word.contains(":") || word.contains(";") || word.contains(".") || word.contains("?") || word.contains("!") || word.contains("\"") )
        {
            return 3;
        }

        else if ( word.length() >= SpritzerCore.LONG_WORD_DELAY_THRESHOLD )
        {
            return 2;
        }

        return 1;
    }

    String getWord()
    {
        return this.word;
    }

    int getPivotPosition()
    {
        return this.pivotPosition;
    }

    int getAdditionalDelay()
    {
        return this.additionalDelay;
    }
}
