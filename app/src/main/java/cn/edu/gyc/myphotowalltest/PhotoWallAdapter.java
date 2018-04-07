package cn.edu.gyc.myphotowalltest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

/**
 * Created by Administrator on 2018/4/6.
 */

public class PhotoWallAdapter extends BaseAdapter implements OnScrollListener{

    private final Context context;
    private final String[] imgUrls;
    private GridView gridView;
    private int firstVisibleImageIndex;
    private int visibleImageCount;
    private boolean isFirstEnter =true;

    public PhotoWallAdapter(Context context,String[] strings,
                            GridView gridView) {
        this.context = context;
        this.imgUrls = strings;
        this.gridView = gridView;


        gridView.setOnScrollListener(this);
    }












    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            if(scrollState==SCROLL_STATE_IDLE){
              //  loadBitmaps(firstVisibleImageIndex,visibleImageCount);
            //   loadImageView(firstVisibleImageIndex,visibleImageCount);
            }else {
              //  cancelAllTasks();
            }
    }


    @Override
    public void onScroll(AbsListView absListView, int firstVisibleImageIndex, int visibleImageCount, int totalImageCount) {
        this.firstVisibleImageIndex=firstVisibleImageIndex;
        this.visibleImageCount=visibleImageCount;


        if(isFirstEnter&&visibleImageCount>0){
           // loadBitmaps(firstVisibleImageIndex,visibleImageCount);
            //loadImageView(firstVisibleImageIndex,visibleImageCount);
            isFirstEnter=false;
        }

    }

    @Override
    public int getCount() {
        return imgUrls.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        String url=imgUrls[position];
        if(convertView==null){
            convertView= LayoutInflater.from(context).inflate(R.layout.item,null);
        }

        ImageView imageView=(ImageView)convertView.findViewById(R.id.photo);

       Glide.with(context).load(url).into(imageView);

        return convertView;
    }
}
