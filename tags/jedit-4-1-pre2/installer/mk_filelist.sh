#!/bin/sh

# This script must be run from the jEdit directory, *not* the installer
# directory!!!

# jedit-program fileset
echo jedit.jar > installer/jedit-program
echo jars/LatestVersion.jar >> installer/jedit-program
echo jars/QuickNotepad.jar >> installer/jedit-program
echo properties/README.txt >> installer/jedit-program
echo startup/README.txt >> installer/jedit-program
find modes -name \*.xml >> installer/jedit-program
echo modes/catalog >> installer/jedit-program
find doc \( -name \*.txt -o -name \*.html -o -name \*.png \) >> installer/jedit-program
find doc -name toc.xml >> installer/jedit-program

echo -n "jedit-program: "
ls -l `cat installer/jedit-program` | awk 'BEGIN { size=0 } { disk_size+=(int($5/4096+1)*4); size+=$5/1024 } END { print disk_size " " size }'

# jedit-macros fileset
find macros -name \*.bsh > installer/jedit-macros

echo -n "jedit-macros: "
ls -l `cat installer/jedit-macros` | awk 'BEGIN { size=0 } { disk_size+=(int($5/4096+1)*4); size+=$5/1024 } END { print disk_size " " size }'

# jedit-windows fileset
echo jeshlstb.dl_ > installer/jedit-windows
echo ltslog.dll >> installer/jedit-windows
echo jeditsrv.exe >> installer/jedit-windows
echo jedit.exe >> installer/jedit-windows
echo jedinit.exe >> installer/jedit-windows
echo unlaunch.exe >> installer/jedit-windows
echo jedinstl.dll >> installer/jedit-windows
echo jeservps.dll >> installer/jedit-windows
echo jedidiff.exe >> installer/jedit-windows
echo jEdit_IE.reg.txt >> installer/jedit-windows

echo -n "jedit-windows: "
ls -l `cat installer/jedit-windows` | awk 'BEGIN { size=0 } { disk_size+=(int($5/4096+1)*4); size+=$5/1024 } END { print disk_size " " size }'

# jedit-mac fileset
echo jars/MacOS.jar > installer/jedit-mac
echo -n "jedit-mac: "
ls -l `cat installer/jedit-mac` | awk 'BEGIN { size=0 } { disk_size+=(int($5/4096+1)*4); size+=$5/1024 } END { print disk_size " " size }'

# jedit-os2 fileset
echo jedit.cmd > installer/jedit-os2

echo -n "jedit-os2: "
ls -l `cat installer/jedit-os2` | awk 'BEGIN { size=0 } { disk_size+=(int($5/4096+1)*4); size+=$5/1024 } END { print disk_size " " size }'


for file in installer/jedit-*
do
	sort $file > $file.tmp
	mv $file.tmp $file
done
