package com.cyanogenmod.id.setup;

public interface SetupDataCallbacks {
    void onPageLoaded(Page page);
    void onPageTreeChanged();
    void onPageFinished(Page page);
    Page getPage(String key);
}
