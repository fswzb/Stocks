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


package com.westsword.stocks.am;
 
import java.util.*;
import org.apache.commons.cli.*;
import org.apache.commons.math3.stat.regression.*;

import com.westsword.stocks.base.Utils;
import com.westsword.stocks.base.utils.CmdLineUtils;

public class AmDerUtils {
    public final static int Default_Backward_SD = 60*5;
    public final static double Default_Threshold = 0.5;
    public final static int Default_Minimum_Skipped_SD = 5;


    public static int getBackwardSd(CommandLine cmd) {
        return CmdLineUtils.getInteger(cmd, "b", Default_Backward_SD);
    }
    public static double getThreshold(CommandLine cmd) {
        return CmdLineUtils.getDouble(cmd, "h", Default_Threshold);
    }
    public static int getMinimumSkipSd(CommandLine cmd) {
        return CmdLineUtils.getInteger(cmd, "m", Default_Minimum_Skipped_SD);
    }


    public static void listSingleSd(int sd, double threshold, int sdbw, int minSkippedSD,
            TreeMap<Integer, AmRecord> amrMap, boolean bStdOut, String sDerivativeFile) {
        
        int minDist=minSkippedSD;
        for(int dist=sdbw; dist>=minDist; dist--) {
            int start=sd-dist;
            int end=sd;
            SimpleRegression sr = new SimpleRegression();
            for(int i=start; i<=end; i++) {
                int x = i - start;
                long y = amrMap.get(i).am;
                sr.addData((double)x, (double)y);
            }
            //0: direct
            //1: indirect
            String sSlope = translateSlope(1, sr, threshold, sr.getRSquare());
            String line = String.format("%-8.3f %8s\n", sr.getRSquare(), sSlope);
            if(bStdOut)
                System.out.format("%s", line);
            if(sDerivativeFile!=null) {
                //write line to derivativeFile
                Utils.append2File(sDerivativeFile, line);
            }
        }
    }
    private static String translateSlope(int type, SimpleRegression sr, double threshold, double r2) {
        String sSlope = translateSlopeD(sr);  //default directly
        if(type==1)    //ind
            sSlope = translateSlopeInd(sr, threshold, r2);

        return sSlope;
    }
    private static String translateSlopeD(SimpleRegression sr) {
        return ""+Utils.roundUp(sr.getSlope());
    }
    private static String translateSlopeInd(SimpleRegression sr, double threshold, double r2) {
        String sSlope = "#N/A";

        if(r2>=threshold)
            sSlope = ""+Utils.roundUp(sr.getSlope(), "#.###");

        return sSlope;
    }


    public static void makeAmDerPng(String stockCode, String tradeDate, String hms) {
        ThreadMakeAmDer t = new ThreadMakeAmDer(stockCode, tradeDate, hms);
        t.start();
    }
}
