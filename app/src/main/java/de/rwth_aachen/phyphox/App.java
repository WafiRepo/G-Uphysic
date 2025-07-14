package de.rwth_aachen.phyphox;

import android.app.Application;

import androidx.multidex.MultiDexApplication;

import de.rwth_aachen.phyphox.model.DataModel;

//This extension to application is only used to store measured data in memory as this may easily exceed the amount of data allowed on the transaction stack

public class App extends MultiDexApplication {
    public PhyphoxExperiment experiment = null;
    private DataModel dataModel;

    public DataModel getDataModel() {
        if(dataModel== null){
            return new DataModel();
        }
        return dataModel;
    }

    public void setDataModel(DataModel dataModel) {
        this.dataModel = dataModel;
    }
}
