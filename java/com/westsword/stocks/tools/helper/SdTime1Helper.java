package com.westsword.stocks.tools.helper;


import com.westsword.stocks.base.time.*;

public class SdTime1Helper {
    public void getRel(String args[]) {
        if(args.length != 3) {
            usage();
            return;
        }

        String stockCode = args[1];
        String hmsList = args[2];
        String[] fields = hmsList.split("_");

        String sSdTime = "";
        SdTime1 sdTime = new SdTime1(stockCode);
        for(int i=0; i<fields.length; i++) {
            String hms = fields[i];
            int sd = sdTime.get(hms);
            //System.out.format("i=%d sd=%d\n", i, sd);
            sSdTime += String.format("%8d ", sd);
        }

        System.out.format("%s\n", sSdTime);
    }
    public void getAbs(String args[]) {
        if(args.length != 4) {
            getabsUsage();
            return;
        }

        String stockCode = args[1];
        String tradeDate = args[2];
        String hmsList = args[3];
        String[] fields = hmsList.split("_");

        String sSdTime = "";
        SdTime1 sdTime = new SdTime1(stockCode);
        for(int i=0; i<fields.length; i++) {
            String hms = fields[i];
            int sd = sdTime.getAbs(tradeDate, hms);
            //System.out.format("i=%d sd=%d\n", i, sd);
            sSdTime += String.format("%8d ", sd);
        }

        System.out.format("%s\n", sSdTime);
    }
    public void rgetAbs(String args[]) {
        if(args.length != 3) {
            rgetabsUsage();
            return;
        }

        String stockCode = args[1];
        Integer abssdtime = Integer.valueOf(args[2]);

        SdTime1 sdTime = new SdTime1(stockCode);
        long tp = sdTime.rgetAbs(abssdtime);
        
        System.out.format("%x\n", tp);
    }





    private static void usage() {
        System.err.println("usage: java AnalyzeTools getrel stockCode hmsList");
        System.exit(-1);
    }
    private static void getabsUsage() {
        System.err.println("usage: java AnalyzeTools getabs stockCode tradeDate hmsList");
        System.exit(-1);
    }
    private static void rgetabsUsage() {
        System.err.println("usage: java AnalyzeTools rgetabs stockCode abssdtime");
        System.exit(-1);
    }
}
