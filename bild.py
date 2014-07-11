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

def latest_antlr4():
	# make sure we have latest antlr4 build
	python("bild.py", workingdir="../antlr4", args="mkjar")
	mkdir("lib")
	# grab the lib that the plugin needs
	copyfile(src="../antlr4/dist/antlr-4.4-complete.jar", trg="lib")
	# put it in JARCARCHE too so bild can find it during antlr4()
	copyfile(src="../antlr4/dist/antlr-4.4-complete.jar", trg=JARCACHE)

def parsers():
	require(latest_antlr4)
	antlr4("src/grammars", "gen", version="4.4", package="org.antlr.intellij.plugin.parser")

def clean():
	rmdir("gen")

def all():
	require(parsers)

processargs(globals())
