./compile.sh
#java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/stanford-corenlp-2012-03-09.jar:../lib/jnisvmlight.jar:. -Djava.library.path=../lib/ CoNLLZeroPronoun/detect/ZeroDetect all train

cd /users/yzcchen/tool/JNI_SVM-light-6.01/src/svmlight-6.01
./linux-svm_learn ~/workspace/CoNLL-2012/src/zeroTrain.all zeroModel.all

cd /users/yzcchen/workspace/CoNLL-2012/src
java -cp ../lib/edu.mit.jwi_2.2.3.jar:../lib/stanford-corenlp-2012-03-09.jar:../lib/jnisvmlight.jar:. -Djava.library.path=../lib/ CoNLLZeroPronoun/detect/ZeroDetect all test
