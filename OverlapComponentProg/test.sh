#!/bin/bash

for((i=0;i<100000;i++))
do
echo "--------- $i ----------"
./main 10000 $i
if [[ $? == 1 ]] 
then 
exit 1
fi
done
