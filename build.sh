#!/bin/bash

mkdir dist
javac ServerNoGUI.java -d "dist/"
javac Client.java -d "dist/"
cp movie.Mjpeg dist/