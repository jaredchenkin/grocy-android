/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Global;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.EditText;
import androidx.annotation.Dimension;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat;
import androidx.core.view.WindowInsetsAnimationCompat.Callback;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.HarmonizedColorsOptions;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.math.MathUtils;
import java.lang.reflect.Field;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.Constants.THEME;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.BottomScrollBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.ActivityMainBinding;

public class UiUtil {

  public static final int SCRIM = 0x55000000;
  public static final int SCRIM_DARK_DIALOG = 0xFF0c0c0e;
  public static final int SCRIM_LIGHT_DIALOG = 0xFF666666;

  private boolean wasKeyboardOpened;
  private float fabBaseY;
  private int focusedScrollOffset;
  private int bottomInsetIme;

  public static void setTheme(Activity activity, SharedPreferences sharedPrefs) {
    switch (sharedPrefs.getString(SETTINGS.APPEARANCE.THEME, SETTINGS_DEFAULT.APPEARANCE.THEME)) {
      case THEME.RED:
        activity.setTheme(R.style.Theme_Grocy_Red);
        break;
      case THEME.YELLOW:
        activity.setTheme(R.style.Theme_Grocy_Yellow);
        break;
      case THEME.LIME:
        activity.setTheme(R.style.Theme_Grocy_Lime);
        break;
      case THEME.GREEN:
        activity.setTheme(R.style.Theme_Grocy_Green);
        break;
      case THEME.TURQUOISE:
        activity.setTheme(R.style.Theme_Grocy_Turquoise);
        break;
      case THEME.TEAL:
        activity.setTheme(R.style.Theme_Grocy_Teal);
        break;
      case THEME.BLUE:
        activity.setTheme(R.style.Theme_Grocy_Blue);
        break;
      case THEME.PURPLE:
        activity.setTheme(R.style.Theme_Grocy_Purple);
        break;
      default:
        if (DynamicColors.isDynamicColorAvailable()) {
          DynamicColors.applyToActivityIfAvailable(
              activity,
              new DynamicColorsOptions.Builder().setOnAppliedCallback(
                  activityCallback -> HarmonizedColors.applyToContextIfAvailable(
                      activity, HarmonizedColorsOptions.createMaterialDefaults()
                  )
              ).build()
          );
        } else {
          activity.setTheme(R.style.Theme_Grocy_Green);
        }
        break;
    }
  }

  public static void layoutEdgeToEdge(Window window) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
      window.setDecorFitsSystemWindows(false);
    } else {
      int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
      View decorView = window.getDecorView();
      decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | flags);
    }
  }

  public static void setLightNavigationBar(@NonNull View view, boolean isLight) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
      view.getWindowInsetsController().setSystemBarsAppearance(
          isLight ? WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : 0,
          WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
      );
    } else if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
      int flags = view.getSystemUiVisibility();
      if (isLight) {
        flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      } else {
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      }
      view.setSystemUiVisibility(flags);
    }
  }

  public static void setLightStatusBar(@NonNull View view, boolean isLight) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
      view.getWindowInsetsController().setSystemBarsAppearance(
          isLight ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
          WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
      );
    } else if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
      int flags = view.getSystemUiVisibility();
      if (isLight) {
        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      } else {
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      }
      view.setSystemUiVisibility(flags);
    }
  }

  public void setUpBottomAppBar(String TAG, ActivityMainBinding binding) {
    binding.bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_END);
    binding.bottomAppBar.setMenuAlignmentMode(BottomAppBar.MENU_ALIGNMENT_MODE_START);

    Context context = binding.getRoot().getContext();
    // Use reflection to store bottomInset in BAB manually
    // The automatic method includes IME insets which is bad behavior for BABs
    ViewCompat.setOnApplyWindowInsetsListener(binding.bottomAppBar, (v, insets) -> {
      int bottomInset = insets.getInsets(Type.systemBars()).bottom;
      ViewCompat.setPaddingRelative(v, 0, 0, 0, bottomInset);
      Class<?> classBottomAppBar = BottomAppBar.class;
      Object objectBottomAppBar = classBottomAppBar.cast(binding.bottomAppBar);
      Field fieldBottomInset = null;
      try {
        if (objectBottomAppBar != null && objectBottomAppBar.getClass().getSuperclass() != null) {
          fieldBottomInset = objectBottomAppBar.getClass().getDeclaredField("bottomInset");
        } else {
          Log.e(TAG, "onCreate: reflection for bottomInset not working");
        }
      } catch (NoSuchFieldException e) {
        Log.e(TAG, "onCreate: ", e);
      }
      if (fieldBottomInset != null) {
        fieldBottomInset.setAccessible(true);
        try {
          fieldBottomInset.set(objectBottomAppBar, bottomInset);
        } catch (IllegalAccessException e) {
          Log.e(TAG, "onCreate: ", e);
        }
      }
      // Calculate initial FAB y position for restoring after shifted by keyboard
      int babHeight = UiUtil.dpToPx(context, 80);
      int fabHeight = UiUtil.dpToPx(context, 56);
      int bottom = UiUtil.getDisplayHeight(context);
      fabBaseY = bottom - bottomInset - (babHeight / 2f) - (fabHeight / 2f);
      return insets;
    });
    binding.bottomAppBar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(context));
    binding.fabMainScroll.setBackgroundTintList(
        ColorStateList.valueOf(SurfaceColors.SURFACE_2.getColor(context))
    );
    ViewUtil.setTooltipText(binding.fabMainScroll, R.string.action_top_scroll);
  }

  public static void updateBottomAppBar(
      ActivityMainBinding binding,
      Context context,
      SharedPreferences sharedPrefs,
      boolean showFab,
      @MenuRes int newMenuId,
      @Nullable OnMenuItemClickListener onMenuItemClickListener
  ) {
    // Handler with postDelayed is necessary for workaround of issue #552
    new Handler().postDelayed(() -> {
      if (showFab && !binding.fabMain.isShown() && !PrefsUtil.isServerUrlEmpty(sharedPrefs)) {
        binding.fabMain.show();
      } else if (!showFab && binding.fabMain.isShown()) {
        binding.fabMain.hide();
      }

      Drawable overflowIcon = binding.bottomAppBar.getOverflowIcon();

      // IF ANIMATIONS DISABLED
      if (!UiUtil.areAnimationsEnabled(context)) {
        binding.bottomAppBar.replaceMenu(newMenuId);
        Menu menu = binding.bottomAppBar.getMenu();
        int tint = ResUtil.getColorAttr(context, R.attr.colorOnSurfaceVariant);
        for (int i = 0; i < menu.size(); i++) {
          MenuItem item = menu.getItem(i);
          if (item.getIcon() != null) {
            item.getIcon().mutate();
            item.getIcon().setAlpha(255);
            item.getIcon().setTint(tint);
          }
        }
        if (overflowIcon != null && overflowIcon.isVisible()) {
          overflowIcon.setAlpha(255);
          overflowIcon.setTint(tint);
        }
        binding.bottomAppBar.setOnMenuItemClickListener(onMenuItemClickListener);
        return;
      }

      long iconFadeOutDuration = 150;
      long iconFadeInDuration = 300;

      int alphaFrom = 255;
      // get better start value if animation was not finished yet
      if (binding.bottomAppBar.getMenu() != null
          && binding.bottomAppBar.getMenu().size() > 0
          && binding.bottomAppBar.getMenu().getItem(0) != null
          && binding.bottomAppBar.getMenu().getItem(0).getIcon() != null) {
        alphaFrom = binding.bottomAppBar.getMenu().getItem(0).getIcon().getAlpha();
      }
      ValueAnimator animatorFadeOut = ValueAnimator.ofInt(alphaFrom, 0);
      animatorFadeOut.addUpdateListener(animation -> {
        for (int i = 0; i < binding.bottomAppBar.getMenu().size(); i++) {
          MenuItem item = binding.bottomAppBar.getMenu().getItem(i);
          if (item.getIcon() != null && item.isVisible()) {
            item.getIcon().setAlpha((int) animation.getAnimatedValue());
          }
        }
        if (overflowIcon != null && overflowIcon.isVisible()) {
          overflowIcon.setAlpha((int) animation.getAnimatedValue());
        }
      });
      animatorFadeOut.setDuration(iconFadeOutDuration);
      animatorFadeOut.setInterpolator(new FastOutSlowInInterpolator());
      animatorFadeOut.start();

      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        binding.bottomAppBar.replaceMenu(newMenuId);

        int iconIndex = 0;
        int overflowCount = 0;
        int tint = ResUtil.getColorAttr(context, R.attr.colorOnSurfaceVariant);
        for (int i = 0; i < binding.bottomAppBar.getMenu().size(); i++) {
          MenuItem item = binding.bottomAppBar.getMenu().getItem(i);
          if (item.getIcon() == null || !item.isVisible()) {
            if (item.isVisible()) {
              overflowCount++;
            }
            continue;
          }
          iconIndex++;
          int index = iconIndex;
          item.getIcon().mutate();
          item.getIcon().setTint(tint);
          item.getIcon().setAlpha(0);
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Rect bounds = item.getIcon().copyBounds();
            int top = bounds.top;
            int offset = UiUtil.dpToPx(context, 12);
            ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
            animator.addUpdateListener(animation -> {
              bounds.offsetTo(
                  0,
                  (int) (top + (float) animation.getAnimatedValue() * offset)
              );
              item.getIcon().setBounds(bounds);
              item.getIcon().setAlpha(255 - (int) ((float) animation.getAnimatedValue() * 255));
              item.getIcon().invalidateSelf();
            });
            animator.setDuration(iconFadeInDuration - index * 50L);
            animator.setInterpolator(new FastOutSlowInInterpolator());
            animator.start();
          }, index * 90L);
        }
        if (overflowCount > 0) {
          Drawable overflowIconNew = binding.bottomAppBar.getOverflowIcon();
          if (overflowIconNew == null || !overflowIconNew.isVisible()) {
            return;
          }
          iconIndex++;
          int index = iconIndex;
          overflowIconNew.setTint(tint);
          overflowIconNew.setAlpha(0);
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Rect bounds = overflowIconNew.copyBounds();
            int top = bounds.top;
            int offset = UiUtil.dpToPx(context, 12);
            ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
            animator.addUpdateListener(animation -> {
              bounds.offsetTo(
                  0,
                  (int) (top + (float) animation.getAnimatedValue() * offset)
              );
              overflowIconNew.setBounds(bounds);
              overflowIconNew.setAlpha(255 - (int) ((float) animation.getAnimatedValue() * 255));
              overflowIconNew.invalidateSelf();
            });
            animator.setDuration(iconFadeInDuration - index * 50L);
            animator.setInterpolator(new FastOutSlowInInterpolator());
            animator.start();
          }, index * 90L);
        }
        binding.bottomAppBar.setOnMenuItemClickListener(onMenuItemClickListener);
      }, iconFadeOutDuration);
    }, 10);
  }

  public void setupImeAnimation(
      MainActivity activity,
      SystemBarBehavior systemBarBehavior,
      BottomScrollBehavior scrollBehavior
  ) {
    ActivityMainBinding binding = activity.binding;
    Callback callback = new Callback(Callback.DISPATCH_MODE_STOP) {
      WindowInsetsAnimationCompat animation;
      int bottomInsetStart, bottomInsetEnd;
      float yStart, yEnd;
      int yScrollStart, yScrollEnd;

      @Override
      public void onPrepare(@NonNull WindowInsetsAnimationCompat animation) {
        this.animation = animation;
        if (systemBarBehavior != null) {
          bottomInsetStart = systemBarBehavior.getAdditionalBottomInset();
        }
        yStart = binding.fabMain.getY();
        // scroll offset to keep focused view visible
        ViewGroup scrollView = scrollBehavior.getScrollView();
        if (scrollView != null) {
          yScrollStart = scrollView.getScrollY();
        }
      }

      @NonNull
      @Override
      public BoundsCompat onStart(
          @NonNull WindowInsetsAnimationCompat animation, @NonNull BoundsCompat bounds) {
        if (systemBarBehavior != null) {
          bottomInsetEnd = systemBarBehavior.getAdditionalBottomInset();
          systemBarBehavior.setAdditionalBottomInset(bottomInsetStart);
          systemBarBehavior.refresh(false);
        }
        yEnd = binding.fabMain.getY();
        binding.fabMain.setY(yStart);
        // scroll offset to keep focused view visible
        ViewGroup scrollView = scrollBehavior.getScrollView();
        if (scrollView != null) {
          yScrollEnd = yScrollStart + focusedScrollOffset;
        }
        return bounds;
      }

      @NonNull
      @Override
      public WindowInsetsCompat onProgress(
          @NonNull WindowInsetsCompat insets,
          @NonNull List<WindowInsetsAnimationCompat> animations) {
        if (systemBarBehavior != null) {
          systemBarBehavior.setAdditionalBottomInset(
              (int) MathUtils.lerp(
                  bottomInsetStart, bottomInsetEnd, animation.getInterpolatedFraction()
              )
          );
          systemBarBehavior.refresh(false);
        }
        binding.fabMain.setY(MathUtils.lerp(yStart, yEnd, animation.getInterpolatedFraction()));
        // scroll offset to keep focused view visible
        ViewGroup scrollView = scrollBehavior.getScrollView();
        if (scrollView != null) {
          View parent = (View) scrollView.getParent();
          parent.setPadding(
              parent.getPaddingLeft(),
              parent.getPaddingTop(),
              parent.getPaddingRight(),
              bottomInsetIme
          );

          scrollView.setScrollY(
              (int) MathUtils.lerp(yScrollStart, yScrollEnd, animation.getInterpolatedFraction())
          );
        }
        return insets;
      }
    };
    ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorMain, (v, insets) -> {
      int bottomInsetIme = insets.getInsets(Type.ime()).bottom;
      int bottomInsetBars = insets.getInsets(Type.systemBars()).bottom;
      if (systemBarBehavior != null) {
        systemBarBehavior.setAdditionalBottomInset(bottomInsetIme);
        systemBarBehavior.refresh(false);
      }
      // view for calculating snackbar anchor's max bottom position
      // to prevent snackbar flickering (caused by itself when it's drawn behind navbar
      CoordinatorLayout.LayoutParams paramsAnchor =
          (CoordinatorLayout.LayoutParams) binding.anchorMaxBottom.getLayoutParams();
      paramsAnchor.bottomMargin = bottomInsetBars - UiUtil.dpToPx(activity, 12);
      if (insets.isVisible(Type.ime())) {
        wasKeyboardOpened = true;
        binding.fabMain.setTranslationY(-bottomInsetIme - UiUtil.dpToPx(activity, 16));
        int keyboardY = UiUtil.getDisplayHeight(activity) - bottomInsetIme;
        if (keyboardY < scrollBehavior.getSnackbarAnchorY()) {
          binding.anchor.setY(keyboardY);
        } else {
          scrollBehavior.updateSnackbarAnchor();
        }
        float elevation = UiUtil.dpToPx(activity, 6);
        ViewCompat.setElevation(binding.fabMain, elevation);
        binding.fabMain.setCompatElevation(elevation);

        // scroll offset to keep focused view visible
        View focused = activity.getCurrentFocus();
        if (focused instanceof EditText) {
          EditText editText = (EditText) focused;
          int cursorPosition = editText.getSelectionStart();
          if (editText.getLayout() != null) {
            int line = editText.getLayout().getLineForOffset(cursorPosition);
            int cursorYTop = (int) editText.getLayout().getLineTop(line);
            int cursorYBottom = (int) editText.getLayout().getLineBottom(line);
            int lineHeight = cursorYBottom - cursorYTop;
            int[] location = new int[2];
            editText.getLocationInWindow(location);
            int absoluteCursorYBottom = location[1] + cursorYBottom;
            int screenHeight = UiUtil.getDisplayHeight(activity);
            int bottomSpace = screenHeight - absoluteCursorYBottom;

            if (bottomSpace - lineHeight*3 < bottomInsetIme) {
              focusedScrollOffset = bottomInsetIme - bottomSpace + lineHeight*3;
            } else {
              focusedScrollOffset = 0;
            }
          } else {
            focusedScrollOffset = 0;
          }
        } else if (focused != null) {
          int[] location = new int[2];
          focused.getLocationInWindow(location);
          location[1] += focused.getHeight();
          int screenHeight = UiUtil.getDisplayHeight(activity);
          int bottomSpace = screenHeight - location[1];
          focusedScrollOffset = bottomInsetIme - bottomSpace;
        } else {
          focusedScrollOffset = 0;
        }
      } else {
        binding.fabMain.setY(fabBaseY);
        scrollBehavior.updateSnackbarAnchor();
        ViewCompat.setElevation(binding.fabMain, 0);
        binding.fabMain.setCompatElevation(0);
        // If the keyboard was shown and the page was therefore scrollable
        // and the bottom bar has disappeared caused by scrolling down,
        // then the bottom bar should not stay hidden when the keyboard disappears
        if (wasKeyboardOpened) {
          wasKeyboardOpened = false;
          scrollBehavior.setBottomBarVisibility(true);
        }
        // scroll offset to keep focused view visible
        focusedScrollOffset = 0;
      }

      // true if for example keyboard type changes and numeric keyboard is higher
      boolean imeInsetChanged = bottomInsetIme != this.bottomInsetIme
          && bottomInsetIme != 0 && this.bottomInsetIme != 0;
      this.bottomInsetIme = bottomInsetIme;

      ViewGroup scrollView = scrollBehavior.getScrollView();
      if (imeInsetChanged && scrollView != null) {
        View parent = (View) scrollView.getParent();
        parent.setPadding(
            parent.getPaddingLeft(),
            parent.getPaddingTop(),
            parent.getPaddingRight(),
            bottomInsetIme
        );
      }

      return insets;
    });
    ViewCompat.setWindowInsetsAnimationCallback(binding.coordinatorMain, callback);
  }

  public static boolean isDarkModeActive(Context context) {
    int uiMode = context.getResources().getConfiguration().uiMode;
    return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  }

  public static boolean isNavigationModeGesture(Context context) {
    final int NAV_GESTURE = 2;
    Resources resources = context.getResources();
    @SuppressLint("DiscouragedApi")
    int resourceId = resources.getIdentifier(
        "config_navBarInteractionMode", "integer", "android"
    );
    int mode = resourceId > 0 ? resources.getInteger(resourceId) : 0;
    return mode == NAV_GESTURE;
  }

  public static boolean isOrientationPortrait(Context context) {
    int orientation = context.getResources().getConfiguration().orientation;
    return orientation == Configuration.ORIENTATION_PORTRAIT;
  }

  public static boolean isLayoutRtl(Context context) {
    int direction = context.getResources().getConfiguration().getLayoutDirection();
    return direction == View.LAYOUT_DIRECTION_RTL;
  }

  public static boolean isFullWidth(Context context) {
    int maxWidth = context.getResources().getDimensionPixelSize(R.dimen.max_content_width);
    return maxWidth >= getDisplayWidth(context);
  }

  // Unit conversions

  public static int dpToPx(@NonNull Context context, @Dimension(unit = Dimension.DP) float dp) {
    Resources r = context.getResources();
    return Math.round(dp * r.getDisplayMetrics().density);
  }

  public static int spToPx(@NonNull Context context, @Dimension(unit = Dimension.SP) float sp) {
    Resources r = context.getResources();
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, r.getDisplayMetrics());
  }

  // Display metrics

  public static int getDisplayWidth(Context context) {
    return getDisplayMetrics(context, true);
  }

  public static int getDisplayHeight(Context context) {
    return getDisplayMetrics(context, false);
  }

  private static int getDisplayMetrics(Context context, boolean useWidth) {
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
      WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
      if (useWidth) {
        return windowMetrics.getBounds().width();
      } else {
        return windowMetrics.getBounds().height();
      }
    } else {
      // More reliable method from MDC lib
      Display defaultDisplay = windowManager.getDefaultDisplay();
      Point defaultDisplaySize = new Point();
      defaultDisplay.getRealSize(defaultDisplaySize);
      Rect bounds = new Rect();
      bounds.right = defaultDisplaySize.x;
      bounds.bottom = defaultDisplaySize.y;
      return useWidth ? bounds.width() : bounds.height();
      // Old method
      /*DisplayMetrics displayMetrics = new DisplayMetrics();
      windowManager.getDefaultDisplay().getMetrics(displayMetrics);
      return useWidth ? displayMetrics.widthPixels : displayMetrics.heightPixels;*/
    }
  }

  // A11y animation reduction

  public static boolean areAnimationsEnabled(Context context) {
    boolean duration = Global.getFloat(
        context.getContentResolver(), Global.ANIMATOR_DURATION_SCALE, 1
    ) != 0;
    boolean transition = Global.getFloat(
        context.getContentResolver(), Global.TRANSITION_ANIMATION_SCALE, 1
    ) != 0;
    boolean window = Global.getFloat(
        context.getContentResolver(), Global.WINDOW_ANIMATION_SCALE, 1
    ) != 0;
    return duration && transition && window;
  }
}
