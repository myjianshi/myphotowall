package cn.edu.gyc.myphotowalltest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Administrator on 2018/4/6.
 */

public class PhotoWallAdapter extends BaseAdapter implements OnScrollListener{
    private Set<BitmapTask> bitmapTaskSet;
    private LruCache<String,Bitmap> bitmapLruCache;
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

        bitmapTaskSet=new HashSet<>();
        int maxMemory=(int)Runtime.getRuntime().maxMemory();
        int cacheSize=maxMemory/8;
       /* bitmapLruCache=new LruCache<>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }

        };
        */
        bitmapLruCache=new LruCache<>(cacheSize);
        gridView.setOnScrollListener(this);
    }

    class BitmapTask extends AsyncTask<String,Void,Bitmap>{

        private String imageUrl;

        @Override
        protected Bitmap doInBackground(String... strings) {
            imageUrl=strings[0];
            Bitmap bitmap=downlaodBitmap(imageUrl);
            if(bitmap!=null){
                addBitmapToCache(imageUrl,bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ImageView imageView=(ImageView)gridView.findViewWithTag(imageUrl);
            if(imageView!=null&&bitmap!=null){
                imageView.setImageBitmap(bitmap);
            }
            bitmapTaskSet.remove(this);
        }

        private Bitmap downlaodBitmap(String urls){
            Bitmap bitmap=null;
            HttpURLConnection connection=null;
            try{
                URL url =new URL(urls);
                connection=(HttpURLConnection)url.openConnection();
                connection.setConnectTimeout(5*1000);
                connection.setReadTimeout(10*1000);
                bitmap= BitmapFactory.decodeStream(connection.getInputStream());
            }catch (Exception e){
                e.printStackTrace();
            }
            finally {
                if(connection!=null)
                    connection.disconnect();
            }
            return bitmap;
        }
    }

    private void addBitmapToCache(String imageUrl, Bitmap bitmap) {
        if(getBitmapFormCache(imageUrl)==null){
            bitmapLruCache.put(imageUrl,bitmap);
        }
    }

    private Bitmap  getBitmapFormCache(String imageUrl) {
        return bitmapLruCache.get(imageUrl);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            if(scrollState==SCROLL_STATE_IDLE){
                loadBitmaps(firstVisibleImageIndex,visibleImageCount);
            }else {
                cancelAllTasks();
            }
    }

    public void cancelAllTasks() {
        if(bitmapTaskSet!=null){
            for(BitmapTask task:bitmapTaskSet){
                task.cancel(false);
            }
        }
    }

    private void loadBitmaps(int firstVisibleImageIndex, int visibleImageCount) {
        try{
            for(int i=firstVisibleImageIndex;i<firstVisibleImageIndex+visibleImageCount;i++){
                String imgUrl=Images.imageThumbUrls[i];
                Bitmap bitmap=getBitmapFormCache(imgUrl);
                if(bitmap==null){
                    BitmapTask task=new BitmapTask();
                    bitmapTaskSet.add(task);
                    task.execute(imgUrl);
                }else {
                    ImageView imageView=(ImageView)gridView.findViewWithTag(imgUrl);
                    if(imageView!=null&&bitmap!=null){
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleImageIndex, int visibleImageCount, int totalImageCount) {
        this.firstVisibleImageIndex=firstVisibleImageIndex;
        this.visibleImageCount=visibleImageCount;

        if(isFirstEnter&&visibleImageCount>0){
            loadBitmaps(firstVisibleImageIndex,visibleImageCount);
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

        imageView.setTag(url);
        Bitmap bitmap=getBitmapFormCache(url);
        if(bitmap!=null){
            imageView.setImageBitmap(bitmap);
        }
        return convertView;
    }
}
