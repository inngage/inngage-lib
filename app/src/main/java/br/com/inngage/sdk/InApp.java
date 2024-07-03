package br.com.inngage.sdk;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class InApp extends AppCompatActivity {
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inapp);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            ArrayList<String> arrayList = bundle.getStringArrayList("keyInApp");
            boolean containsCarousel = containsCarouselTrue(arrayList);
            boolean containsBackgroundImage = containsBackgroundImage(arrayList, 5);
            if(containsCarousel){
                showInAppDialogRichContent(arrayList);
            } else if(containsBackgroundImage){
                showInAppBackgroundImage(arrayList);
            } else {
                showInAppDialogNormal(arrayList);
            }
        }
    }

    private boolean containsCarouselTrue(ArrayList<String> list) {
        String searchString = "\"carousel\":true";
        for (String item : list) {
            if (item != null && item.contains(searchString)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsBackgroundImage(ArrayList<String> list, int position) {
        if (position >= 0 && position < list.size()) {
            return list.get(position) != null;
        }
        return false;
    }

    private void showInAppDialogRichContent(final ArrayList<String> arrayList){
        Executor executor = Executors.newFixedThreadPool(10);
        executor.execute(() -> {
            handler.post(() -> {
                if (hasValidValues(arrayList)) {
                    renderInAppRichContent(arrayList);
                } else {
                    closedAndClear();
                }
            });
        });
    }

    private void showInAppBackgroundImage(final ArrayList<String> arrayList){
        Executor executor = Executors.newFixedThreadPool(10);
        executor.execute(() -> {
            handler.post(() -> {
                if (hasValidValues(arrayList)) {
                    renderInAppBackgroundImage(arrayList);
                } else {
                    closedAndClear();
                }
            });
        });
    }

    private void showInAppBackgroundAndRichContent(final ArrayList<String> arrayList){
        Executor executor = Executors.newFixedThreadPool(10);
        executor.execute(() -> {
            handler.post(() -> {
                if (hasValidValues(arrayList)) {
                    renderInAppBackgroundAndRichContent(arrayList);
                } else {
                    closedAndClear();
                }
            });
        });
    }

    private void showInAppDialogNormal(final ArrayList<String> arrayList) {
        Executor executor = Executors.newFixedThreadPool(10);
        executor.execute(() -> {
            handler.post(() -> {
                if (hasValidValues(arrayList)) {
                    renderInAppNormal(arrayList);
                } else {
                    closedAndClear();
                }
            });
        });
    }

    private boolean hasValidValues(ArrayList<String> values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private int getColorFromHex(String hex) {
        return Color.parseColor(hex);
    }

    public static int countPositionsExcludingCarousel(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        List<String> keysToExclude = Arrays.asList("carousel", "carouselPosition", "images");

        Iterator<String> keys = jsonObject.keys();
        int count = 0;

        while (keys.hasNext()) {
            String key = keys.next();
            if (!keysToExclude.contains(key)) {
                count++;
            }
        }

        return count;
    }

    private void renderInAppRichContent(ArrayList<String> values) {
        Dialog dialog = createDialog(R.layout.in_app_rich_content);
        dialog.show();

        CardView card = dialog.findViewById(R.id.cardInAppRichContent);

        int imgCount = 0;

        try{
            imgCount = countPositionsExcludingCarousel(values.get(InAppConstants.RICH_CONTENT));
        }catch (JSONException e){
            e.printStackTrace();
        }

        final int[] pendingTasks = {imgCount};

        configureCardDimensions(card);

        if(values.get(InAppConstants.TITLE_IN_APP) != null){
            TextView title = dialog.findViewById(R.id.titleInAppRichContent);
            int titleColor = getColorFromHex(values.get(InAppConstants.TITLE_FONT_COLOR));
            title.setVisibility(View.VISIBLE);
            setTextAndColors(title, values.get(InAppConstants.TITLE_IN_APP), titleColor);
        }
        if(values.get(InAppConstants.BODY_IN_APP) != null){
            TextView body = dialog.findViewById(R.id.bodyInAppRichContent);
            int bodyColor = getColorFromHex(values.get(InAppConstants.BODY_FONT_COLOR));
            body.setVisibility(View.VISIBLE);
            setTextAndColors(body, values.get(InAppConstants.BODY_IN_APP), bodyColor);
        }

        renderSliderImages(dialog, values, () -> {
            pendingTasks[0]--;
            if (pendingTasks[0] == 0) {
                card.setVisibility(View.VISIBLE);
                card.setCardBackgroundColor(getColorFromHex(values.get(InAppConstants.BACKGROUND_COLOR)));
            }
        });

        Button btnRight = dialog.findViewById(R.id.buttonRightRichContent);
        Button btnLeft = dialog.findViewById(R.id.buttonLeftRichContent);

        renderButton(dialog, btnRight, btnLeft, values);
    }

    interface TaskCompletionListener {
        void onTaskCompleted();
    }

    private void renderInAppBackgroundImage(ArrayList<String> values){
        Dialog dialog = createDialog(R.layout.in_app_background_image);
        dialog.show();

        CardView card = dialog.findViewById(R.id.cardInApp);

        setCloseButton(dialog);

        configureCardDimensions(card);

        if(values.get(InAppConstants.TITLE_IN_APP) != null){
            TextView title = dialog.findViewById(R.id.titleInApp);
            int titleColor = getColorFromHex(values.get(InAppConstants.TITLE_FONT_COLOR));
            title.setVisibility(View.VISIBLE);
            setTextAndColors(title, values.get(InAppConstants.TITLE_IN_APP), titleColor);
        }
        if(values.get(InAppConstants.BODY_IN_APP) != null){
            TextView body = dialog.findViewById(R.id.bodyInApp);
            int bodyColor = getColorFromHex(values.get(InAppConstants.BODY_FONT_COLOR));
            body.setVisibility(View.VISIBLE);
            setTextAndColors(body, values.get(InAppConstants.BODY_IN_APP), bodyColor);
        }

        renderBackgroundImage(card, values);

        if(card.getVisibility() == View.VISIBLE){
            card.setCardBackgroundColor(getColorFromHex(values.get(InAppConstants.BACKGROUND_COLOR)));
        }

        Button btnRight = dialog.findViewById(R.id.buttonRight);
        Button btnLeft = dialog.findViewById(R.id.buttonLeft);

        renderButton(dialog, btnRight, btnLeft, values);

        renderPixel(dialog, values);
    }

    private void renderInAppBackgroundAndRichContent(ArrayList<String> values){
        Dialog dialog = createDialog(R.layout.in_app_background_rich_content);
        dialog.show();

        CardView card = dialog.findViewById(R.id.cardInApp);

        setCloseButton(dialog);

        configureCardDimensions(card);

        if(values.get(InAppConstants.TITLE_IN_APP) != null){
            TextView title = dialog.findViewById(R.id.titleInApp);
            int titleColor = getColorFromHex(values.get(InAppConstants.TITLE_FONT_COLOR));
            title.setVisibility(View.VISIBLE);
            setTextAndColors(title, values.get(InAppConstants.TITLE_IN_APP), titleColor);
        }
        if(values.get(InAppConstants.BODY_IN_APP) != null){
            TextView body = dialog.findViewById(R.id.bodyInApp);
            int bodyColor = getColorFromHex(values.get(InAppConstants.BODY_FONT_COLOR));
            body.setVisibility(View.VISIBLE);
            setTextAndColors(body, values.get(InAppConstants.BODY_IN_APP), bodyColor);
        }

//        renderSliderImages(dialog, values, );

        renderBackgroundImage(card, values);

        if(card.getVisibility() == View.VISIBLE){
            card.setCardBackgroundColor(getColorFromHex(values.get(InAppConstants.BACKGROUND_COLOR)));
        }

        Button btnRight = dialog.findViewById(R.id.buttonRight);
        Button btnLeft = dialog.findViewById(R.id.buttonLeft);

        renderButton(dialog, btnRight, btnLeft, values);

        renderPixel(dialog, values);
    }

    private void renderInAppNormal(ArrayList<String> values) {
        Dialog dialog = createDialog(R.layout.in_app_normal);
        dialog.show();

        CardView card = dialog.findViewById(R.id.cardInApp);

        setCloseButton(dialog);

        configureCardDimensions(card);

        if(values.get(InAppConstants.TITLE_IN_APP) != null){
            TextView title = dialog.findViewById(R.id.titleInApp);
            int titleColor = getColorFromHex(values.get(InAppConstants.TITLE_FONT_COLOR));
            title.setVisibility(View.VISIBLE);
            setTextAndColors(title, values.get(InAppConstants.TITLE_IN_APP), titleColor);
        }
        if(values.get(InAppConstants.BODY_IN_APP) != null){
            TextView body = dialog.findViewById(R.id.bodyInApp);
            int bodyColor = getColorFromHex(values.get(InAppConstants.BODY_FONT_COLOR));
            body.setVisibility(View.VISIBLE);
            setTextAndColors(body, values.get(InAppConstants.BODY_IN_APP), bodyColor);
        }

        Button btnRight = dialog.findViewById(R.id.buttonRight);
        Button btnLeft = dialog.findViewById(R.id.buttonLeft);

        renderButton(dialog, btnRight, btnLeft, values);

        renderPixel(dialog, values);

        card.setVisibility(View.VISIBLE);
        card.setCardBackgroundColor(getColorFromHex(values.get(InAppConstants.BACKGROUND_COLOR)));
    }

    private Dialog createDialog(int xml) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(dialogInterface -> {
            closedAndClear();
        });
        dialog.setContentView(xml);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    private void configureCardDimensions(CardView card){
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        int minWidth = (int) (screenWidth * 0.8);
        int minHeight = minWidth / 2;

        card.setMinimumWidth(minWidth);
        card.setMinimumHeight(minHeight);

        Log.d("INAPP", "height: " + card.getLayoutParams().height + "screen height: " + screenHeight + "get height: " + card.getHeight());
    }

    private void renderSliderImages(Dialog dialog, ArrayList<String> values, TaskCompletionListener listener) {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        //int screenHeight = displayMetrics.heightPixels;

        int minWidth = (int) (screenWidth * 0.9);

        ImageSlider imageSlider = dialog.findViewById(R.id.imageSliderInApp);
        ViewGroup.LayoutParams layoutParams = imageSlider.getLayoutParams();

        layoutParams.width = minWidth;

        if (values.get(InAppConstants.RICH_CONTENT) != null){
            try {
                JSONObject jsonObject = new JSONObject(values.get(InAppConstants.RICH_CONTENT));
                boolean isCarousel = jsonObject.optBoolean("carousel");

                if (isCarousel) {
                    ArrayList<SlideModel> imageList = createSlideModelsFromJSON(jsonObject, imageSlider, listener);
                    configureImageSlider(imageSlider, imageList);
                } else {
                    imageSlider.setVisibility(View.GONE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                imageSlider.setVisibility(View.GONE);
            }
        } else {
            imageSlider.setVisibility(View.GONE);
        }
    }

    private ArrayList<SlideModel> createSlideModelsFromJSON(JSONObject jsonObject, ImageSlider imageSlider, TaskCompletionListener listener) {
        ArrayList<SlideModel> imageList = new ArrayList<>();
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;

        for (int i = 1; i <= 5; i++) {
            String imageUrl = jsonObject.optString("img" + i, "");
            if (!imageUrl.isEmpty()) {
                new GetImageDimensionsTask(imageUrl, (width, height) -> {
                    Log.i("inapp", width + " width; " + height + " heigth;");

                    int maxHeightPx = 1200;
                    if (height > maxHeightPx) {
                        imageSlider.getLayoutParams().height = maxHeightPx;
                    } else {
                        imageSlider.getLayoutParams().height = 1200;
                    }

                    imageSlider.requestLayout();

                    imageList.add(new SlideModel(imageUrl, ScaleTypes.CENTER_INSIDE));

                    imageSlider.setImageList(imageList);
                    imageSlider.setVisibility(View.VISIBLE);

                    listener.onTaskCompleted();
                }).execute();
            }
        }

        return imageList;
    }

    public interface ImageDimensionsListener {
        void onImageDimensionsReceived(int width, int height);
    }

    private static class GetImageDimensionsTask extends AsyncTask<String, Void, Bitmap> {
        private String url;
        private ImageDimensionsListener listener;

        public GetImageDimensionsTask(String url, ImageDimensionsListener listener) {
            this.url = url;
            this.listener = listener;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                InputStream inputStream = new URL(url).openStream();
                return BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && listener != null) {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                listener.onImageDimensionsReceived(width, height);
            } else {
                System.out.println("Não foi possível ler a imagem.");
            }
        }
    }

    private void configureImageSlider(ImageSlider imageSlider, ArrayList<SlideModel> imageList) {
        if (!imageList.isEmpty()) {
            imageSlider.setImageList(imageList);
        } else {
            imageSlider.setVisibility(View.GONE);
        }
    }

    private void setCloseButton(Dialog dialog){
//        ImageButton btnClose = dialog.findViewById(R.id.closeButton);
//
//        btnClose.setOnClickListener(view -> {
//            closedAndClear();
//        });
    }

    private void renderButton(Dialog dialog, Button btnRight, Button btnLeft, ArrayList<String> values) {
        renderButtonWithTextAndColor(
                btnLeft,
                values.get(InAppConstants.BTN_LEFT_TXT),
                values.get(InAppConstants.BTN_LEFT_TXT_COLOR),
                values.get(InAppConstants.BTN_LEFT_BG_COLOR));
        renderButtonWithTextAndColor(
                btnRight,
                values.get(InAppConstants.BTN_RIGHT_TXT),
                values.get(InAppConstants.BTN_RIGHT_TXT_COLOR),
                values.get(InAppConstants.BTN_RIGHT_BG_COLOR));
        setButtonClickListener(
                btnLeft,
                values.get(InAppConstants.BTN_LEFT_ACTION_TYPE),
                values.get(InAppConstants.BTN_LEFT_ACTION_LINK),
                "left");
        setButtonClickListener(
                btnRight,
                values.get(InAppConstants.BTN_RIGHT_ACTION_TYPE),
                values.get(InAppConstants.BTN_RIGHT_ACTION_LINK),
                "right");
    }

    private void setRoundedButton(Button button, String bgColorHex) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(8);
        shape.setColor(getColorFromHex(bgColorHex));
        button.setBackground(shape);
    }

    private void setButtonClickListener(Button button, String actionType, String actionValue, String typeButton) {
        button.setOnClickListener(view -> {
            if (actionType != null && actionType.contains("deep")) {
                if (actionValue != null || !actionValue.isEmpty()){
                    InngageUtils.deep(this, actionValue);
                }
                else
                    closedAndClear();
            } else if (actionType != null && actionType.contains("inapp")) {
                if (actionValue != null || !actionValue.isEmpty())
                    InngageUtils.web(actionValue, this);
                else
                    closedAndClear();
            }

//            if(typeButton == "left")
//                inAppClickListener.onButtonLeftClick();
//            if(typeButton == "right")
//                inAppClickListener.onButtonRightClick();
            closedAndClear();
        });
    }

    private void setCardButtonClickListener(CardView card, String actionType, String actionValue){
        card.setOnClickListener(view -> {
            if (actionType != null && actionType.contains("deep")) {
                if (actionValue != null && !actionValue.isEmpty())
                    InngageUtils.deep(this, actionValue);
                else
                    closedAndClear();
            } else if (actionType != null && actionType.contains("inapp")) {
                if (actionValue != null && !actionValue.isEmpty())
                    InngageUtils.web(actionValue, this);
                else
                    closedAndClear();
            }
            closedAndClear();
        });
    }

    private void renderButtonWithTextAndColor(
            Button button,
            String text,
            String textColorHex,
            String bgColorHex) {
        if (text != null && !text.isEmpty()) {
            setRoundedButton(button, bgColorHex);
            button.setTextColor(getColorFromHex(textColorHex));
            button.setText(text);
            setButtonWidthToMatchParent(button);
        } else {
            button.setVisibility(View.GONE);
        }
    }

    private void renderPixel(Dialog dialog, ArrayList<String> values){
        ImageView pixel = dialog.findViewById(R.id.pixel);
        Glide.with(this)
                .load(values.get(InAppConstants.IMPRESSION))
                .into(pixel);
    }

    private void renderBackgroundImage(CardView card, ArrayList<String> values){
        if (values.get(InAppConstants.BACKGROUND_IMAGE) != null
                && !values.get(InAppConstants.BACKGROUND_IMAGE).isEmpty()) {
            card.setVisibility(View.VISIBLE);
            backgroundImage(card, values);
            setCardButtonClickListener(
                    card,
                    values.get(InAppConstants.BACKGROUND_IMG_ACTION_TYPE),
                    values.get(InAppConstants.BACKGROUND_IMG_ACTION_LINK));
        } else {
            card.setVisibility(View.VISIBLE);
        }
    }

    private void backgroundImage(CardView card, ArrayList<String> values){
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        float cornerRadius = 8f;

        Glide.with(this)
                .asBitmap()
                .load(values.get(InAppConstants.BACKGROUND_IMAGE))
                .apply(new RequestOptions().override(Target.SIZE_ORIGINAL))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        int largura = resource.getWidth();
                        int altura = resource.getHeight();

                        ViewGroup.LayoutParams layoutParams = card.getLayoutParams();

                        Bitmap roundedImage = addRoundedCornersToBitmap(resource, cornerRadius);
                        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), roundedImage);

                        layoutParams.height = altura;
                        layoutParams.width = (int) (screenWidth * 0.9);

                        card.post(() -> {
                            int maxHeightPx = (int) (screenHeight * 0.7); // Altura máxima em pixels
                            if (altura > maxHeightPx) {
                                layoutParams.height = maxHeightPx;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    resource.setHeight(maxHeightPx);
                                }
                                card.setLayoutParams(layoutParams);
                            }
                        });

                        card.setBackground(bitmapDrawable);
                        card.setVisibility(View.VISIBLE);

                        Log.d("IN-APP", "Largura: " + String.valueOf(largura) + "; Altura: " + String.valueOf(altura));
                        Log.d("CARD", "Largura: " + String.valueOf(card.getWidth()) + "; Altura: " + String.valueOf(card.getHeight()));
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }
    private Bitmap addRoundedCornersToBitmap(Bitmap bitmap, float cornerRadius) {
        Bitmap roundedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(roundedBitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), cornerRadius, cornerRadius, paint);
        }
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        canvas.drawBitmap(bitmap, 0, 0, paint);

        return roundedBitmap;
    }

    private void setButtonWidthToMatchParent(Button button) {
        ViewGroup.LayoutParams layoutParams = button.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        button.setLayoutParams(layoutParams);
    }

    private void setTextAndColors(TextView textView, String text, int color){
        textView.setText(text);
        textView.setTextColor(color);
    }

    private void closedAndClear(){
        AppPreferences appPreferences = new AppPreferences(this);
        finish();
        appPreferences.clearPreferences();
    }
}
