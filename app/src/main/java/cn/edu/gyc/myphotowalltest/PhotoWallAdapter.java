package cn.edu.gyc.myphotowalltest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Administrator on 2018/4/6.
 */

public class PhotoWallAdapter extends BaseAdapter implements OnScrollListener{
    private Set<BitmapTask> bitmapTaskSet;
    private LruCache<String,Bitmap> bitmapLruCache;
    private DiskLruCache diskLruCache;
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

        try{
            File cacheDir=getDiskCacheDir(context,"imgs");
            if(!cacheDir.exists()){
                cacheDir.mkdirs();
            }
            diskLruCache=DiskLruCache.open(cacheDir,1,1,50*1024*1024);
        }catch (Exception e){
            e.printStackTrace();
        }
        gridView.setOnScrollListener(this);
    }

    public File getDiskCacheDir(Context context, String thumb) {
        String cachePath="";
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()){
            cachePath=context.getExternalCacheDir().getPath();
        }else {
            cachePath=context.getCacheDir().getPath();
        }
        return new File(cachePath+File.pathSeparator+thumb);
    }
    /**
     * 使用MD5算法对传入的key进行加密并返回。
     */
    public String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    /**
     * 将缓存记录同步到journal文件中。
     */
    public void fluchCache() {
        if (diskLruCache != null) {
            try {
                diskLruCache.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
    class BitmapTask extends AsyncTask<String,Void,Bitmap>{

        private String imageUrl;

        @Override
      /*  protected Bitmap doInBackground(String... strings) {
            imageUrl=strings[0];
            Bitmap bitmap=downlaodBitmap(imageUrl);
            if(bitmap!=null){
                addBitmapToCache(imageUrl,bitmap);
            }
            return bitmap;
        }
 */


        protected Bitmap doInBackground(String... params) {
            imageUrl = params[0];
            FileDescriptor fileDescriptor = null;
            FileInputStream fileInputStream = null;
            DiskLruCache.Snapshot snapShot = null;
            try {
                // 生成图片URL对应的key
                final String key = hashKeyForDisk(imageUrl);
                // 查找key对应的缓存
                snapShot = diskLruCache.get(key);
                if (snapShot == null) {
                    // 如果没有找到对应的缓存，则准备从网络上请求数据，并写入缓存
                    DiskLruCache.Editor editor = diskLruCache.edit(key);
                    if (editor != null) {
                        OutputStream outputStream = editor.newOutputStream(0);
                        if (downloadUrlToStream(imageUrl, outputStream)) {
                            editor.commit();
                        } else {
                            editor.abort();
                        }
                    }
                    // 缓存被写入后，再次查找key对应的缓存
                    snapShot = diskLruCache.get(key);
                }
                if (snapShot != null) {
                    fileInputStream = (FileInputStream) snapShot.getInputStream(0);
                    fileDescriptor = fileInputStream.getFD();
                }
                // 将缓存数据解析成Bitmap对象
                Bitmap bitmap = null;
                if (fileDescriptor != null) {
                 //   bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    bitmap=BitmapHelper.decodeSampledBitmapFromFileDescriptor(fileDescriptor,120,120);
                }
                if (bitmap != null) {
                    // 将Bitmap对象添加到内存缓存当中
                    addBitmapToMemoryCache(params[0], bitmap);
                }
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fileDescriptor == null && fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
            return null;
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

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (bitmapLruCache.get(key) == null) {
            bitmapLruCache.put(key, bitmap);
        }
    }


    private boolean downloadUrlToStream(String imageUrl, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(imageUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return false;
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
