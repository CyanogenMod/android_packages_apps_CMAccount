package com.cyanogenmod.id.setup;

import java.util.ArrayList;

public class PageList extends ArrayList<Page> implements PageNode {

    public PageList(Page... pages) {
        for (Page page : pages) {
            add(page);
        }
    }

    @Override
    public Page findPage(String key) {
        for (Page childPage : this) {
            Page found = childPage.findPage(key);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

}
