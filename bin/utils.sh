#!/bin/bash

function getWindowPathOfTmpFile {
    local tmpCygwinPath=$1

    local rootTmp="D:\cygwin64\tmp"
    echo $rootTmp\\`basename $tmpCygwinPath`
}

function getWindowPathOfFile {
    local cygwinFile=$1

    local cygwinWinRoot="d:\cygwin64"

    local file=
    file=`basename $cygwinFile`
    local dir=
    dir=`dirname $cygwinFile`
    echo $dir|grep -q "\/cygdrive" && {
        dir=`echo $dir|sed 's@\/cygdrive@@g'`
        drive=${dir:1:1}
        dir=`echo $dir|sed "s@^\/$drive@$drive:@g"`
        dir=`echo $dir|sed 's@/@\\\@g'`
        echo $dir\\$file        
    } || {
        dir=`echo $dir|sed 's@/@\\\@g'`
        [[ $dir = '\' ]] && dir=
        echo "$cygwinWinRoot$dir\\$file"
    }
}



function divide {
    local f0=$1
    local f1=$2

    local result=
    result=`echo "scale=3; $f0 / $f1"|bc`

    echo $result

}
function mul {
    local f0=$1
    local f1=$2

    local result=
    result=`echo "$f0 * $f1"|bc`

    echo $result
}
function add {
    local f0=$1
    local f1=$2

    local result=
    result=`echo "$f0 + $f1"|bc`

    echo $result
}
function substract {
    local f0=$1
    local f1=$2

    local result=
    result=`echo "$f0 - $f1"|bc`

    echo $result
}

function ge {
    local f0=$1
    local f1=$2

    local bCompare=
    bCompare=`echo "$f0 >= $f1"|bc`

    echo $bCompare
}
function le {
    local f0=$1
    local f1=$2

    local bCompare=
    bCompare=`echo "$f0 <= $f1"|bc`

    echo $bCompare
}


function getCodeStats {
    local javaLiines=
    javaLiines=`find $cygwinRootDir/java -name *.java|xargs wc|grep -v "/cygdrive/d/stocks/"|awk '{print $1}'`

    local cLines=
    cLines=`find $cygwinRootDir/c -name *.c -o -name *.h|xargs wc|grep -v "/cygdrive/d/stocks/"|awk '{print $1}'`

    local scriptLines=
    scriptLines=`find $cygwinRootDir/bin/  -name *.bat -o -name *.sh|grep -v backup|grep -v test|grep -v obso|xargs wc|grep -v "/cygdrive/d/stocks/"|awk '{print $1}'`

    local idcLines=
    idcLines=`find $cygwinRootDir/scripts/  -name *.idc |grep -v backup | xargs wc|grep -v "/cygdrive/d/stocks/"|awk '{print $1}'`

    printf "%8s %8s %12s %8s\n" "Java" "C" "bash&bat" "idc"
    printf "%8s %8s %12s %8s\n" $javaLiines $cLines $scriptLines $idcLines 
}

function killHexin {
    taskkill.exe  /S localhost /IM hexin.exe
    taskkill.exe  /S localhost /IM sjsj.exe 
    taskkill.exe  /S localhost /IM zdsj.exe 
}

function abs {
    local value=$1

    [[ $value -lt 0 ]] && value=$((value*-1))

    echo $value
}




function getListAvg {
    local fList=$1
    local colN=$2

    awk "BEGIN{count=0; sum=0}{sum+=\$$colN; count++;}END{avg=sum/count; print avg}" $fList
}
function getListSum {
    local fList=$1
    local colN=$2

    awk "BEGIN{count=0; sum=0}{sum+=\$$colN; count++;}END{print sum}" $fList
}
function getListStdDev {
    local fList=$1
    local colN=$2

    awk "{sum+=\$$colN; sumsq+=\$$colN*\$$colN} END {print sqrt(sumsq/NR - (sum/NR)**2)}" $fList
}

BuyStockServiceRate=${BuyStockServiceRate:-0.0003}
SellStockServiceRate=${SellStockServiceRate:-0.0003}
SellStockTaxRate=${SellStockTaxRate:-0.001}
function getTradeCost {
    local buyPrice=$1
    local sellPrice=$2

    local cost=
    local cost0=`mul $buyPrice $BuyStockServiceRate`
    local cost1=`mul $sellPrice $SellStockServiceRate`
    local cost2=`mul $sellPrice $SellStockTaxRate`
    cost=`add $cost0 $cost1`
    cost=`add $cost $cost2`

    echo $cost
}
function getNetProfit {
    local buyPrice=$1
    local sellPrice=$2

    local netProfit=
    local profit0=`substract $sellPrice $buyPrice`
    local profit1=`getTradeCost $buyPrice $sellPrice`
    netProfit=`substract $profit0 $profit1`

    echo $netProfit
}

