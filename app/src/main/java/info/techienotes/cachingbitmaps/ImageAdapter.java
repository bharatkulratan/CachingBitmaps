package info.techienotes.cachingbitmaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by Bharat Kul Ratan
 */
public class ImageAdapter extends BaseAdapter{
    private String TAG = getClass().getSimpleName();
    Context mContext;
    ArrayList<Uri> imageList;

    private LruCache<String, Bitmap> mLruCache;

    public ImageAdapter (Context context){
        mContext = context;

        //Find out maximum memory available to application
        //1024 is used because LruCache constructor takes int in kilobytes
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/4th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 4;
        Log.d(TAG, "max memory " + maxMemory + " cache size " + cacheSize);

        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes
                return bitmap.getByteCount() / 1024;
            }
        };

        imageList  = new ArrayList<Uri>();
        //Change this directory to where the images are stored
        String imagesFolderPath = Environment.getExternalStorageDirectory().getPath()+"/backups/";

        File imageSrcDir = new File (imagesFolderPath);
        // if directory not present, build it
        if (!imageSrcDir.exists()){
            imageSrcDir.mkdirs();
        }

        ArrayList<File> imagesInDir = getImagesFromDirectory(imageSrcDir);

        for (File file: imagesInDir){
            // imageList will hold Uri of all images
            imageList.add(Uri.fromFile(file));
        }
    }

    @Override
    public int getCount() {
        return imageList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        Bitmap thumbnailImage = null;
        if (convertView == null){
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(
                    new GridView.LayoutParams(150, 150));   //150,150 is size of imageview
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
        else {
            imageView = (ImageView)convertView;
        }

        // Use the path as the key to LruCache
        final String imageKey = imageList.get(position).toString();

        //thumbnailImage is fetched from LRU cache
        thumbnailImage = getBitmapFromMemCache(imageKey);

        if (thumbnailImage == null){
            // if asked thumbnail is not present it will be put into cache
            BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            task.execute(imageKey);
        }

        imageView.setImageBitmap(thumbnailImage);
        return imageView;
    }

    private ArrayList<File> getImagesFromDirectory (File parentDirPath){
        ArrayList <File> listOfImages =  new ArrayList<File>();
        File [] fileArray = null;

        if ( parentDirPath.isDirectory() ){//parentDirPath.exists() &&
            //    &&
             //   parentDirPath.canRead()){
            fileArray = parentDirPath.listFiles();
        }

        if (fileArray == null){
            return listOfImages;    // return empty list
        }

        for (File file: fileArray){
            if (file.isDirectory()){
                listOfImages.addAll(getImagesFromDirectory(file));
            }
            else {
                // Only JPEG and PNG formats are included
                // for sake of simplicity
                if (file.getName().endsWith("png") ||
                        file.getName().endsWith("jpg")){
                    listOfImages.add(file);
                }
            }
        }
        return listOfImages;
    }

    /**
     *  This function will return the scaled version of original image.
     *  Loading original images into thumbnail is wastage of computation
     *  and hence we will take put scaled version.
     */
    private Bitmap getScaledImage (String imagePath){
        Bitmap bitmap = null;
        Uri imageUri = Uri.parse (imagePath);
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();

            /**
             * inSampleSize flag if set to a value > 1,
             * requests the decoder to sub-sample the original image,
             * returning a smaller image to save memory.
             * This is a much faster operation as decoder just reads
             * every n-th pixel from given image, and thus
             * providing a smaller scaled image.
             * 'n' is the value set in inSampleSize
             * which would be a power of 2 which is downside
             * of this technique.
             */
            options.inSampleSize = 4;

            options.inScaled = true;

            InputStream inputStream = mContext.getContentResolver().openInputStream(imageUri);

            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mLruCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mLruCache.get(key);
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            final Bitmap bitmap = getScaledImage(params[0]);
            addBitmapToMemoryCache(String.valueOf(params[0]), bitmap);
            return bitmap;
        }

        //  onPostExecute() sets the bitmap fetched by doInBackground();
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = (ImageView)imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}
