#!/bin/bash

function getAmCorrel {
    local stockCode=$1
    local tradeDate1=$2
    local tradeDate2=$3
    local hmsList=$4

    java -jar $analyzetoolsJar getamcorrel $stockCode $tradeDate1 $tradeDate2 $hmsList 2>/dev/null
}

