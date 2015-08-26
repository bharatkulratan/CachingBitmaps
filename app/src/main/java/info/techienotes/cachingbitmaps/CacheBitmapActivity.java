package info.techienotes.cachingbitmaps;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;

public class CacheBitmapActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache_bitmap);

        // Images will be displayed in GridView
        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setAdapter(new ImageAdapter(this));
    }
}
