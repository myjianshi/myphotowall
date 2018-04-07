package cn.edu.gyc.myphotowalltest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;

public class MainActivity extends AppCompatActivity {

    GridView gridView;
    PhotoWallAdapter adapter;
    private int mImageThumbSize;
    private int mImageThumbSpacing;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        gridView = (GridView) findViewById(R.id.wallgridview);

        adapter=new PhotoWallAdapter(this,Images.imageThumbUrls,gridView);
        gridView.setAdapter(adapter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // adapter.cancelAllTasks();
    }
}
