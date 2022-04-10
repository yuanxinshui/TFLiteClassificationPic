package com.example.myopencvdemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    Button loadButton;
    Button detectFaceButton;

    ImageView imageView1;
    ImageView imageView2;
    String imagePathLoaded;
    Mat matrix;
    Bitmap bitmap;


    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        OpenCVLoader.initDebug();
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},200);
       loadButton=findViewById(R.id.loadButton);
       detectFaceButton=findViewById(R.id.processButton);
       imageView1=findViewById(R.id.imageView1);
       imageView2=findViewById(R.id.imageView2);

       loadButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               Intent intent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
               //打开一个带有返回值的交互界面
               startActivityForResult(intent,300);
           }
       });

       detectFaceButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
              detectFace();
           }
       });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == 300){
            if(resultCode == RESULT_OK){
                Uri uri=data.getData();
                imageView1.setImageURI(uri);
                imagePathLoaded=getPathFromURI(MainActivity.this,uri);

                try {
                    FileInputStream fis = new FileInputStream(imagePathLoaded);
                    bitmap=BitmapFactory.decodeStream(fis);
                    imageView1.setImageBitmap(bitmap);

                } catch (Exception e) {
                    e.printStackTrace();
                }
//                matrix= Imgcodecs.imread(imagePathLoaded);
            }
        }
    }
    // 根据相册的Uri获取图片的路径
    public static String getPathFromURI(Context context, Uri uri) {
        String result;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    public void detectFace(){
        if(bitmap == null)
        {
            return ;
        }
        Bitmap bit=bitmap.copy(Bitmap.Config.ARGB_8888,false);
        Mat src=new Mat(bit.getHeight(),bit.getWidth(), CvType.CV_8UC(3));

        Utils.bitmapToMat(bit,src);
        Mat dst=src.clone();
        Imgproc.cvtColor(src,dst, Imgproc.COLOR_BGR2GRAY);
        Mat matrix =dst.clone();

        CascadeClassifier cascadeClassifier=new CascadeClassifier();

        try {
            //Copy the resource into a temp file so OpenCV can load it
            InputStream is=this.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir=getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir,"lbpcascade_frontalface.xml");
            FileOutputStream os=new FileOutputStream((mCascadeFile));
            byte[] buffer=new byte[4096];
            int bytesRead;
            while((bytesRead = is.read(buffer))!=-1){
                os.write(buffer,0,bytesRead);
            }
            is.close();
            os.close();

            //Load the cascade classifer
            cascadeClassifier=new CascadeClassifier(mCascadeFile.getAbsolutePath());

        }catch (Exception e){
            Log.e("OpenCVActivity","Error loading casce",e);
        }

        MatOfRect faceArray = new MatOfRect();
        cascadeClassifier.detectMultiScale(matrix,faceArray);

        int numFaces=faceArray.toArray().length;
        for(Rect face:faceArray.toArray()){
            Imgproc.rectangle(src,new Point(face.x,face.y),new Point(face.x+face.width,face.y+face.height),new Scalar(0,0,255),3);
        }

        Mat finalMatrix = src.clone();
        Bitmap bitmap =Bitmap.createBitmap(finalMatrix.cols(),finalMatrix.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(finalMatrix,bitmap);
        imageView2.setImageBitmap(bitmap);

        Toast.makeText(getApplicationContext(),numFaces+ " faces found!",Toast.LENGTH_SHORT).show();
    }



    @Override
    protected void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            Log.i("cv","未收到内置OpenCV库，使用OpenCV Manager进行初始化。");

//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }else
        {
            Log.i("cv","发现了内置OpenCV库，使用OpenCV 库进行初始化。");
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
