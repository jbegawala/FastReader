package jb.fastreader.events;

import jb.fastreader.formats.DummyHtmlPage;

/**
 * Created by jb on 2/25/18.
 */

public class DummyParsedEvent
{

    private DummyHtmlPage mResult;

    public DummyParsedEvent(DummyHtmlPage result) {
        this.mResult = result;
    }

    public DummyHtmlPage getResult() {
        return this.mResult;
    }
}
