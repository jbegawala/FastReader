package jb.fastreader.events;

import jb.fastreader.formats.HtmlPage;

/**
 * Created by David Brodsky on 3/23/14.
 */
public class HttpUrlParsedEvent {

    private HtmlPage mResult;

    public HttpUrlParsedEvent(HtmlPage result) {
        mResult = result;
    }

    public HtmlPage getResult() {
        return mResult;
    }

}
