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

package xyz.zedler.patrick.grocy.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.ViewAppBarLayoutBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.util.BindingAdaptersUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class CustomAppBarLayout extends AppBarLayout {

  public ViewAppBarLayoutBinding binding;

  public CustomAppBarLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    binding = ViewAppBarLayoutBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    );
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    binding = null;
  }

  public void setOnSearchFieldTextChanged(TextChangedListener listener) {
    binding.editTextSearch.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }
      @Override
      public void afterTextChanged(Editable s) {
        listener.afterTextChanged(s != null ? s.toString() : "");
      }
    });
  }

  public interface TextChangedListener {
    void afterTextChanged(String s);
  }

  public void setOnKeyboardSearchListener(Runnable listener) {
    BindingAdaptersUtil.setOnSearchClickInSoftKeyboardListener(binding.editTextSearch, listener);
  }

  public void setFilterScrollViewVisibility(boolean visible) {
    binding.filterScrollView.setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  public void setFilters(FilterChipLiveData... liveDataObjects) {
    binding.filterContainer.removeAllViews();
    View separatorView = createSeparatorView(getContext());
    for (FilterChipLiveData liveData : liveDataObjects) {
      if (liveData != null) {
        FilterChip filterChip = new FilterChip(getContext());
        filterChip.setData(liveData, true);
        binding.filterContainer.addView(filterChip);
      } else {
        binding.filterContainer.addView(separatorView);
      }
    }
  }

  private View createSeparatorView(Context context) {
    View separator = new View(context);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        UiUtil.dpToPx(context, 1),
        UiUtil.dpToPx(context, 18)
    );
    params.setMargins(
        UiUtil.dpToPx(context, 4), 0,
        UiUtil.dpToPx(context, 4), 0
    );
    params.gravity = Gravity.CENTER_VERTICAL;
    separator.setLayoutParams(params);

    TypedValue typedValue = new TypedValue();
    getContext().getTheme().resolveAttribute(R.attr.colorOutlineVariant, typedValue, true);
    separator.setBackgroundColor(typedValue.data);
    return separator;
  }

  public void setOfflineInfoVisibility(boolean visible) {
    binding.offlineInfo.setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  public void animateChanges(@NonNull View titleBar, Runnable changes) {
    View viewFadeOut = null;
    for (int i = 0; i < binding.topBar.getChildCount(); i++) {
      View child = binding.topBar.getChildAt(i);
      if (child.getVisibility() == VISIBLE && child.getId() != titleBar.getId()) {
        viewFadeOut = child;
      }
    }

    if (viewFadeOut != null) {
      View finalViewFadeOut = viewFadeOut;
      viewFadeOut.animate().alpha(0f).setDuration(200)
          .withEndAction(() -> finalViewFadeOut.setVisibility(GONE));
    }
    titleBar.setAlpha(0f);
    titleBar.setVisibility(View.VISIBLE);
    titleBar.animate().alpha(1f).setDuration(200);



    changes.run();

    int height = binding.appBar.getMeasuredHeight();


    if (height > 0) {
      int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.AT_MOST);
      int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
      binding.appBar.measure(widthMeasureSpec, heightMeasureSpec);
      ValueAnimator anim = ValueAnimator.ofInt(height, binding.appBar.getMeasuredHeight());
      anim.addUpdateListener(valueAnimator -> {
        int val = (Integer) valueAnimator.getAnimatedValue();
        ViewGroup.LayoutParams layoutParams = binding.appBar.getLayoutParams();
        layoutParams.height = val;
        binding.appBar.setLayoutParams(layoutParams);
      });
      anim.setDuration(200);
      anim.start();
    }


  }




}
