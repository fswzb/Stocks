 /*
 Copyright (C) 2019-2050 WestSword, Inc.
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.  */
 
 /* Written by whogiawho <whogiawho@gmail.com>. */
 
 
package com.westsword.stocks.base;


import java.io.*;
import java.util.*;
import java.math.*;
import java.text.*;
import java.nio.charset.*;
import org.apache.commons.io.FileUtils;

import com.westsword.stocks.base.Settings;
import com.westsword.stocks.base.time.*;

public class Utils {


    public static boolean isFile(String path) {
        File f = new File(path);
        return f.isFile();
    }
    //mkdir path if not exist
    public static void mkDir(String path) {
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
    }
    public static void deleteFile(String path) {
        boolean bSwitchOfRawData = Settings.getSwitch(Settings.SWITCH_OF_RAW_DATA);

        try{
            File file = new File(path);
            
            if(file.delete()){
                if(bSwitchOfRawData)
                    System.out.format("%s\n", file.getName() + " is deleted!");
            }else{
                if(bSwitchOfRawData)
                    System.out.format("Delete(%s) operation is failed.\n", path);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public static void append2File(String fileName, String line, boolean bAppend) {
        try {
            FileUtils.writeStringToFile(new File(fileName), line, StandardCharsets.UTF_8.name(), bAppend);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void append2File(String fileName, String line) {
        append2File(fileName, line, true);
    }
    public static void resetDir(String sDir) {
        try {
            FileUtils.deleteDirectory(new File(sDir));
            //force creating dir
            FileUtils.forceMkdir(new File(sDir));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    public static String[] getSubNames(String sDir) {
        File fDir = new File(sDir);
        String[] sNames = fDir.list();
        //System.out.format("size=%d\n", sNames.length);
	    TreeSet<String> trSet = new TreeSet<String>(Arrays.asList(sNames));
	    sNames = trSet.toArray(new String[0]);

        return sNames;
    }






    public static String getCallerName(Class c) {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement e = stacktrace[2];//maybe this number needs to be corrected
        String methodName = e.getMethodName();

        return c.getSimpleName() + "." + methodName;
    }













    public static double roundUp(double inD) {
        return roundUp(inD, null);
    }
    public static double roundUp(double inD, String sFormat) {
        if(sFormat==null) {
            sFormat = Settings.getPriceDecimalFormat();
        }
        DecimalFormat df = new DecimalFormat(sFormat);
        df.setRoundingMode(RoundingMode.CEILING);

        //adjust inD to specified Decimal format
        inD = Double.valueOf(df.format(inD));

        return inD;
    }


    public static boolean isWindows()
    {
        return System.getProperty("os.name").startsWith("Windows");
    }
    public static boolean isLinux()
    {
        return  System.getProperty("os.name").startsWith("Linux");
    }
    public static String getSeperator() {
        return Utils.isWindows()? "\\":"/";
    }


    public static String getAmcKey(String tradeDate0, String tradeDate1, String startHMS, String endHMS) {
        String sHMSPair = startHMS + "," + endHMS;
        String key = tradeDate0 + ",";
        key += tradeDate1 + ",";
        key += sHMSPair;

        return key;
    }


    public static double getOutPrice(double inPrice, double targetProfit, int tradeType) {
        double outPrice;
        if(tradeType == Stock.TRADE_TYPE_LONG)
            outPrice = inPrice + targetProfit;
        else
            outPrice = inPrice - targetProfit;

        return outPrice;
    }



    public static int getIdx(String[] strings, String s) {
        int idx = -1;
        for(int i=0; i<strings.length; i++) {
            if(s.equals(strings[i]))
                return i;
        }
        return idx;
    }



    public static boolean isMarketOff() {
        return isOfflineRun();
    }
    public static boolean isOfflineRun() {
        String tradeDate = Settings.getTradeDate();
        return isOfflineRun(tradeDate);
    }
    public static boolean isOfflineRun(String tradeDate) {
        boolean bOffline = false;

        long closeTp4RunDate = Time.getSpecificTime(tradeDate, AStockSdTime.getCloseQuotationTime());
        //offline run
        long currentTp = System.currentTimeMillis()/1000;
        if(currentTp >= closeTp4RunDate)
            bOffline = true;

        return bOffline;
    }
    //(14:56:00, 15:30:00)
    public static boolean isRRPTime(long currentTp) {
        long rrpStartTime = AStockSdTime.getRrpStartTime(currentTp);
        if(currentTp<rrpStartTime) {
            return false;
        }

        long rrpEndTime = AStockSdTime.getRrpEndTime(currentTp);
        if(currentTp>rrpEndTime) {
            return false;
        }

        return true;
    }


    public static int getCkptIntervalSdLength() {
        int ckptInterval = Settings.getCkptInterval();
        int sdInterval = Settings.getSdInterval();

        int sdLength = (int)roundUp((double)ckptInterval/sdInterval, "#");

        return sdLength;
    }
}
