#!/bin/bash
#####################################################################
## This file is part of the software CodeLab IDE and Simulators    ##
## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
#####################################################################

## DO NOT DELETE, MODIFY OR MOVE THIS SCRIPT !!

if ( test $# -ne 1 )
then
    echo "Incorrect number of parameters: $#"
    echo "Usage: $0 ModuleName"
    exit 1
fi

echo "   Unzipping the plugin template..."
unzip -q newmodule.zip

NAME=$1
PACKAGE=$(echo $1 | tr "[A-Z]" "[a-z]")
MODULEPATH=YourModule/src/codelab/modules/yourmodule

FILE1=$MODULEPATH/YourModule.java
FILE2=$MODULEPATH/YourToolbar.java
FILE3=YourModule/includes/yourAPI.xml

HELLO_C=YourModule/templates/hello.c
HELLO_GO=YourModule/templates/hello.go
HELLO_PY=YourModule/templates/hello.py
HELLO_CLP=YourModule/templates/hello.clp
HELLO_JAVA=YourModule/templates/hello.java

#########################################################
# Replace some strings in files
#########################################################

echo "   Replacing some strings..."

sed -e "s/yourmodule/$PACKAGE/g" $FILE1 > tmp && mv tmp $FILE1
sed -e "s/yourmodule/$PACKAGE/g" $FILE2 > tmp && mv tmp $FILE2
sed -e "s/yourmodule/$PACKAGE/g" $FILE3 > tmp && mv tmp $FILE3

sed -e "s/YourModule/$NAME/g" $FILE1 > tmp && mv tmp $FILE1
sed -e "s/YourModule/$NAME/g" $FILE2 > tmp && mv tmp $FILE2

sed -e "s/YourToolbar/${NAME}Toolbar/g" $FILE1 > tmp && mv tmp $FILE1
sed -e "s/YourToolbar/${NAME}Toolbar/g" $FILE2 > tmp && mv tmp $FILE2

sed -e "s/yourAPI/${NAME}API/g" $FILE3 > tmp && mv tmp $FILE3

sed -e "s/yourmodule/$PACKAGE/g" $HELLO_C > tmp && mv tmp $HELLO_C
sed -e "s/yourmodule/$PACKAGE/g" $HELLO_GO > tmp && mv tmp $HELLO_GO
sed -e "s/yourmodule/$PACKAGE/g" $HELLO_PY > tmp && mv tmp $HELLO_PY
sed -e "s/yourmodule/$PACKAGE/g" $HELLO_CLP > tmp && mv tmp $HELLO_CLP
sed -e "s/yourmodule/$PACKAGE/g" $HELLO_JAVA > tmp && mv tmp $HELLO_JAVA

sed -e "s/yourAPI/${NAME}API/g" $HELLO_C > tmp && mv tmp $HELLO_C
sed -e "s/yourAPI/${NAME}API/g" $HELLO_GO > tmp && mv tmp $HELLO_GO
sed -e "s/yourAPI/${NAME}API/g" $HELLO_PY > tmp && mv tmp $HELLO_PY
sed -e "s/yourAPI/${NAME}API/g" $HELLO_CLP > tmp && mv tmp $HELLO_CLP
sed -e "s/yourAPI/${NAME}API/g" $HELLO_JAVA > tmp && mv tmp $HELLO_JAVA

#########################################################
# Rename the project files
#########################################################

echo "   Renaming the project files..."

mv $MODULEPATH/YourModule.java $MODULEPATH/$NAME.java
mv $MODULEPATH/YourToolbar.java $MODULEPATH/${NAME}Toolbar.java
mv $MODULEPATH YourModule/src/codelab/modules/$PACKAGE

mv YourModule/includes/yourAPI.xml YourModule/includes/${NAME}API.xml
mv YourModule ../../modules/$NAME

#########################################################
# Display some information
#########################################################

echo "      Package name : codelab.modules.$PACKAGE"
echo "      Module file  : $NAME.java"
echo "      Toolbar file : ${NAME}Toolbar.java"
echo "      API file     : ${NAME}API.xml"
