package br.com.inngage.sdk;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class InApp extends AppCompatActivity {
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inapp);

        showInAppDialog(getIntent());
    }

    public void showInAppDialog(final Intent intent) {
        Executor executor = Executors.newFixedThreadPool(10);
        executor.execute(() -> {
            Bundle bundle = intent.getExtras();
            if (bundle != null){
                ArrayList<String> arrayList = bundle.getStringArrayList("keyInApp");
                handler.post(() -> {
                    if (hasValidValues(arrayList)) {
                        renderInApp(arrayList);
                    } else {
                        closedAndClear();
                    }
                });
            }
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

    private void renderInApp(ArrayList<String> values) {
        Dialog dialog = createDialog();
        dialog.show();

        CardView card = dialog.findViewById(R.id.cardInApp);
        TextView title = dialog.findViewById(R.id.titleInApp);
        TextView body = dialog.findViewById(R.id.bodyInApp);

        configureCardDimensions(card);

        int titleColor = getColorFromHex(values.get(InAppConstants.TITLE_FONT_COLOR));
        int bodyColor = getColorFromHex(values.get(InAppConstants.BODY_FONT_COLOR));

        renderBackgroundImage(card, values);

        if(card.getVisibility() == View.VISIBLE){
            card.setCardBackgroundColor(getColorFromHex(values.get(InAppConstants.BACKGROUND_COLOR)));
        }

        setTextAndColors(title, values.get(InAppConstants.TITLE_IN_APP), titleColor);
        setTextAndColors(body, values.get(InAppConstants.BODY_IN_APP), bodyColor);

        renderSliderImages(dialog, values);

        renderButton(dialog, values);

        renderPixel(dialog, values);
    }

    private Dialog createDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(dialogInterface -> {
            closedAndClear();
        });
        dialog.setContentView(R.layout.inapp);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    private void configureCardDimensions(CardView card){
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        int minWidth = (int) (screenWidth * 0.9);
        int minHeight = (int) (screenHeight * 0.3);

        card.setMinimumWidth(minWidth);
        card.setMinimumHeight(minHeight);
    }

    private void renderSliderImages(Dialog dialog, ArrayList<String> values) {
        ImageSlider imageSlider = dialog.findViewById(R.id.imageSliderInApp);

        if (values.get(InAppConstants.RICH_CONTENT) != null){
            try {
                JSONObject jsonObject = new JSONObject(values.get(InAppConstants.RICH_CONTENT));
                boolean isCarousel = jsonObject.optBoolean("carousel");

                if (isCarousel) {
                    LinearLayout linearLayout = dialog.findViewById(R.id.containerBody);
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) linearLayout.getLayoutParams();
                    params.gravity = Gravity.TOP;
                    ArrayList<SlideModel> imageList = createSlideModelsFromJSON(jsonObject);
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

    private ArrayList<SlideModel> createSlideModelsFromJSON(JSONObject jsonObject) {
        ArrayList<SlideModel> imageList = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            String imageUrl = jsonObject.optString("img" + i, "");
            if (!imageUrl.isEmpty()) {
                imageList.add(new SlideModel(imageUrl, ScaleTypes.CENTER_INSIDE));
            }
        }

        return imageList;
    }

    private void configureImageSlider(ImageSlider imageSlider, ArrayList<SlideModel> imageList) {
        if (!imageList.isEmpty()) {
            imageSlider.setImageList(imageList);
        } else {
            imageSlider.setVisibility(View.GONE);
        }
    }

    private void renderButton(Dialog dialog, ArrayList<String> values) {
        Button btnRight = dialog.findViewById(R.id.buttonRight);
        Button btnLeft = dialog.findViewById(R.id.buttonLeft);

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
                values.get(InAppConstants.BTN_LEFT_ACTION_LINK));
        setButtonClickListener(
                btnRight,
                values.get(InAppConstants.BTN_RIGHT_ACTION_TYPE),
                values.get(InAppConstants.BTN_RIGHT_ACTION_LINK));
    }

    private void setRoundedButton(Button button, String bgColorHex) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(40);
        shape.setColor(getColorFromHex(bgColorHex));
        button.setBackground(shape);
    }

    private void setButtonClickListener(Button button, String actionType, String actionValue) {
        button.setOnClickListener(view -> {
            if (actionType != null && actionType.contains("deep")) {
                if (actionValue != null || !actionValue.isEmpty())
                    InngageUtils.deep(this, actionValue);
                else
                    closedAndClear();
            } else if (actionType != null && actionType.contains("inapp")) {
                if (actionValue != null || !actionValue.isEmpty())
                    InngageUtils.web(actionValue, this);
                else
                    closedAndClear();
            }
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

        float cornerRadius = 10f;

        Glide.with(this)
                .asBitmap()
                .load(values.get(InAppConstants.BACKGROUND_IMAGE))
                .apply(new RequestOptions().override(Target.SIZE_ORIGINAL))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        int largura = resource.getWidth();
                        int altura = resource.getHeight();

                        Bitmap roundedImage = addRoundedCornersToBitmap(resource, cornerRadius);
                        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), roundedImage);

                        if (altura - 100 > largura){
                            card.getLayoutParams().height = (int) (screenHeight * 0.8);
                            card.getLayoutParams().width = (int) (screenWidth * 0.9);
                        } else {
                            card.getLayoutParams().height = card.getHeight();
                            card.getLayoutParams().width = card.getWidth();
                        }
                        card.setBackground(bitmapDrawable);
                        card.setVisibility(View.VISIBLE);

                        Log.i(InngageConstants.TAG_INAPP, "Largura: " + String.valueOf(largura) + "; Altura: " + String.valueOf(altura));
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
