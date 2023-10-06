package com.example.estgmapper;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ZoomControls;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.estgmapper.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FrameLayout imageGroup;

    private ImageView imageView;
    private ImageView imgEeE, imgSalas, imgLabs, imgAuds, imgBeC, imgGabs, imgWC, imgOS;
    private ZoomControls zoomControls;
    private Button button;
    private float lastX, lastY, originalX, originalY;
    private float offsetX, offsetY;
    private float maxX, maxY;
    private float screenWidth, screenHeight;
    private float initialFingerSpacing = -1;
    private float originalScaleX, originalScaleY;
    private String lastFilterApplied;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        imageView = findViewById(R.id.pisos);
        imgEeE = findViewById(R.id.imgEeE);
        imgSalas = findViewById(R.id.imgSalas);
        imgLabs = findViewById(R.id.imgLabs);
        imgAuds = findViewById(R.id.imgAuds);
        imgBeC = findViewById(R.id.imgBeC);
        imgOS = findViewById(R.id.imgOS);
        imgGabs = findViewById(R.id.imgGabs);
        imgWC = findViewById(R.id.imgWC);

        zoomControls = findViewById(R.id.zoom_controls);
        imageGroup = findViewById(R.id.imageGroup);
        button = findViewById(R.id.button); // Assuming your button has the id "button" in your XML layout file

        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache the original image without downsampling
                .placeholder(R.drawable.loading_gif_png_5); // Placeholder image while loading

        // Load main image using Glide
        Glide.with(this)
                .load(R.drawable.piso1)
                .apply(requestOptions)
                .into(imageView);

        // Load other images using Glide
        loadImages();

        WindowManager wm = getWindowManager();
        screenWidth = wm.getDefaultDisplay().getWidth();
        screenHeight = wm.getDefaultDisplay().getHeight();

        originalScaleX = imageView.getScaleX();
        originalScaleY = imageView.getScaleY();

        Button resetButton = findViewById(R.id.button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset translation and scale properties of the main ImageView
                imageView.setTranslationX(0);
                imageView.setTranslationY(0);
                imageView.setScaleX(originalScaleX);
                imageView.setScaleY(originalScaleY);

                // Reset translation and scale properties of other ImageViews in imageGroup
                for (int i = 0; i < imageGroup.getChildCount(); i++) {
                    View childView = imageGroup.getChildAt(i);
                    if (childView instanceof ImageView) {
                        ImageView childImageView = (ImageView) childView;
                        childImageView.setTranslationX(0);
                        childImageView.setTranslationY(0);
                        childImageView.setScaleX(originalScaleX);
                        childImageView.setScaleY(originalScaleY);
                    }
                }
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        originalX = imageView.getTranslationX();
                        originalY = imageView.getTranslationY();
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        initialFingerSpacing = calculateFingerSpacing(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() > 1 && initialFingerSpacing != -1) {
                            float newFingerSpacing = calculateFingerSpacing(event);
                            float scaleFactor = newFingerSpacing / initialFingerSpacing;
                            float newZoom = imageView.getScaleX() * scaleFactor;

                            // Limit the zoom level to original scale or larger
                            newZoom = Math.max(originalScaleX, newZoom);
                            imageView.setScaleX(newZoom);
                            imageView.setScaleY(newZoom);
                        } else {
                            float deltaX = event.getRawX() - lastX;
                            float deltaY = event.getRawY() - lastY;
                            float newTranslateX = originalX + deltaX;
                            float newTranslateY = originalY + deltaY;

                            // Calculate maximum translation values based on zoom level and screen dimensions
                            float maxTranslateX = (imageView.getScaleX() - 1) * (imageView.getWidth() / 2);
                            float maxTranslateY = (imageView.getScaleY() - 1) * (imageView.getHeight() / 2);

                            // Ensure the image stays within the screen boundaries
                            newTranslateX = Math.min(Math.max(newTranslateX, -maxTranslateX), maxTranslateX);
                            newTranslateY = Math.min(Math.max(newTranslateY, -maxTranslateY), maxTranslateY);

                            imageView.setTranslationX(newTranslateX);
                            imageView.setTranslationY(newTranslateY);
                        }
                        applyTransformationsToImageViews();
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        initialFingerSpacing = -1;
                        break;
                }
                return true;
            }
        });

        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                final int homeId = R.drawable.piso1;

                final int dashboardId = R.drawable.piso2;

                final int notificationsId = R.drawable.piso3;


                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        imageView.setImageResource(homeId);
                        loadPiso1();
                        return true;

                    case R.id.navigation_dashboard:
                        imageView.setImageResource(dashboardId);
                        loadPiso2();
                        return true;

                    case R.id.navigation_notifications:
                        imageView.setImageResource(notificationsId);
                        loadPiso3();
                        return true;
                }
                return false;
            }
        });

        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float currentZoom = imageView.getScaleX();
                if (currentZoom + 0.1f >= 1) {
                    imageView.setScaleX(currentZoom + 0.1f);
                    imageView.setScaleY(currentZoom + 0.1f);
                for (int i = 0; i < imageGroup.getChildCount(); i++) {
                    View childView = imageGroup.getChildAt(i);
                    if (childView instanceof ImageView) {
                        ImageView childImageView = (ImageView) childView;
                        childImageView.setScaleX(currentZoom + 0.1f);
                        childImageView.setScaleY(currentZoom + 0.1f);
                        }
                    }
                }
            }
        });

        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float currentZoom = imageView.getScaleX();
                if (currentZoom - 0.1f >= 1) {
                    imageView.setScaleX(currentZoom - 0.1f);
                    imageView.setScaleY(currentZoom - 0.1f);
                    for (int i = 0; i < imageGroup.getChildCount(); i++) {
                        View childView = imageGroup.getChildAt(i);
                        if (childView instanceof ImageView) {
                            ImageView childImageView = (ImageView) childView;
                            childImageView.setScaleX(currentZoom + 0.1f);
                            childImageView.setScaleY(currentZoom + 0.1f);
                        }
                    }
                }
            }
        });

        FloatingActionButton bOpenAlertDialog = findViewById(R.id.btnFilter);
        final TextView tvSelectedItemsPreview = findViewById(R.id.selectedItemPreview);
       // final TextView tvSelectedItemsPreview = findViewById(R.id.selectedItemPreview);

        final String[] listItems = new String[]{"Salas", "Laboratórios", "Auditórios", "Gabinetes", "Bar & Cantina", "Outros Serviços", "WC's"};
        final boolean[] checkedItems = new boolean[listItems.length];

        // copy the items from the main list to the selected item list for the preview
        // if the item is checked then only the item should be displayed for the user
        final List<String> selectedItems = Arrays.asList(listItems);

        //Arrays.fill(checkedItems, true);

        // handle the Open Alert Dialog button
        bOpenAlertDialog.setOnClickListener(v -> {
            // initially set the null for the text preview
            tvSelectedItemsPreview.setText(null);

            // initialise the alert dialog builder
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            // set the title for the alert dialog
            builder.setTitle("Escolher Filtros:");

            // set the icon for the alert dialog
            builder.setIcon(R.drawable.estg_logo_wht);

            // now this is the function which sets the alert dialog for multiple item selection ready
            builder.setMultiChoiceItems(listItems, checkedItems, (dialog, which, isChecked) -> {
                checkedItems[which] = isChecked;
                String currentItem = selectedItems.get(which);

                switch (currentItem){
                    case "Salas":
                        imgSalas.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
                        break;
                    case "Laboratórios":
                        imgLabs.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
                        break;
                    case "Auditórios":
                        imgAuds.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
                        break;
                    case "Gabinetes":
                        imgGabs.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
                        break;
                    case "Bar & Cantina":
                        imgBeC.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
                        break;
                    case "Outros Serviços":
                        imgOS.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
                        break;
                    case "WC's":
                        imgWC.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
                        break;
                }

            });

            // alert dialog shouldn't be cancellable
            builder.setCancelable(false);

            // handle the positive button of the dialog
            builder.setPositiveButton("FILTRAR", (dialog, which) -> {});

            // handle the negative button of the alert dialog
            builder.setNegativeButton("CANCELAR", (dialog, which) -> {});

            // handle the neutral button of the dialog to clear the selected items boolean checkedItem
            builder.setNeutralButton("LIMPAR", (dialog, which) -> {
                Arrays.fill(checkedItems, false);
                imgSalas.setVisibility(View.VISIBLE);
                imgLabs.setVisibility(View.VISIBLE);
                imgAuds.setVisibility(View.VISIBLE);
                imgGabs.setVisibility(View.VISIBLE);
                imgBeC.setVisibility(View.VISIBLE);
                imgOS.setVisibility(View.VISIBLE);
                imgWC.setVisibility(View.VISIBLE);
            });

            // create the builder
            builder.create();

            // create the alert dialog with the alert dialog builder instance
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            tvSelectedItemsPreview.setText(lastFilterApplied);

            // Check if there are any selected items after the dialog is shown
            alertDialog.setOnDismissListener(dialogInterface -> {
                boolean hasSelectedItems = false;
                for (boolean checkedItem : checkedItems) {
                    if (checkedItem) {
                        hasSelectedItems = true;
                        break;
                    }
                }

                // Set the text and style based on selected items
                if (hasSelectedItems) {
                    tvSelectedItemsPreview.setTypeface(null, Typeface.BOLD_ITALIC);
                    tvSelectedItemsPreview.setText("Filtros Selecionados!");
                    lastFilterApplied = "Filtros Selecionados!";
                } else {
                    tvSelectedItemsPreview.setText(null);
                    lastFilterApplied = null;
                }
            });
        });

    }

    private void applyTransformationsToImageViews() {
        // Get the translation and scale values of the original ImageView
        float translationX = imageView.getTranslationX();
        float translationY = imageView.getTranslationY();
        float scaleX = imageView.getScaleX();
        float scaleY = imageView.getScaleY();

        // Apply the same translation and scale to other ImageViews in the imageGroup
        for (int i = 0; i < imageGroup.getChildCount(); i++) {
            View childView = imageGroup.getChildAt(i);
            if (childView instanceof ImageView) {
                ImageView childImageView = (ImageView) childView;
                childImageView.setTranslationX(translationX);
                childImageView.setTranslationY(translationY);
                childImageView.setScaleX(scaleX);
                childImageView.setScaleY(scaleY);
            }
        }
    }


    private float calculateFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void loadImages() {
        RequestOptions requestOptions = new RequestOptions()
                .priority(Priority.HIGH) // Set loading priority to high
                .dontTransform() // Don't transform the loaded image (load original size)
                .placeholder(R.drawable.loading_gif_png_5);

        int[] imageIds = {
                R.drawable.piso1___escadas___entradas,
                R.drawable.piso1___salas,
                R.drawable.piso1___labs,
                R.drawable.piso1___auditorios,
                R.drawable.piso1___gabinetes,
                R.drawable.piso1___comida,
                R.drawable.piso1___outros_servi_os,
                R.drawable.piso1___wc
        };

        ImageView[] imageViews = {
                imgEeE, imgSalas, imgLabs, imgAuds, imgGabs, imgBeC, imgOS, imgWC
        };

        for (int i = 0; i < imageIds.length; i++) {
            Glide.with(this)
                    .load(imageIds[i])
                    .apply(requestOptions)
                    .into(imageViews[i]);
        }
    }

    private void loadPiso1(){
        RequestOptions requestOptions = new RequestOptions()
                .priority(Priority.HIGH) // Set loading priority to high
                .dontTransform() // Don't transform the loaded image (load original size)
                .placeholder(R.drawable.loading_gif_png_5);

        int[] imageIds = {
                R.drawable.piso1___escadas___entradas,
                R.drawable.piso1___salas,
                R.drawable.piso1___labs,
                R.drawable.piso1___auditorios,
                R.drawable.piso1___gabinetes,
                R.drawable.piso1___comida,
                R.drawable.piso1___outros_servi_os,
                R.drawable.piso1___wc
        };

        ImageView[] imageViews = {
                imgEeE, imgSalas, imgLabs, imgAuds, imgGabs, imgBeC, imgOS, imgWC
        };

        for (int i = 0; i < imageIds.length; i++) {
            Glide.with(this)
                    .load(imageIds[i])
                    .apply(requestOptions)
                    .into(imageViews[i]);
        }
    }

    private void loadPiso2(){
        RequestOptions requestOptions = new RequestOptions()
                .priority(Priority.HIGH) // Set loading priority to high
                .dontTransform() // Don't transform the loaded image (load original size)
                .placeholder(R.drawable.loading_gif_png_5);

        int[] imageIds = {
                R.drawable.piso2___escadas,
                R.drawable.piso2___salas,
                R.drawable.piso2___labs,
                R.drawable.piso2___audit_rios,
                R.drawable.piso2___gabinetes,
                R.drawable.piso2___bar,
                R.drawable.piso2___outros_servi_os,
                R.drawable.piso2___wc
        };

        ImageView[] imageViews = {
                imgEeE, imgSalas, imgLabs, imgAuds, imgGabs, imgBeC, imgOS, imgWC
        };

        for (int i = 0; i < imageIds.length; i++) {
            Glide.with(this)
                    .load(imageIds[i])
                    .apply(requestOptions)
                    .into(imageViews[i]);
        }
    }

    private void loadPiso3(){
        RequestOptions requestOptions = new RequestOptions()
                .priority(Priority.HIGH) // Set loading priority to high
                .dontTransform() // Don't transform the loaded image (load original size)
                .placeholder(R.drawable.loading_gif_png_5);

        int[] imageIds = {
                R.drawable.piso3___escadas,
                R.drawable.piso3___salas,
                R.drawable.piso3___labs,
                R.drawable.piso3___auditorios,
                R.drawable.piso3___gabinetes,
                R.drawable.piso3___bar,
                R.drawable.piso3___outros_servi_os,
                R.drawable.piso3___wc
        };

        ImageView[] imageViews = {
                imgEeE, imgSalas, imgLabs, imgAuds, imgGabs, imgBeC, imgOS, imgWC
        };

        for (int i = 0; i < imageIds.length; i++) {
            Glide.with(this)
                    .load(imageIds[i])
                    .apply(requestOptions)
                    .into(imageViews[i]);
        }
    }



}
