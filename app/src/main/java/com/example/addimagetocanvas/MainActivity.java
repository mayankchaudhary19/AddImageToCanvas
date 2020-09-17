package com.example.addimagetocanvas;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    LinearLayout addImage,change_image,editLinearLayout,linearLayout;
    ImageView imageView, removeImage;

    // to get image uri
    private static final int PICK_IMAGE = 100;
    Uri imageUri;

    // matrices to move and zoom image
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    // states - DRAG,ZOOM
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    // for zooming
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;
    // to rotate
    private float d = 0f;
    private float newRot = 0f;
    private float[] lastEvent = null;

    // to bound the image within thw layout when dragging
    private float dx; // postTranslate X distance
    private float dy; // postTranslate Y distance
    private float[] matrixValues = new float[9];
    float matrixX = 0; // X coordinate of matrix inside the ImageView
    float matrixY = 0; // Y coordinate of matrix inside the ImageView
    float width = 0; // width of drawable
    float height = 0; // height of drawable
    Bitmap borderImage, selectedImage;

    public void init(){
        linearLayout=findViewById(R.id.linearLayout);
        addImage = findViewById(R.id.add_image);
        imageView = findViewById(R.id.added_image);
        removeImage= findViewById(R.id.removeImage);
        editLinearLayout=findViewById(R.id.editLinearLayout);
        change_image= findViewById(R.id.change_image);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        getSupportActionBar().hide();
        // initially remove image button is not visible
        removeImage.setVisibility(View.GONE);
        editLinearLayout.setVisibility(View.GONE);
        linearLayout.setVisibility(View.VISIBLE);
        // add image button to get image from gallery in the parent layout
        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });
        // change image button to change image from gallery when an image is already present in the parent layout
        change_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });
        // set touch listener on image
        imageView.setOnTouchListener(this);

        // to remove image from the parent background
        removeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageView.setImageResource(0);
                removeImage.setVisibility(View.GONE);
                editLinearLayout.setVisibility(View.GONE);
                linearLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    // intent to open Gallery to select the image
    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    // checking the result(data) from the intent and setting that to imageView
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE){
            try {
                imageUri = data.getData();
                // changing the Uri to input stream to resize the selected image with same aspect ratio of image
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                // converting the input stream to Bitmap
                selectedImage = BitmapFactory.decodeStream(imageStream);
                selectedImage = getResizedBitmap(selectedImage,700);
                imageView.setImageBitmap(selectedImage);
                // to add the border when the the selected image is touched
                borderImage = addBlackBorder(selectedImage,2,R.color.colorBorder);

                removeImage.setVisibility(View.VISIBLE);
                editLinearLayout.setVisibility(View.VISIBLE);
                linearLayout.setVisibility(View.GONE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // function to resize the image and maintain the aspect ratio of image
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageView view = (ImageView) v;
        view.setScaleType(ImageView.ScaleType.MATRIX);
        float scale;
        // touch events:
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // when only 1 finger is down
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = rotation(event);
                break;

            case MotionEvent.ACTION_UP: // when first finger is lifted
            case MotionEvent.ACTION_POINTER_UP: // when second finger is lifted
                mode = NONE;                    // setting mode to NONE
                imageView.setImageBitmap(selectedImage);
                break;
            case MotionEvent.ACTION_MOVE:       // when the MOTION_EVENT is MOVE
                imageView.setImageBitmap(borderImage);
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    // to bound the image within the parent layout while dragging
                    matrix.getValues(matrixValues);
                    matrixX = matrixValues[2];
                    matrixY = matrixValues[5];
                    width = matrixValues[0] * (((ImageView) view).getDrawable()
                            .getIntrinsicWidth());
                    height = matrixValues[4] * (((ImageView) view).getDrawable()
                            .getIntrinsicHeight());

                    dx = event.getX() - start.x;
                    dy = event.getY() - start.y;

                    // if image goes outside left bound
                    if (matrixX + dx < 0){
                        dx = -matrixX;
                    }
                    // if image goes outside right bound
                    if(matrixX + dx + width > view.getWidth()){
                        dx = view.getWidth() - matrixX - width;
                    }
                    // if image goes outside top bound
                    if (matrixY + dy < 0){
                        dy = -matrixY;
                    }
                    // if image goes outside bottom bound
                    if(matrixY + dy + height > view.getHeight()){
                        dy = view.getHeight() - matrixY - height;
                    }
                    // setting the translated values to the matrix
                    matrix.postTranslate(dx, dy);

                } else if (mode == ZOOM && event.getPointerCount() == 2) {      // when MODE is ZOOM and 2 fingers are down
                    float newDist = spacing(event);
                    matrix.set(savedMatrix);
                    if (newDist > 10f) {
                        scale = newDist / oldDist;
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                    if (lastEvent != null) {
                        // to rotate the image (2 fingers are down)
                        newRot = rotation(event);
                        float r = newRot - d;
                        matrix.postRotate(r, view.getMeasuredWidth() / 2,
                                view.getMeasuredHeight() / 2);
                    }
                }
                break;

        }
        // to perform the transformation
        view.setImageMatrix(matrix);

        return true; // event handled
    }

    // function to add the border of the image when the MotionEvent is MOVE (onTouch)
    private Bitmap addBlackBorder(Bitmap bmp, int borderSize, int color) {
        Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(color);
        canvas.drawBitmap(bmp, borderSize, borderSize, null);
        return bmpWithBorder;
    }

    // to Rotate the image
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);

        return (float) Math.toDegrees(radians);
    }

    // to calc. the spacing
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);

    }

    // to find the mid point
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x/2, y/2);

    }
}