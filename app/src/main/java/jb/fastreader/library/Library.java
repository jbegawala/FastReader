package jb.fastreader.library;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import jb.fastreader.R;

/**
 * Created by Junaid Begawala on 4/8/18.
 */

public class Library extends AppCompatActivity
{
    public static final String ARTICLE_DIRECTORY = "articles";

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        getSupportFragmentManager().beginTransaction().replace(R.id.activity, new Fragment()).commit();
    }
}
