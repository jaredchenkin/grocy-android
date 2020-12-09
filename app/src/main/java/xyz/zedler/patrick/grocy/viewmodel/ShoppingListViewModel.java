package xyz.zedler.patrick.grocy.viewmodel;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.ShoppingListHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.repository.ShoppingListRepository;
import xyz.zedler.patrick.grocy.util.Constants;

public class ShoppingListViewModel extends AndroidViewModel {

    private static final String TAG = ShoppingListViewModel.class.getSimpleName();
    private static final int DEFAULT_SHOPPING_LIST_ID = 1;

    private final SharedPreferences sharedPrefs;
    private final DownloadHelper dlHelper;
    private final GrocyApi grocyApi;
    private final EventHandler eventHandler;
    private final ShoppingListRepository repository;

    private final MutableLiveData<Boolean> isLoadingLive;
    private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
    private final MutableLiveData<Integer> selectedShoppingListIdLive;
    private final MutableLiveData<Boolean> offlineLive;
    private final MutableLiveData<ArrayList<GroupedListItem>> filteredGroupedListItemsLive;

    private ArrayList<ShoppingListItem> shoppingListItems;
    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<Product> products;
    private ArrayList<MissingItem> missingItems;

    private DownloadHelper.Queue currentQueueLoading;
    private String searchInput;
    private int filterState;
    private int itemsMissingCount;
    private int itemsUndoneCount;
    private final boolean debug;

    public ShoppingListViewModel(@NonNull Application application) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        isLoadingLive = new MutableLiveData<>(false);
        dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
        grocyApi = new GrocyApi(getApplication());
        eventHandler = new EventHandler();
        repository = new ShoppingListRepository(application);

        infoFullscreenLive = new MutableLiveData<>();
        offlineLive = new MutableLiveData<>(false);
        selectedShoppingListIdLive = new MutableLiveData<>(1);
        filteredGroupedListItemsLive = new MutableLiveData<>();

        filterState = ShoppingListItemAdapter.FILTER_NOTHING;
        itemsMissingCount = 0;
        itemsUndoneCount = 0;

        int lastId = sharedPrefs.getInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1);
        if(lastId != DEFAULT_SHOPPING_LIST_ID
                && !isFeatureEnabled(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS)) {
            sharedPrefs.edit()
                    .putInt(Constants.PREF.SHOPPING_LIST_LAST_ID, DEFAULT_SHOPPING_LIST_ID)
                    .apply();
            lastId = DEFAULT_SHOPPING_LIST_ID;
        }
        selectedShoppingListIdLive.setValue(lastId);
    }

    public void loadFromDatabase(boolean downloadAfterLoading) {
        repository.loadFromDatabase(
                (shoppingListItems, shoppingLists, productGroups, quantityUnits) -> {
                    this.shoppingListItems = shoppingListItems;
                    this.shoppingLists = shoppingLists;
                    this.productGroups = productGroups;
                    this.quantityUnits = quantityUnits;
                    updateFilteredShoppingListItems();
                    if(downloadAfterLoading) downloadData();
                }
        );
    }

    public ArrayList<QuantityUnit> getQuantityUnits() {
        return this.quantityUnits;
    }

    public void updateFilteredShoppingListItems() {
        filteredGroupedListItemsLive.setValue(
                ShoppingListHelper.groupItems(
                        getApplication(),
                        getFilteredShoppingListItems(),
                        this.productGroups,
                        this.shoppingLists,
                        getSelectedShoppingListId(),
                        true
                )
        );
        selectedShoppingListIdLive.setValue(selectedShoppingListIdLive.getValue());
    }

    @Nullable
    public ArrayList<ShoppingListItem> getFilteredShoppingListItems() {
        if(this.shoppingListItems == null) return null;

        ArrayList<ShoppingListItem> filteredShoppingListItems = new ArrayList<>();
        itemsMissingCount = 0;
        itemsUndoneCount = 0;

        for(ShoppingListItem shoppingListItem : this.shoppingListItems) {
            if(shoppingListItem.getShoppingListId() != getSelectedShoppingListId()) continue;
            if(shoppingListItem.isMissing()) itemsMissingCount += 1;
            if(shoppingListItem.isUndone()) itemsUndoneCount += 1;

            boolean searchContainsItem = true;
            if(searchInput != null && !searchInput.isEmpty()) {
                String name;
                String description = null;
                if(shoppingListItem.getProduct() != null) {
                    name = shoppingListItem.getProduct().getName();
                    description = shoppingListItem.getProduct().getDescription();
                } else {
                    name = shoppingListItem.getNote();
                }
                name = name != null ? name.toLowerCase() : "";
                description = description != null ? description.toLowerCase() : "";
                if(!name.contains(searchInput) && !description.contains(searchInput)) {
                    searchContainsItem = false;
                }
            }
            if(!searchContainsItem) continue;

            if(filterState == ShoppingListItemAdapter.FILTER_NOTHING
                    || filterState == ShoppingListItemAdapter.FILTER_MISSING
                    && shoppingListItem.isMissing()
                    || filterState == ShoppingListItemAdapter.FILTER_UNDONE
                    && shoppingListItem.isUndone()
            ) filteredShoppingListItems.add(shoppingListItem);
        }
        return filteredShoppingListItems;
    }

    public boolean isSearchActive() {
        return searchInput != null && !searchInput.isEmpty();
    }

    public int getFilterState() {
        return filterState;
    }

    public void onFilterChanged(int state) {
        this.filterState = state;
        updateFilteredShoppingListItems();
    }

    public void resetSearch() {
        searchInput = null;
    }

    public MutableLiveData<ArrayList<GroupedListItem>> getFilteredGroupedListItemsLive() {
        return filteredGroupedListItemsLive;
    }

    public ArrayList<GroupedListItem> getFilteredGroupedListItems() {
        return filteredGroupedListItemsLive.getValue();
    }

    public int getItemsMissingCount() {
        return itemsMissingCount;
    }

    public int getItemsUndoneCount() {
        return itemsUndoneCount;
    }

    public void updateSearchInput(String input) {
        this.searchInput = input.toLowerCase();
        updateFilteredShoppingListItems();
    }

    public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
        return selectedShoppingListIdLive;
    }

    public void downloadData(@Nullable String dbChangedTime) {
        if(currentQueueLoading != null) {
            currentQueueLoading.reset(true);
            currentQueueLoading = null;
        }
        if(isOffline()) { // skip downloading and update recyclerview
            isLoadingLive.setValue(false);
            updateFilteredShoppingListItems();
            return;
        }
        if(dbChangedTime == null) {
            dlHelper.getTimeDbChanged((DownloadHelper.OnStringResponseListener) this::downloadData, () -> onDownloadError(null));
            return;
        }

        // get last offline db-changed-time values
        String lastTimeShoppingListItems = sharedPrefs
                .getString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
        String lastTimeShoppingLists = sharedPrefs
                .getString(Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, null);
        String lastTimeProductGroups = sharedPrefs
                .getString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
        String lastTimeQuantityUnits = sharedPrefs
                .getString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
        String lastTimeProducts = sharedPrefs
                .getString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
        String lastTimeMissingItems = sharedPrefs
                .getString(Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, null);

        SharedPreferences.Editor editPrefs = sharedPrefs.edit();
        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
        if(lastTimeShoppingListItems == null || !lastTimeShoppingListItems.equals(dbChangedTime)) {
            queue.append(dlHelper.getShoppingListItems(shoppingListItems -> {
                this.shoppingListItems = shoppingListItems;
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped ShoppingListItems download");
        if(lastTimeShoppingLists == null || !lastTimeShoppingLists.equals(dbChangedTime)) {
            queue.append(dlHelper.getShoppingLists(shoppingLists -> {
                this.shoppingLists = shoppingLists;
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped ShoppingLists download");
        if(lastTimeProductGroups == null || !lastTimeProductGroups.equals(dbChangedTime)) {
            queue.append(dlHelper.getProductGroups(productGroups -> {
                this.productGroups = productGroups;
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped ProductGroups download");
        if(lastTimeQuantityUnits == null || !lastTimeQuantityUnits.equals(dbChangedTime)) {
            queue.append(dlHelper.getQuantityUnits(quantityUnits -> {
                this.quantityUnits = quantityUnits;
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped QuantityUnits download");
        if(lastTimeProducts == null || !lastTimeProducts.equals(dbChangedTime)) {
            queue.append(dlHelper.getProducts(products -> {
                this.products = products;
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped Products download");
        if(lastTimeMissingItems == null || !lastTimeMissingItems.equals(dbChangedTime)) {
            queue.append(dlHelper.getVolatile((expiring, expired, missing) -> {
                this.missingItems = missing;
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped Volatile download");

        if(queue.isEmpty()) return;

        currentQueueLoading = queue;
        queue.start();
    }

    public void downloadData() {
        downloadData(null);
    }

    private void onQueueEmpty() {
        if(isOffline()) setOfflineLive(false);
        repository.updateDatabase(
                this.shoppingListItems,
                this.shoppingLists,
                this.productGroups,
                this.quantityUnits,
                this.products,
                this.missingItems,
                (itemsToSync, serverItemHashMap) -> {
                    if(itemsToSync.isEmpty()) {
                        tidyUpItems(itemsChanged -> {
                            if(itemsChanged) {
                                downloadData();
                            } else {
                                updateFilteredShoppingListItems();
                            }
                        });
                        return;
                    }
                    DownloadHelper.OnQueueEmptyListener emptyListener = () -> {
                        ArrayList<ShoppingListItem> itemsToUpdate = new ArrayList<>();
                        for(ShoppingListItem itemToSync : itemsToSync) {
                            int itemId = itemToSync.getId();
                            ShoppingListItem itemToUpdate = serverItemHashMap.get(itemId);
                            if(itemToUpdate == null) continue;
                            itemToUpdate.setDone(itemToSync.getDone());
                            itemsToUpdate.add(itemToUpdate);
                        }
                        repository.insertShoppingListItems(
                                () -> {
                                    showMessage(getString(R.string.msg_synced));
                                    loadFromDatabase(false);
                                },
                                itemsToUpdate.toArray(new ShoppingListItem[0])
                        );
                    };
                    DownloadHelper.OnErrorListener errorListener = error -> {
                        showMessage(getString(R.string.msg_failed_to_sync));
                        downloadData();
                    };
                    DownloadHelper.Queue queue = dlHelper.newQueue(emptyListener, errorListener);
                    for(ShoppingListItem itemToSync : itemsToSync) {
                        JSONObject body = new JSONObject();
                        try {
                            body.put("done", itemToSync.getDone());
                        } catch (JSONException e) {
                            if(debug) Log.e(TAG, "syncItems: " + e);
                        }
                        queue.append(dlHelper.editShoppingListItem(itemToSync.getId(), body));
                    }
                    currentQueueLoading = queue;
                    queue.start();
                }
        );
    }

    private void onDownloadError(@Nullable VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        showMessage(getString(R.string.msg_no_connection));
        if(!isOffline()) setOfflineLive(true);
    }

    private void tidyUpItems(OnTidyUpFinishedListener onFinished) {
        // Tidy up lost shopping list items, which have deleted shopping lists
        // as an id – else they will never show up on any shopping list
        ArrayList<Integer> listIds = new ArrayList<>();
        if(isFeatureEnabled(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS)) {
            for(ShoppingList shoppingList : shoppingLists) listIds.add(shoppingList.getId());
            if(listIds.isEmpty()) {
                if(onFinished != null) onFinished.run(false);
                return;  // possible if download error happened
            }
        } else {
            listIds.add(1);  // id of first and single shopping list
        }

        DownloadHelper.Queue queue = dlHelper.newQueue(
                () -> { if(onFinished != null) onFinished.run(true); },
                error -> { if(onFinished != null) onFinished.run(true); }
        );
        for(ShoppingListItem listItem : shoppingListItems) {
            if(listIds.contains(listItem.getShoppingListId())) continue;
            if(debug) Log.i(TAG, "tidyUpItems: " + listItem);
            queue.append(dlHelper.deleteShoppingListItem(listItem.getId()));
        }
        if(queue.getSize() == 0) {
            onFinished.run(false);
            return;
        }
        currentQueueLoading = queue;
        queue.start();
    }

    private interface OnTidyUpFinishedListener {
        void run(boolean itemsChanged);
    }

    public int getSelectedShoppingListId() {
        return selectedShoppingListIdLive.getValue();
    }

    public void selectShoppingList(int shoppingListId) {
        if(shoppingListId == getSelectedShoppingListId()) return;
        ShoppingList shoppingList = getSelectedShoppingList();
        if(shoppingList == null) return;
        sharedPrefs.edit().putInt(Constants.PREF.SHOPPING_LIST_LAST_ID, shoppingListId).apply();
        selectedShoppingListIdLive.setValue(shoppingListId);
        updateFilteredShoppingListItems();
    }

    public void toggleDoneStatus(int movedPosition) { // movedPosition follows the numbering of groupedListItems (without the filter row)
        ArrayList<GroupedListItem> currentList = filteredGroupedListItemsLive.getValue();
        if(movedPosition > currentList.size()-1) {
            showErrorMessage(); // TODO: Test error messages
            return;
        }
        ShoppingListItem shoppingListItem = ((ShoppingListItem) currentList.get(movedPosition))
                .getClone();

        if(shoppingListItem.getDoneSynced() == -1) {
            shoppingListItem.setDoneSynced(shoppingListItem.getDone());
        }

        shoppingListItem.setDone(shoppingListItem.getDone() == 0 ? 1 : 0);  // toggle state

        if(isOffline()) {
            updateDoneStatus(shoppingListItem);
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("done", shoppingListItem.getDone());
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "toggleDoneStatus: " + e);
        }
        dlHelper.editShoppingListItem(
                shoppingListItem.getId(),
                body,
                response -> updateDoneStatus(shoppingListItem),
                error -> {
                    showMessage(getString(R.string.error_undefined));
                    if(debug) Log.e(TAG, "toggleDoneStatus: " + error);
                }
        ).perform(dlHelper.getUuid());
    }

    private void updateDoneStatus(ShoppingListItem shoppingListItem) {
        repository.insertShoppingListItems(
                () -> loadFromDatabase(false),
                shoppingListItem
        );
    }

    public void addMissingItems() {
        ShoppingList shoppingList = getSelectedShoppingList();
        if(shoppingList == null) {
            showMessage(getString(R.string.error_undefined));
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("list_id", getSelectedShoppingListId());
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "setUpBottomMenu: add missing: " + e);
        }
        dlHelper.post(
                grocyApi.addMissingProducts(),
                jsonObject,
                response -> {
                    showMessage(getString(
                            R.string.msg_added_missing_products,
                            shoppingList.getName()
                    ));
                },
                error -> {
                    showMessage(getString(R.string.error_undefined));
                    if(debug) Log.e(
                            TAG, "setUpBottomMenu: add missing "
                                    + shoppingList.getName()
                                    + ": " + error
                    );
                }
        );
    }

    public void saveNotes(Spanned notes) {
        JSONObject body = new JSONObject();

        String notesHtml = notes != null ? Html.toHtml(notes) : "";
        try {
            body.put("description", notesHtml);
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "saveNotes: " + e);
        }
        dlHelper.put(
                grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LISTS, getSelectedShoppingListId()),
                body,
                response -> {
                    ShoppingList shoppingList = getSelectedShoppingList();
                    if(shoppingList == null) return;
                    shoppingList.setNotes(notesHtml);
                    downloadData();
                },
                error -> {
                    showMessage(getString(R.string.error_undefined));
                    if(debug) Log.e(TAG, "saveNotes: " + error);
                    downloadData();
                }
        );
    }

    public void deleteItem(int movedPosition) {
        ArrayList<GroupedListItem> currentList = filteredGroupedListItemsLive.getValue();
        if(movedPosition > currentList.size()-1) {
            showErrorMessage(); // TODO: Test error messages
            return;
        }
        ShoppingListItem shoppingListItem = (ShoppingListItem) currentList.get(movedPosition);
        dlHelper.delete(
                grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, shoppingListItem.getId()),
                response -> loadFromDatabase(false),
                error -> {
                    showMessage(getString(R.string.error_undefined));
                    if(debug) Log.e(TAG, "deleteItem: " + error);
                }
        );
    }

    public void safeDeleteCurrentShoppingList() {
        ShoppingList shoppingList = getSelectedShoppingList();
        if(shoppingList == null) {
            showMessage(getString(R.string.error_undefined));
            return;
        }
        clearAllItems(
                shoppingList,
                () -> deleteShoppingList(shoppingList)
        );
    }

    public void deleteShoppingList(ShoppingList shoppingList) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("list_id", getSelectedShoppingListId());
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "deleteShoppingList: delete list: " + e);
        }

        dlHelper.delete(
                grocyApi.getObject(
                        GrocyApi.ENTITY.SHOPPING_LISTS,
                        shoppingList.getId()
                ),
                response -> {
                    showMessage(
                            getString(R.string.msg_shopping_list_deleted, shoppingList.getName())
                    );
                    shoppingLists.remove(shoppingList);
                    selectShoppingList(1);

                    tidyUpItems(itemsChanged -> downloadData());
                },
                error -> {
                    showMessage(getString(R.string.error_undefined));
                    if(debug) Log.e(
                            TAG, "deleteShoppingList: delete "
                                    + shoppingList.getName() + ": " + error
                    );
                    downloadData();
                }
        );
    }

    public void clearAllItems(
            ShoppingList shoppingList,
            Runnable onResponse
    ) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("list_id", getSelectedShoppingListId());
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "clearShoppingList: " + e);
        }
        dlHelper.post(
                grocyApi.clearShoppingList(),
                jsonObject,
                response -> {
                    if(onResponse != null) onResponse.run();
                },
                error -> {
                    showMessage(getString(R.string.error_undefined));
                    if(debug) Log.e(
                            TAG, "clearShoppingList: "
                                    + shoppingList.getName()
                                    + ": " + error
                    );
                }
        );
    }

    public void clearDoneItems(ShoppingList shoppingList) {
        DownloadHelper.Queue queue = dlHelper.newQueue(
                () -> {
                    showMessage(
                            getString(
                                    R.string.msg_shopping_list_cleared,
                                    shoppingList.getName()
                            )
                    );
                    downloadData();
                }, volleyError -> {
                    showMessage(getString(R.string.error_undefined));
                    downloadData();
                }
        );
        for(ShoppingListItem shoppingListItem : shoppingListItems) {
            if(shoppingListItem.getShoppingListId() != shoppingList.getId()) continue;
            if(shoppingListItem.getDone() == 0) continue;
            queue.append(dlHelper.deleteShoppingListItem(shoppingListItem.getId()));
        }
        queue.start();
    }

    @Nullable
    public ShoppingList getShoppingListFromId(int id) {
        if(shoppingLists == null) return null;
        for(ShoppingList temp : shoppingLists) {
            if(temp.getId() == id) return temp;
        }
        return null;
    }

    @Nullable
    public ShoppingList getSelectedShoppingList() {
        return getShoppingListFromId(getSelectedShoppingListId());
    }

    @Nullable
    public ShoppingListItem getShoppingListItemAtPos(int position) { // from current GroupedListItems
        ArrayList<GroupedListItem> groupedListItems = filteredGroupedListItemsLive.getValue();
        if(groupedListItems == null) return null;
        if(position > groupedListItems.size()-1) return null;
        return (ShoppingListItem) groupedListItems.get(position);
    }

    public boolean isDataLoaded() {
        return shoppingLists != null && shoppingListItems != null
                && productGroups != null && quantityUnits != null;
    }

    @Nullable
    public ArrayList<ShoppingList> getShoppingLists() {
        return shoppingLists;
    }

    public QuantityUnit getQuantityUnitFromId(int id) {
        if(quantityUnits == null) return null;
        for(QuantityUnit quantityUnit : quantityUnits) {
            if(quantityUnit.getId() == id) {
                return quantityUnit;
            }
        } return null;
    }

    @NonNull
    public MutableLiveData<Boolean> getOfflineLive() {
        return offlineLive;
    }

    public Boolean isOffline() {
        return offlineLive.getValue();
    }

    public void setOfflineLive(boolean isOffline) {
        offlineLive.setValue(isOffline);
    }

    @NonNull
    public MutableLiveData<Boolean> getIsLoadingLive() {
        return isLoadingLive;
    }

    @NonNull
    public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
        return infoFullscreenLive;
    }

    public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
        currentQueueLoading = queueLoading;
    }

    private void showErrorMessage() {
        showMessage(getString(R.string.error_undefined));
    }

    private void showMessage(@NonNull String message) {
        showSnackbar(new SnackbarMessage(message));
    }

    private void showSnackbar(@NonNull SnackbarMessage snackbarMessage) {
        eventHandler.setValue(snackbarMessage);
    }

    private void sendEvent(int type) {
        eventHandler.setValue(new Event() {
            @Override
            public int getType() {return type;}
        });
    }

    private void sendEvent(int type, Bundle bundle) {
        eventHandler.setValue(new Event() {
            @Override
            public int getType() {return type;}

            @Override
            public Bundle getBundle() {return bundle;}
        });
    }

    @NonNull
    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public boolean isFeatureEnabled(String pref) {
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    private String getString(@StringRes int resId) {
        return getApplication().getString(resId);
    }

    private String getString(@StringRes int resId, Object... formatArgs) {
        return getApplication().getString(resId, formatArgs);
    }

    @Override
    protected void onCleared() {
        dlHelper.destroy();
        super.onCleared();
    }
}
