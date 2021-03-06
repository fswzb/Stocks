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
 
 
package com.westsword.stocks.base.ckpt;


import java.util.*;

import com.westsword.stocks.base.time.*;
import com.westsword.stocks.base.ckpt.*;
import com.westsword.stocks.base.Settings;

public class CheckPoint0 extends CheckPoint {
    //interval - in seconds
    public CheckPoint0(int interval, String endHMS) {
        super();

        String currentDate = Time.currentDate();

        //add 09:25:00
        add(AStockSdTime.getCallAuctionEndTime0());

        //skip 09:30:00
        //add [09:31:00, 11:30:00]
        long startTp = Time.getSpecificTime(currentDate, AStockSdTime.getOpenQuotationTime()) + interval;
        long endTp = Time.getSpecificTime(currentDate, AStockSdTime.getMidSuspensionTime());
        add(startTp, endTp, interval);

        //skip 13:00:00
        //add [13:01:00, endHMS]
        startTp = Time.getSpecificTime(currentDate, AStockSdTime.getMidOpenQuotationTime()) + interval;
        endTp = Time.getSpecificTime(currentDate, endHMS);
        add(startTp, endTp, interval);
    }

    public CheckPoint0() {
        this(Settings.getCkptInterval(), "14:56:00");
    }
}
