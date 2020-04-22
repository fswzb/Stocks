package com.westsword.stocks.analyze;


import java.util.*;

import com.westsword.stocks.am.*;
import com.westsword.stocks.base.Stock;
import com.westsword.stocks.base.Utils;
import com.westsword.stocks.base.Settings;
import com.westsword.stocks.base.time.*;
import com.westsword.stocks.base.utils.StockPaths;

public class Analyze600030 {
    public final static int LAST_RAW_DETAILS_IDX   = 0;
    public final static int LAST_RAW_PANKOU_IDX    = 1;

    private final RealtimeAnalyze mRTAnalyzeFrame;
    private final String mAnalysisFile;
    private final long mCloseTP;
    private final SdTime1 mSdTime;
    private final AmUtils mAmu;
    private final AmUtils.TrackExtreme mTer;
    private final ArrayList<SSTableRecord> mSSTableRecordList;
    private final AmManager mAm;
    private final int[] mIndexs = {
        -1,               //raw details idx processed last time               LAST_RAW_DETAILS_IDX
        -1,               //raw pankou idx processed last time                LAST_RAW_PANKOU_IDX 
    };

    private long mStartAm;
    private TreeMap<Integer, AmRecord> mAmRecordMap;


    Analyze600030(RealtimeAnalyze rtAnalyzeFrame) {
        String stockCode = Settings.getStockCode();
        String tradeDate = Settings.getTradeDate();

        mRTAnalyzeFrame = rtAnalyzeFrame;
        //set mAnalysisFile, and reset it
        mAnalysisFile = getAnalysisFile(mRTAnalyzeFrame);
        Utils.deleteFile(mAnalysisFile);
        //set mCloseTP
        mCloseTP = Time.getSpecificTime(tradeDate, AStockSdTime.getCloseQuotationTime());

        //set mSdTime
        mSdTime = new SdTime1(stockCode);
        //set mAmu
        mAmu = new AmUtils(stockCode);
        //set mTer
        mTer = new AmUtils.TrackExtreme();
        //set mSSTableRecordList
        mSSTableRecordList = new ArrayList<SSTableRecord>();
        loadSSTable(mSSTableRecordList);
        //set mAm
        ArrayList<String> tradeDateList = SSTableRecord.getTradeDates(mSSTableRecordList);
        StockDates stockDates = new StockDates(stockCode);
        mAm = new AmManager(stockCode, tradeDateList);

        //set mStartAm
        mStartAm = mAmu.loadPrevLastAm(tradeDate);
        //set mAmRecordMap
        mAmRecordMap = new TreeMap<Integer, AmRecord>();
    }
    private void loadSSTable(ArrayList<SSTableRecord> sstrList) {
        SSTableLoader loader = new SSTableLoader();
        String sSSTable = StockPaths.getSSTableFile();
        loader.load(sstrList, sSSTable);
        //System.out.format("%s: size=%d\n", Utils.getCallerName(getClass()), sstrList.size());
    }


    private boolean bCallAuctionComplete = false;
    public void startAnalyze(ArrayList<RawTradeDetails> rawDetailsList, 
            ArrayList<RawRTPankou> rawPankouList) {
        bCallAuctionComplete = callAuctionCompleted(rawDetailsList, bCallAuctionComplete);

        processRawTradeDetails(mIndexs, rawDetailsList);
        processRawPankou(mIndexs, rawPankouList);

        //ckpt actions here

        if(isLastRawTradeDetailHandled()||isLastPankouHandled()) {
            System.out.format("%s: isLastRawTradeDetailHandled=%b, isLastPankouHandled=%b\n", 
                    Utils.getCallerName(getClass()), isLastRawTradeDetailHandled(), isLastPankouHandled());
        }
    }

    private void writeRange(int start, int end, long am, AmUtils.TrackExtreme ter, 
            String sAnalysisFile, long closeTP, TreeMap<Integer, AmRecord> amRecordMap) {
        mAmu.writeRange(start, end, am, mTer, 
                mAnalysisFile, mCloseTP, amRecordMap);
    }
    private void processRawTradeDetails(int[] indexs, ArrayList<RawTradeDetails> rawDetailsList) {
        int last = indexs[LAST_RAW_DETAILS_IDX];
        int current = rawDetailsList.size()-1;
        //prevSd starts from LAST_RAW_DETAILS_IDX; which can be -1(invalid) or valid ones
        int prevSd = getSdTime(last, rawDetailsList);
        long am = mStartAm;

        while(current > last) {
            last++;
            RawTradeDetails r = rawDetailsList.get(last);
            int rSd = mSdTime.getAbs(r.time);
            if(rSd != prevSd) {
                //skip writeRange if prevSd==-1
                if(prevSd != -1)
                    writeRange(prevSd, rSd, am, mTer, 
                            mAnalysisFile, mCloseTP, mAmRecordMap);
                prevSd = rSd;
                mTer.resetEx();
            }
            if(r.type == Stock.TRADE_TYPE_UP) {
                am += r.count;
                mTer.traceUp(r.price);
            } else {
                am -= r.count;
                mTer.traceDown(r.price);
            }
        }

        mStartAm = am;
        indexs[LAST_RAW_DETAILS_IDX] = current;
        checkLastRawTradeDetail(indexs, rawDetailsList);
    }

    private void processRawPankou(int[] indexs, ArrayList<RawRTPankou> rawPankouList) {
        int last = indexs[LAST_RAW_PANKOU_IDX];
        int current = rawPankouList.size()-1;
        while(current > last) {
            last++;
            handleRawPankou(rawPankouList.get(last));
        }

        indexs[LAST_RAW_PANKOU_IDX] = current;
        checkLastPankou(indexs, rawPankouList);
    }
    private void handleRawPankou(RawRTPankou raw) {
    }


    private boolean mLastRawTradeDetailHandled = false;
    public boolean isLastRawTradeDetailHandled() {
        return mLastRawTradeDetailHandled;
    }
    public void setLastRawTradeDetailHandled(boolean bLast) {
        mLastRawTradeDetailHandled = bLast;
    }
    private boolean mLastPankouHandled = false;
    public boolean isLastPankouHandled() {
        return mLastPankouHandled;
    }
    public void setLastPankouHandled(boolean bLast) {
        mLastPankouHandled = bLast;
    }
    //set it if LAST_RAW_PANKOU_IDX >15:00:00
    private void checkLastPankou(int[] indexs, ArrayList<RawRTPankou> rawPankouList) {
        int idx = indexs[LAST_RAW_PANKOU_IDX];
        if(idx != -1) {
            long timePt = rawPankouList.get(idx).mSecondsFrom1970Time;

            if(timePt > AStockSdTime.getCloseQuotationTime(timePt))
                setLastPankouHandled(true);
        }
    }
    //set it if LAST_RAW_DETAILS_IDX == 15:01:00
    private void checkLastRawTradeDetail(int[] indexs, ArrayList<RawTradeDetails> rawDetailsList) {
        int idx = indexs[LAST_RAW_DETAILS_IDX];
        if(idx != -1) {
            long timePt = rawDetailsList.get(idx).time;

            if(timePt == AStockSdTime.getLastRawTradeDetailTime(timePt))
                setLastRawTradeDetailHandled(true);
        }
    }


    private boolean callAuctionCompleted(ArrayList<RawTradeDetails> rawDetailsList, 
            boolean completed) {
        //get the ratio of ab/(ab+as) when call auction completes
        if(!completed && rawDetailsList.size() != 0) {
            RawTradeDetails current = rawDetailsList.get(rawDetailsList.size()-1);
            long callAuctionTime = AStockSdTime.getCallAuctionEndTime(current.time);

            if(current.time >= callAuctionTime) {
                int asSum=0, abSum=0;
                //only include those tradeDetails <= callAuctionTime
                //sum as&ab during callAuction;
                for(int i=0; i<rawDetailsList.size(); i++) {
                    RawTradeDetails element = rawDetailsList.get(i);
                    if(element.time > callAuctionTime)
                        break;

                    if(element.type == Stock.TRADE_TYPE_UP)
                        abSum += element.count;
                    else if(element.type == Stock.TRADE_TYPE_DOWN)
                        asSum += element.count;
                    else
                        System.out.format("%s: unsupported type = %d\n", 
                                Utils.getCallerName(getClass()), element.type);
                }

                String sFormat = "\nCallAuction Completed! ab/(ab+as) = %8.3f%%\n";
                System.out.format(sFormat, (double)abSum/(abSum+asSum)*100);
                completed= true;
            }
        }

        return completed;
    }
    private String getAnalysisFile(RealtimeAnalyze frame) {
        String sAnalysisFile = frame.getAnalysisFile();

        if(!sAnalysisFile.equals("")) {
            return sAnalysisFile;
        }

        return StockPaths.getAnalysisFile();
    }
    private int getSdTime(int idx, ArrayList<RawTradeDetails> rawDetailsList) {
        if(idx == -1)
            return -1;

        RawTradeDetails r = rawDetailsList.get(idx);
        
        return mSdTime.getAbs(r.time); 
    }
}

