
CP="lib/agiga-1.0.jar:lib/trove-3.0.2.jar:."
javac -cp $CP AGMRRunner.java || exit -1
javac -cp $CP TestSentenceMapper.java || exit -2

AGIGA_FILE=/export/common/data/corpora/LDC/LDC2012T21/data/xml/afp_eng_199405.xml.gz
if [ -d scratch ]; then
	rm scratch/*
else
	mkdir scratch
fi
java -ea -cp $CP AGMRRunner TestSentenceMapper $AGIGA_FILE scratch


