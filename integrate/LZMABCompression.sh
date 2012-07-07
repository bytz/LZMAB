#!/bin/bash
PROGRAM_DIR=`dirname "$0"`
PROGRAM_DIR=`cd "$PROGRAM_DIR"; pwd`
java -jar $PROGRAM_DIR/LZMABCompression.jar &
