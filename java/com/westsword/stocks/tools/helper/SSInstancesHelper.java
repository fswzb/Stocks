package com.westsword.stocks.tools.helper;


import java.util.*;
import org.apache.commons.cli.*;
import org.apache.commons.math3.util.Combinations;

import com.westsword.stocks.am.*;
import com.westsword.stocks.base.time.*;
import com.westsword.stocks.base.ckpt.*;
import com.westsword.stocks.tools.helper.man.*;
import com.westsword.stocks.base.utils.AnsiColor;

public class SSInstancesHelper {

    public void run(String args[]) {
        CommandLine cmd = getCommandLine(args);
        String[] newArgs = cmd.getArgs();
        if(newArgs.length<2) {
            usage();
            return;
        }

        String stockCode = SSUtils.getStockCode(cmd);
        String startDate = SSUtils.getStartDate(cmd);
        double threshold = SSUtils.getThreshold(cmd);
        int sTDistance = SSUtils.getNearestOutDist(cmd);
        int tradeType = SSUtils.getTradeType(cmd);

        double targetRate = Double.valueOf(newArgs[1]);
        if(!SSUtils.checkTargetRate(newArgs[1])) {
            usage();
            return;
        }
        int maxCycle = Integer.valueOf(newArgs[0]);

        String sTradeSumFile = SSUtils.getTradeSumFile(cmd);
        String hmsList = SSUtils.getHMSList(cmd);
        if(sTradeSumFile!=null&&hmsList!=null) {
            usage();
            return;
        }

        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        //
        boolean bResetLog = SSUtils.getSwitchResetLog(cmd);
        boolean bLog2Files = SSUtils.getSwitchLog2File(cmd);
        boolean bStdout = SSUtils.getSwitchStdout(cmd);
        AmManager am = new AmManager(stockCode, startDate, true);
        StockDates stockDates = new StockDates(stockCode);
        SSiManager ssim = new SSiManager();
        SSInstance template = new SSInstance(stockCode, startDate, threshold, sTDistance, tradeType,
                    "", "", maxCycle, targetRate);

        if(sTradeSumFile != null) {
            ArrayList<TradeSum> list = getTradeSumList(sTradeSumFile);
            handle01(template, list,
                    bLog2Files, bResetLog, bStdout,
                    stockDates, am, ssim);
        } else {
            handleX0(template, hmsList, 
                    bLog2Files, bResetLog, bStdout,
                    stockDates, am, ssim, cmd);
        }
    }

    private static void handle01(SSInstance template, ArrayList<TradeSum> list, 
            boolean bLog2Files, boolean bResetLog, boolean bStdout,
            StockDates stockDates, AmManager am, SSiManager ssim) {
        //loop the tradeSumList 
        for(int i=0; i<list.size(); i++) {
            TradeSum ts = list.get(i);

            SSInstance r = new SSInstance(template);
            r.tradeDate = ts.tradeDate;
            r.hmsList = ts.hmsList;
            System.out.format("%8s %8s %8.2f %4d %4d %8s %8s %4d %8.3f\n",
                    r.stockCode, r.startDate, r.threshold, r.sTDistance, r.tradeType,
                    ts.tradeDate, ts.hmsList, r.maxCycle, r.targetRate);

            ssim.run(r, am, stockDates,
                    bLog2Files, bResetLog, bStdout);
        }
    }
    private static void handleX0(SSInstance template, String hmsList,
            boolean bLog2Files, boolean bResetLog, boolean bStdout,
            StockDates stockDates, AmManager am, SSiManager ssim, CommandLine cmd) {
        CmManager cm = new CmManager();

        TradeDates tradeDates = new TradeDates(template.stockCode, template.startDate);
        if(hmsList!=null) {
            loopTradeDates(template, hmsList, tradeDates,
                    bLog2Files, bResetLog, bStdout,
                    stockDates, am, ssim, cm);
        } else {
            CheckPoint0 ckpt = new CheckPoint0();
            String startHMSList = SSUtils.getStartHMSList(cmd);
            int[] startIdxs = null;
            if(startHMSList!=null)
                startIdxs = ckpt.getIdxList(startHMSList);
            String endHMSList = SSUtils.getEndHMSList(cmd);
            int[] endIdxs = null;
            if(endHMSList!=null)
                endIdxs = ckpt.getIdxList(endHMSList);

            cm.startWorker(template.stockCode, template.startDate, 
                    startHMSList, endHMSList, am);

            int length = ckpt.getLength();
            Combinations c = new Combinations(length, 2);
            Comparator<int[]> iC = c.comparator();
            //loop hmsList combination(n,2)
            Iterator<int[]> itr = c.iterator();
            while(itr.hasNext()) {
                int[] e = itr.next();
                hmsList = ckpt.getHMSList(e);
                if(startIdxs!=null && iC.compare(e, startIdxs)<0) {
                    System.out.format("handleX0: skipping %s\n", hmsList);
                    continue;
                }
                if(endIdxs!=null && iC.compare(e, endIdxs)>=0) {
                    break;
                }

                //clear am's trBuf(optionaly)
                loopTradeDates(template, hmsList, tradeDates,
                        bLog2Files, bResetLog, bStdout,
                        stockDates, am, ssim, cm);
            }
            System.out.format("handleX0: %s\n", "finished!");
        }

        cm.close();
    }
    private static void loopTradeDates(SSInstance template, String hmsList, TradeDates tradeDates,
            boolean bLog2Files, boolean bResetLog, boolean bStdout,
            StockDates stockDates, AmManager am, SSiManager ssim, CmManager cm) {
        double[][] corrM = cm.getCorrMatrix(template.stockCode, template.startDate, hmsList, am);
        String tradeDate = tradeDates.firstDate();
        while(tradeDate!=null) {
            SSInstance r = new SSInstance(template);
            r.tradeDate = tradeDate;
            r.hmsList = hmsList;
            ssim.run(r, am, stockDates, corrM,
                    bLog2Files, bResetLog, bStdout);

            tradeDate = tradeDates.nextDate(tradeDate);
        }
    }

    private static ArrayList<TradeSum> getTradeSumList(String sTradeSumFile) {
        ArrayList<TradeSum> list = new ArrayList<TradeSum>();

        if(sTradeSumFile != null) {
            TradeSumLoader l = new TradeSumLoader();
            l.load(sTradeSumFile, list);
        }

        return list;
    }


    private static void usage() {
        String sPrefix = "usage: java AnalyzeTools ";
        System.err.println(sPrefix+"ssinstances [-rnocdhtsfm] maxCycle targetRate");
        System.err.println("       targetRate      ; something like [0-9]{1,}.[0-9]{1,3}");
        System.err.println("                       relative(<=1): targetRate"); 
        System.err.println("                       absolute(>1) : targetRate-1");

             String line = "       only one, either -f or -m or None can be enabled!";
        line = AnsiColor.getColorString(line, AnsiColor.ANSI_RED);
        System.err.println(line);
        SSInstanceHelper.commonUsageInfo();

        System.err.println("       -f sTradeSumFile; [tradeDate, hmsList] in the file; exclusive to -m");
        System.err.println("       -m hmsList      ; loop all [tradeDate, hmsList]; exclusive to -f");
        System.err.println("       -a startHMSList ; loop from startHMSList(inclusive)");
        System.err.println("       -b endHMSList   ; loop until endHMSList(exclusive)");
        System.err.println("                         effective only when no -fm");
        System.exit(-1);
    }

    public static CommandLine getCommandLine(String[] args) {
        CommandLine cmd = null;
        try {
            String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
            Options options = SSInstanceHelper.getOptions();

            options.addOption("f", true,  "a tradeSum file");
            options.addOption("m", true,  "hmsList");
            options.addOption("a", true,  "the start hmsList(inclusive) to be looped");
            options.addOption("b", true,  "the end hmsList(exclusive) to be looped");
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, newArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cmd;
    }

}
