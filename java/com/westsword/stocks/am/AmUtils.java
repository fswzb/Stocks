package com.westsword.stocks.am;

import java.util.*;

import com.westsword.stocks.base.Stock;
import com.westsword.stocks.base.Utils;
import com.westsword.stocks.base.time.*;
import com.westsword.stocks.base.utils.StockPaths;
import com.westsword.stocks.analyze.RawTradeDetails;
import com.westsword.stocks.analyze.RawTradeDetailsList;


public class AmUtils {
    public AmUtils(String stockCode) {
        mStockCode = stockCode;
        mSdTime = new SdTime1(stockCode);   //get sdStartDate&sdStartTime&interval from settings.txt
        mStockDates = new StockDates(stockCode);
    }

    public void writeAllAmRecords() {
        writeAmRecords(mStockDates.firstDate(), mStockDates.lastDate());
    }
    public void writeAmRecords(String tradeDate1, String tradeDate2) {
        tradeDate1 = mStockDates.ceiling(tradeDate1);
        tradeDate2= mStockDates.floor(tradeDate2);
        long startAm = loadPrevLastAm(tradeDate1);
        while(tradeDate1!=null&&tradeDate2!=null&&tradeDate1.compareTo(tradeDate2)<=0) {
            if(!mStockDates.isMissingDate(tradeDate1)) {
                startAm = writeAmRecords(startAm, tradeDate1);
            }

            tradeDate1 = mStockDates.nextDate(tradeDate1);
        }
    }
    public long writeAmRecords(long startAm, String tradeDate) {
        //any tp later than tradeDate's close_quotation_time is not written
        long closeTP = Time.getSpecificTime(tradeDate, AStockSdTime.getCloseQuotationTime());
        System.out.format("%s: closeTP=%x\n", Utils.getCallerName(getClass()), closeTP);
        
        ArrayList<RawTradeDetails> rawDetailsList = loadRawTradeDetails(tradeDate);
        int lSize = rawDetailsList.size();
        TrackExtreme ter = new TrackExtreme(rawDetailsList);

        //reset sAnalysisFile
        String sAnalysisFile = StockPaths.getAnalysisFile(mStockCode, tradeDate);
        Utils.deleteFile(sAnalysisFile);

        long am = startAm;
        long caeTp = Time.getSpecificTime(tradeDate, mSdTime.getCallAuctionEndTime0());
        int caeSd = mSdTime.getAbs(caeTp);
        int prevSd = caeSd;                //prevSd starts from callAuctionEndTime
        for(int i=0; i<lSize; i++) {
            RawTradeDetails r = rawDetailsList.get(i);
            //System.out.format("%s: %x\n", Utils.getCallerName(getClass()), r.time);
            int rSd = mSdTime.getAbs(r.time);
            if(rSd != prevSd) {
                //write AmRecords between [prevSd, rSd) to sAnalysisFile
                writeRange(prevSd, rSd, am, ter, sAnalysisFile, closeTP);

                prevSd = rSd;
                ter.resetEx();
            }
            if(r.type == Stock.TRADE_TYPE_UP) {
                am += r.count;
                ter.traceUp(r.price);
            } else {
                am -= r.count;
                ter.traceDown(r.price);
            }
        }
        //last record
        if(lSize!=0) {
            System.out.format("%s: %d\n", Utils.getCallerName(getClass()), prevSd);
            writeRange(prevSd, prevSd+1, am, ter, sAnalysisFile, closeTP);
        }

        return am;
    }
    public void writeRange(int start, int end, long am, TrackExtreme ter, 
            String sAnalysisFile, long closeTP, TreeMap<Integer, AmRecord> amRecordMap) {
        ter.setEx2Prev();
        for(int i=start; i<end; i++) {
            long tp = mSdTime.rgetAbs(i);
            if(tp<=closeTP) {
                //String tradeDate = Time.getTimeYMD(tp);
                //String tradeTime = Time.getTimeHMS(tp);
                AmRecord r = new AmRecord(tp, i, am, ter.maxUP, ter.minDP);
                if(amRecordMap!=null) {
                    amRecordMap.put(i, r);
                }
                r.append2File(sAnalysisFile);
            }
        }
    }
    public void writeRange(int start, int end, long am, 
            TrackExtreme ter, String sAnalysisFile, long closeTP) {
        writeRange(start, end, am, ter, sAnalysisFile, closeTP, null);
    }
    private ArrayList<RawTradeDetails> loadRawTradeDetails(String tradeDate) {
        //load rawTradeDetails to rawDetailsList
        String sRawTradeDetailsFile = StockPaths.getRawTradeDetailsFile(mStockCode, tradeDate);
        RawTradeDetailsList rtdList = new RawTradeDetailsList();
        ArrayList<RawTradeDetails> rawDetailsList = new ArrayList<RawTradeDetails>();
        rtdList.load(rawDetailsList, sRawTradeDetailsFile);

        return rawDetailsList;
    }

    public AmRecord loadPrevLastAmRecord(String tradeDate) {
        if(!mStockDates.contains(tradeDate)) {
            String msg = String.format("%s is not valid for %s\n", 
                    tradeDate, mStockCode);
            throw new RuntimeException(msg);
        }

        if(tradeDate.equals(mStockDates.firstDate()))
            return null;

        tradeDate = mStockDates.prevDate(tradeDate);
        String sAnalysisFile = StockPaths.getAnalysisFile(mStockCode, tradeDate);
        AmRecordLoader amLoader = new AmRecordLoader();
        ArrayList<AmRecord> amList = new ArrayList<AmRecord>();
        amLoader.load(amList, null, null, sAnalysisFile);

        if(amList.size() == 0)
            return null;

        return amList.get(amList.size()-1);
    }
    public long loadPrevLastAm(String tradeDate) {
        long lastAm = 0;

        AmRecord r = loadPrevLastAmRecord(tradeDate);
        if(r != null)
            lastAm = r.am;
        //System.out.format("lastAm=%d\n", lastAm);

        return lastAm;
    }




    private String mStockCode;
    private StockDates mStockDates;
    private SdTime1 mSdTime;

    public static class TrackExtreme {
        public double maxUP;
        public double minDP;
        public double prevMaxUP;
        public double prevMinDP;

        public TrackExtreme() {
            this(new ArrayList<RawTradeDetails>());
        }
        public TrackExtreme(ArrayList<RawTradeDetails> rawDetailsList) {
            maxUP=Double.NEGATIVE_INFINITY; 
            prevMaxUP=Double.NaN;
            minDP=Double.POSITIVE_INFINITY; 
            prevMinDP=Double.NaN;
            if(rawDetailsList.size()>0) {
                RawTradeDetails r = rawDetailsList.get(0);
                prevMaxUP=r.price; prevMinDP=r.price;
            }
        }
        public void traceUp(double price) {
                if(price>=maxUP) {
                    maxUP = price;
                    prevMaxUP = maxUP;
                }
        }
        public void traceDown(double price) {
                if(price<=minDP) {
                    minDP = price;
                    prevMinDP = minDP;
                }
        }
        public void resetEx() {
                maxUP = Double.NEGATIVE_INFINITY;
                minDP = Double.POSITIVE_INFINITY;
        }
        public void setEx2Prev() {
            maxUP = maxUP==Double.NEGATIVE_INFINITY? prevMaxUP:maxUP;
            minDP = minDP==Double.POSITIVE_INFINITY? prevMinDP:minDP;
        }
    }



    public static double[][] getAmMatrix(String stockCode, String startDate, String hmsList) {
        AmManager am = new AmManager(stockCode, true);

        String[] sTradeDates = new TradeDates(stockCode, startDate).getAllDates();
        double[][] m = am.getAmMatrix(hmsList, sTradeDates);

        return m;
    }
}
