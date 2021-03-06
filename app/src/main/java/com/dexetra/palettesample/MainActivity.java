package com.dexetra.palettesample;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static android.widget.LinearLayout.LayoutParams;
import static com.dexetra.palettesample.ScalingUtilities.ScalingLogic;


public class MainActivity extends ActionBarActivity {

    private final int SELECT_PHOTO = 1001;
    int padding;

    ImageView mImageView;
    LayoutParams titleParams, bodyParams;
    int mDefaultHeight;
    private int mTotalPixels;
    private Menu mMenu;

    Comparator<Object> mComparator = new Comparator<Object>() {
        @Override
        public int compare(Object lhs, Object rhs) {
            Palette.Swatch l = (Palette.Swatch)lhs;
            Palette.Swatch r = (Palette.Swatch)rhs;
            if(l.getPopulation()<r.getPopulation())
                return 1;
            else if(l.getPopulation()==r.getPopulation())
                return 0;
            else
                return -1;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, displayMetrics);
        mDefaultHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170,
                displayMetrics);

        setListeners();
        refreshPalettes(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            final Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    setSupportProgressBarIndeterminateVisibility(true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (mImageView != null) {
                                try {
                                    final int dstHeight = mImageView.getHeight();
                                    int dstWidth = mImageView.getWidth();

                                    final Uri imageUri = imageReturnedIntent.getData();

                                    // Part 1: Decode image
                                    Bitmap unscaledBitmap = ScalingUtilities.decodeStream(
                                            MainActivity.this, imageUri, dstWidth, dstHeight,
                                            ScalingLogic.FIT);

                                    if (unscaledBitmap != null) {
                                        // Part 2: Scale image
                                        int reqH = unscaledBitmap.getHeight() * dstWidth /
                                                unscaledBitmap.getWidth();
                                        final Bitmap scaledBitmap =
                                                ScalingUtilities.createScaledBitmap(unscaledBitmap,
                                                        dstWidth, reqH, ScalingLogic.FIT);
                                        unscaledBitmap.recycle();


                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (mImageView != null) {
                                                    mImageView.setImageBitmap(scaledBitmap);
                                                    refreshPalettes(scaledBitmap);
                                                    mTotalPixels = scaledBitmap.getHeight() *
                                                            scaledBitmap.getWidth();

                                                    if (mMenu != null) {
                                                        int h = scaledBitmap.getHeight();
                                                        mMenu.findItem(R.id.action_toggle_resize)
                                                             .setVisible(h > mDefaultHeight + 80);
                                                        mImageView.setTag(h);
                                                    }
                                                }
                                            }
                                        });
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();

                }
        }
    }

    public LayoutParams getTitleParams() {
        if (titleParams == null) {
            titleParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            titleParams.topMargin = padding;
        }
        return titleParams;
    }

    public LayoutParams getBodyParams() {
        if (bodyParams == null) {
            bodyParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            bodyParams.bottomMargin = padding;
        }
        return bodyParams;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
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
        } else if (id == R.id.action_toggle_resize) {
            LayoutParams layoutParams = (LayoutParams) mImageView.getLayoutParams();
            if (layoutParams.height == mDefaultHeight)
                layoutParams.height = (int) mImageView.getTag();
            else layoutParams.height = mDefaultHeight;
            mImageView.setLayoutParams(layoutParams);
        }else if(id==R.id.action_wallpaper){
            setSupportProgressBarIndeterminateVisibility(true);
            item.setEnabled(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    WallpaperManager wp = WallpaperManager.getInstance(
                            MainActivity.this.getApplicationContext());
                    WallpaperInfo wi = wp.getWallpaperInfo();
                    if(wi==null) {
                        final BitmapDrawable d = (BitmapDrawable) wp.getDrawable();
                        if (d != null) {
                            Bitmap src = d.getBitmap();
                            if (src != null)
                                calculateFromBitmap(src);
                        }
                    }else{
                        Drawable d = wi.loadThumbnail(getPackageManager());
                        if(d instanceof BitmapDrawable){
                            Bitmap b =((BitmapDrawable)d).getBitmap();
                            if(b!=null)
                                calculateFromBitmap(b);
                        }
                    }
                }
            }).start();
        }

        return super.onOptionsItemSelected(item);
    }

    private void calculateFromBitmap(final Bitmap src) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mImageView != null) {
                    mImageView.setImageBitmap(src);
                    refreshPalettes(src);
                    mTotalPixels = src.getHeight() *
                            src.getWidth();

                    if (mMenu != null) {
                        int h = src.getHeight();
                        mMenu.findItem(R.id.action_toggle_resize)
                             .setVisible(h > mDefaultHeight + 80);
                        mImageView.setTag(h);
                    }
                }
                if(mMenu!=null)
                    mMenu.findItem(R.id.action_wallpaper).setEnabled(true);
                setSupportProgressBarIndeterminateVisibility(false);
            }
        });
    }

    private void refreshPalettes(Bitmap scaledBitmap) {
        setSupportProgressBarIndeterminateVisibility(true);
        final TextView textView = (TextView) findViewById(R.id.text);
        if (scaledBitmap == null) {
            scaledBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
            mTotalPixels = scaledBitmap.getHeight() * scaledBitmap.getWidth();
        }

        Palette.generateAsync(scaledBitmap, new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette palette) {
                // Do something with colors...
                List<Palette.Swatch> swatches = palette.getSwatches();
                Object[] s = swatches.toArray();
                Arrays.sort(s,mComparator);
                textView.setText(getColouredSequence(palette));
                int count = 0;
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.swatches);
                linearLayout.removeAllViews();
                for (Object swat : s) {
                    Palette.Swatch swatch = (Palette.Swatch) swat;
                    TextView titleTextView = null;
                    try {
                        titleTextView = new TextView(MainActivity.this);
                        titleTextView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                LayoutParams.WRAP_CONTENT));
                        titleTextView.setBackgroundColor(swatch.getRgb());
                        titleTextView.setTextColor(swatch.getTitleTextColor());
                        titleTextView.setPadding(padding, padding, padding, 0);
                        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                        titleTextView.setLayoutParams(getTitleParams());
                        titleTextView.setText(addColorSequenceForPalette(swatch,null));
                        titleTextView.append("\nTitle : " + Integer.toHexString(
                                swatch.getTitleTextColor()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        TextView bodyTextView = new TextView(MainActivity.this);
                        bodyTextView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                LayoutParams.WRAP_CONTENT));
                        bodyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                        bodyTextView.setLayoutParams(getBodyParams());
                        bodyTextView.setPadding(padding, 0, padding, padding);
                        bodyTextView.setBackgroundColor(swatch.getRgb());
                        bodyTextView.setTextColor(swatch.getBodyTextColor());
                        bodyTextView.setText("Body : " + Integer.toHexString(
                                swatch.getBodyTextColor()) + "\n");
                        bodyTextView.append(getString(R.string.placeholder));

                        linearLayout.addView(titleTextView);
                        linearLayout.addView(bodyTextView);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                setSupportProgressBarIndeterminateVisibility(false);
            }
        });
    }

    private String getPercentageString(int population) {
        return population+"("+String.format("%.3f", (population * 100.0f) / mTotalPixels) + "%)";
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

    private CharSequence getColouredSequence(Palette palette) {
        SpannableStringBuilder b = new SpannableStringBuilder();
        b.append(addColorSequenceForPalette(palette.getVibrantSwatch(), "Vibrant"));
        b.append(addColorSequenceForPalette(palette.getDarkVibrantSwatch(), "Dark Vibrant"));
        b.append(addColorSequenceForPalette(palette.getLightVibrantSwatch(), "Light Vibrant"));
        b.append(addColorSequenceForPalette(palette.getMutedSwatch(), "Muted"));
        b.append(addColorSequenceForPalette(palette.getDarkMutedSwatch(), "Dark Muted"));
        b.append(addColorSequenceForPalette(palette.getLightMutedSwatch(), "Light Muted"));
        return b;
    }

    private CharSequence addColorSequenceForPalette(Palette.Swatch swatch, String colorName) {
        SpannableStringBuilder b = new SpannableStringBuilder();
        int index = b.length();
        if (swatch != null) {
            int color = swatch.getRgb();
            b.append("    ");
            b.setSpan(new BackgroundColorSpan(color), index, b.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            b.append(" - ");
            b.append(Integer.toHexString(color));
            if(colorName!=null){
                b.append(" - ");
                b.append(colorName);
                b.append(" -");
            }
            b.append("\n");
            b.append(getPercentageString(swatch.getPopulation()));
            b.append(" - YIQ ");
            b.append(Boolean.toString(isYIQCompatible(color)));
            b.append(" - 50/50 ");
            b.append(Boolean.toString(is5050Compatible(color)));
            if(colorName!=null)
                b.append(" \n\n");
        }
        return b;
    }

    private boolean isYIQCompatible(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.green(color);

        int yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
        return (yiq < 128);
    }
    private boolean is5050Compatible(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.green(color);
        return r + g + b < 740;
    }
}
