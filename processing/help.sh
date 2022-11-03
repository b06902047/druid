#!/bin/bash
input="help.txt"
while read -r line
do
  prefix="org.apache.druid."
  new_line=$(echo $line | sed 's/\//\./g')
  output=$(cat "$line"  | grep "@Test" -A1 |grep -v "@Test" | awk '{FS=" "} {print $3}' | grep -v '^$')
  for i in $output
  do
    new_output=$(echo $i | sed 's/()//g')
    echo "${new_line}#${new_output}"
  done
done < "$input"


