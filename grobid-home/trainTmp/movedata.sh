#!/bin/sh
echo "1) figure  2) fulltext  3) header  4) referSeg  5) seg  6) table"
echo "select target : "
read target

if [ $target -eq 1 ];then
	echo "figure?"
fi
if [ $target -eq 2 ];then
        echo "fulltext?"
fi
if [ $target -eq 3 ];then
        echo "header?"
fi
if [ $target -eq 4 ];then
        echo "referSeg?"
fi
if [ $target -eq 5 ];then
        echo "seg?"
fi
if [ $target -eq 6 ];then
        echo "table?"
fi

echo "[y/n]"
read yn

if [ $yn == y ];then
echo "move.."
else exit
fi

find . -type 

