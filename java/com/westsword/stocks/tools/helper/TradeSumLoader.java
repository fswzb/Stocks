package com.westsword.stocks.tools.helper;


import java.util.*;

import com.westsword.stocks.utils.*;

public class TradeSumLoader extends FileLoader {
    private String mText = null;
    private String mHMSList = null;

    private ArrayList<TradeSum> mList = null;


    private void setVars(String hmsList, String sText) {
        mHMSList = hmsList;
        mText = sText;
    }

    public boolean onLineRead(String line, int count) {
        if(line.matches("^ *#.*")||line.matches("^ *$"))
            return true;

        String[] fields=line.split(" +");
        if(mHMSList!=null&&mText!=null) {
            if(!fields[8].equals(mHMSList)) {
                mText += line + "\n";
            } 
        }
        if(mList != null) {
            TradeSum r = new TradeSum(fields);
            mList.add(r);
        }

        return true;
    }

    public void load(String sTradeSumeFile, String hmsList, String out[]) {
        setVars(hmsList, "");
        load(sTradeSumeFile);
        setVars(null, null);
        
        out[0] = mText;
    }

    public void load(String sTradeSumeFile, ArrayList<TradeSum> list) {
        mList = list;
        load(sTradeSumeFile);
        mList = null;
    }
}
