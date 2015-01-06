package com.dexetra.palettesample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.InputStream;


public class MainActivity extends ActionBarActivity {

    private final int SELECT_PHOTO = 1001;

    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        setListeners();
        refreshPalettes();
    }

    private void refreshPalettes() {
        setSupportProgressBarIndeterminateVisibility(true);
        final TextView textView = (TextView) findViewById(R.id.text);
        BitmapDrawable bitmapDrawable = (BitmapDrawable) mImageView.getDrawable();
        Palette.generateAsync(bitmapDrawable.getBitmap(), new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette palette) {
                // Do something with colors...
                textView.setText(getColouredSequence(palette));
                int count=0;
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.swatches);
                linearLayout.removeAllViews();
                for (Palette.Swatch swatch:palette.getSwatches()) {
                    TextView textView = new TextView(MainActivity.this);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 48));
                    textView.setBackgroundColor(swatch.getRgb());
                    textView.setTextColor(swatch.getTitleTextColor());
                    textView.setHintTextColor(swatch.getBodyTextColor());
                    textView.setTag(swatch.getPopulation());
                    textView.setHint("swatch : "+count++);
                    linearLayout.addView(textView);
                }
                setSupportProgressBarIndeterminateVisibility(false);
            }
        });
    }

    private void setListeners() {
        mImageView = (ImageView) findViewById(R.id.image);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        mImageView.setImageBitmap(selectedImage);
                        refreshPalettes();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
        }
    }

    private CharSequence getColouredSequence(Palette palette) {
        SpannableStringBuilder b = new SpannableStringBuilder();
        b.append(addColorSequence(palette.getVibrantColor(-1),"Vibrant"));
        b.append(addColorSequence(palette.getDarkVibrantColor(-1),"Dark Vibrant"));
        b.append(addColorSequence(palette.getLightVibrantColor(-1),"Light Vibrant"));
        b.append(addColorSequence(palette.getMutedColor(-1),"Muted"));
        b.append(addColorSequence(palette.getDarkMutedColor(-1),"Dark Muted"));
        b.append(addColorSequence(palette.getLightMutedColor(-1),"Light Muted"));
        return b;
    }

    private CharSequence addColorSequence(int color,String colorName){
        SpannableStringBuilder b = new SpannableStringBuilder();
        int index = b.length();
        if(color!=-1) {
            b.append("    ");
            b.setSpan(new BackgroundColorSpan(color),index,b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            b.append(" - ");
            b.append(colorName);
            b.append(" \n");
        }
        return b;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
