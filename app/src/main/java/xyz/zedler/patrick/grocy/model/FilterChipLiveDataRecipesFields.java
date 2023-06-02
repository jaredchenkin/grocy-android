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

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;

public class FilterChipLiveDataRecipesFields extends FilterChipLiveData {

  public final static int ID_FIELD_DUE_SCORE = 0;
  public final static int ID_FIELD_FULFILLMENT = 1;
  public final static int ID_FIELD_CALORIES = 2;

  public final static String FIELD_DUE_SCORE = "extra_field_due_score";
  public final static String FIELD_FULFILLMENT = "extra_field_fulfillment";
  public final static String FIELD_CALORIES = "extra_field_calories";

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private ArrayList<String> activeFields;

  public FilterChipLiveDataRecipesFields(Application application, Runnable clickListener) {
    this.application = application;
    setItemIdChecked(-1);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    activeFields = getNamesListFromMulti(sharedPrefs.getString(PREF.RECIPES_FIELDS, null));
    if (activeFields.isEmpty()) activeFields = getNamesList(
        FIELD_DUE_SCORE, FIELD_FULFILLMENT
    );
    setFilterText();
    setItems();
    if (clickListener != null) {
      setMenuItemClickListener(item -> {
        setValues(item.getItemId());
        setItems();
        emitValue();
        clickListener.run();
        return true;
      });
    }
  }

  public List<String> getActiveFields() {
    return activeFields;
  }

  private void setFilterText() {
    setText(application.getString(R.string.property_fields));
  }

  public void setValues(int id) {
    String field = null;
    if (id == ID_FIELD_DUE_SCORE) {
      field = FIELD_DUE_SCORE;
    } else if (id == ID_FIELD_FULFILLMENT) {
      field = FIELD_FULFILLMENT;
    } else if (id == ID_FIELD_CALORIES) {
      field = FIELD_CALORIES;
    }
    addOrRemoveNameFromList(activeFields, field);
    sharedPrefs.edit().putString(PREF.RECIPES_FIELDS, createMultiNamesActive(activeFields)).apply();
  }

  private void setItems() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        ID_FIELD_DUE_SCORE,
        0,
        application.getString(R.string.property_due_score),
        activeFields.contains(FIELD_DUE_SCORE)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_FIELD_FULFILLMENT,
        0,
        application.getString(R.string.property_requirements_fulfilled),
        activeFields.contains(FIELD_FULFILLMENT)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_FIELD_CALORIES,
        0,
        application.getString(R.string.property_calories),
        activeFields.contains(FIELD_CALORIES)
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(new MenuItemGroup(0, true, false));
    emitValue();
  }
}