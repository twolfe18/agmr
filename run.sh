
rm *.class

CP=".:`find lib/ -name "*.jar" | paste -sd ":"`"
javac -cp $CP AGMRRunner.java || exit -1
javac -cp $CP WordBeforeSBar.java || exit -2
javac -cp $CP TestSentenceMapper.java || exit -3
javac -cp $CP TestBigKeySentenceMapper.java || exit -3

if [ -d scratch ]; then
	rm scratch/*
else
	mkdir scratch
fi

java -ea -Xmx750M -cp $CP AGMRRunner WordBeforeSBar scratch /export/common/data/corpora/LDC/LDC2012T21/data/xml/afp_eng_1994*
java -ea -Xmx750M -cp $CP AGMRRunner TestBigKeySentenceMapper scratch /export/common/data/corpora/LDC/LDC2012T21/data/xml/afp_eng_1994*
java -ea -Xmx750M -cp $CP AGMRRunner TestSentenceMapper scratch /export/common/data/corpora/LDC/LDC2012T21/data/xml/afp_eng_1994*


