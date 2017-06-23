#!/bin/sh

cp_var=
PROGRAM_NAME=MainFile.java

for i in `ls lib/*.jar`
  do
  cp_var=${cp_var}:${i}
done

javac -d class -cp ".:${cp_var}:class" src/$PROGRAM_NAME

if [ $? -eq 0 ]
then
  echo "compiled successfully :) :) :) :) :)"
  java -cp .:class:lib/json-simple-1.1.1.jar src.MainFile
else
  echo "compilation failed :( :( :( :( :("
fi
