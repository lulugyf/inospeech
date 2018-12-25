package com.laog.test1;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.laog.test1.ag.CustomAdapter;
import com.laog.test1.ag.DataModel;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class YTListActivity extends Activity implements View.OnClickListener {

    private final String TAG = "YT";

    ArrayList<DataModel> dataModels;
    ListView listView;
    private static CustomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ag_activity_main);

        listView = (ListView)findViewById(R.id.list);

        dataModels= new ArrayList<>();

        dataModels.add(new DataModel("Apple Pie", "Android 1.0", "1","September 23, 2008"));
        dataModels.add(new DataModel("Banana Bread", "Android 1.1", "2","February 9, 2009"));
        dataModels.add(new DataModel("Cupcake", "Android 1.5", "3","April 27, 2009"));
        dataModels.add(new DataModel("Donut","Android 1.6","4","September 15, 2009"));
        dataModels.add(new DataModel("Eclair", "Android 2.0", "5","October 26, 2009"));
        dataModels.add(new DataModel("Froyo", "Android 2.2", "8","May 20, 2010"));
        dataModels.add(new DataModel("Gingerbread", "Android 2.3", "9","December 6, 2010"));
        dataModels.add(new DataModel("Honeycomb","Android 3.0","11","February 22, 2011"));
        dataModels.add(new DataModel("Ice Cream Sandwich", "Android 4.0", "14","October 18, 2011"));
        dataModels.add(new DataModel("Jelly Bean", "Android 4.2", "16","July 9, 2012"));
        dataModels.add(new DataModel("Kitkat", "Android 4.4", "19","October 31, 2013"));
        dataModels.add(new DataModel("Lollipop","Android 5.0","21","November 12, 2014"));
        dataModels.add(new DataModel("Marshmallow", "Android 6.0", "23","October 5, 2015"));

        adapter = new CustomAdapter(dataModels, getApplicationContext());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                DataModel dataModel= dataModels.get(position);

                Snackbar.make(view, dataModel.getName()+"\n"+dataModel.getType()+" API: "+dataModel.getVersion_number(), Snackbar.LENGTH_LONG)
                        .setAction("No action", null).show();
            }
        });

        ((Button)findViewById(R.id.button)).setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ag_menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == R.id.action_rx){
            rxtest();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * https://www.coderefer.com/rxandroid-tutorial-getting-started/
     * https://www.coderefer.com/rxandroid-example-tutorial/
     * https://medium.com/@kurtisnusbaum/rxandroid-basics-part-1-c0d5edcf6850
     */
    private void rxtest() {
        Observable<String> stringObservable = Observable.just("Hello Reactive Programming!");

        Observer<String> stringObserver = new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(String s) {
                toast(s);
                Log.d(TAG, "---haha:"+s);
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        };

        stringObservable.subscribe(stringObserver);
    }

    private void toast(String msg) {
        Toast.makeText(YTListActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    Observable mObservable;
    Observer<Integer> mObserver;
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.button){
            rxtest1();
        }
    }

    private void rxtest1() {
        mObservable = Observable.create(e -> {
            for(int i=1; i<=5;i++){
                e.onNext(i);
            }
            e.onComplete();
        });
        mObserver = new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                toast("onSubscribe called");
            }

            @Override
            public void onNext(Integer integer) {
                toast("onNext called: " + integer);
            }

            @Override
            public void onError(Throwable e) {
                toast("onError called");
            }

            @Override
            public void onComplete() {
                toast("onComplete called");
            }
        };
    }
}
