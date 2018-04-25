package jb.fastreader.rsvp;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by Junaid Begawala
 */
class HtmlPage extends Media
{

    HtmlPage(@NonNull String title, @NonNull String uri, @NonNull String content, @NonNull Context context) throws FailedToSave
    {
        super(title, uri, content, context);
    }
}
