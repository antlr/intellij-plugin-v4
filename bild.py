#!/usr/bin/env python

# bootstrap by downloading bilder.py if not found
import urllib
import os

if not os.path.exists("bilder.py"):
	print "bootstrapping; downloading bilder.py"
	urllib.urlretrieve(
		"https://raw.githubusercontent.com/parrt/bild/master/src/python/bilder.py",
		"bilder.py")

# assumes bilder.py is in current directory
from bilder import *

def parsers():
	antlr4("src/grammars", "gen", package="org.antlr.intellij.plugin.parser")

def clean():
	rmdir("gen")

def all():
	# make sure we have latest antlr4 build
	python("bild.py", workingdir="../antlr4", args="mkjar")
	mkdir("lib")
	# grab the lib plugin needs
	copyfile(src="../antlr4/dist/antlr-4.4-complete.jar", trg="lib")

processargs(globals())
