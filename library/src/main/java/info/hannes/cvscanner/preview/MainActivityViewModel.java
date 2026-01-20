package info.hannes.cvscanner.preview;

import android.os.Handler;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import timber.log.Timber;

public class MainActivityViewModel extends ViewModel {

    private MutableLiveData<List<String>> fruitList;

    public LiveData<List<String>> getFruitList() {
        if (fruitList == null) {
            fruitList = new MutableLiveData<>();
            loadFruits();
        }
        return fruitList;
    }

    private void loadFruits() {
        // do async operation to fetch users
        Handler myHandler = new Handler();
        myHandler.postDelayed(() -> {
            List<String> fruitsStringList = new ArrayList<>();
            fruitsStringList.add("Mango");
            fruitsStringList.add("Apple");
            fruitsStringList.add("Orange");
            fruitsStringList.add("Banana");
            fruitsStringList.add("Grapes");
            long seed = System.nanoTime();
            Collections.shuffle(fruitsStringList, new Random(seed));

            fruitList.setValue(fruitsStringList);
        }, 5000);

    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Timber.d("on cleared called");
    }

}